package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.bible.Book
import ru.edgarakert.biblenote.data.bible.HighlightColor
import ru.edgarakert.biblenote.data.bible.NoteVerseIndexService
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.settings.SettingsRepository

class BibleReaderViewModel(
    private val bibleService: BibleDatabaseService,
    private val settingsRepository: SettingsRepository,
    private val parser: BibleReferenceParser,
    private val repository: NoteRepository
) : ViewModel() {

    data class UiState(
        val bookId: Int = 1,
        val chapter: Int = 1,
        val bookName: String = "",
        val chapterCount: Int = 1,
        val translation: String = "synodal",
        val enabledTranslations: List<String> = emptyList(),
        val books: List<Book> = emptyList(),
        val verses: List<Pair<Int, String>> = emptyList(),
        val highlights: Map<Int, HighlightColor> = emptyMap(),
        val selectedVerseNumbers: Set<Int> = emptySet(),
        val verseScale: Float = 1.0f,
        val isLoading: Boolean = true,
        val noteCounts: Map<Int, Int> = emptyMap()
    ) {
        val canGoPrev: Boolean get() = chapter > 1
        val canGoNext: Boolean get() = chapter < chapterCount
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var initJob: Job? = null
    private var loadJob: Job? = null

    // navigateTo() called before initJob completes sets this; initJob uses it over last saved position.
    private var pendingNavigation: Pair<Int, Int>? = null
    private var isInitialized = false

    // Индекс заметок по стихам текущей главы — источник noteCounts в UiState.
    // Хранится отдельно, а не в UiState, потому что списки Note тяжелее, чем нужно
    // для отрисовки (там нужны только количества), а notesForVerse() отдаёт их по требованию.
    private var verseNotes: Map<Int, List<Note>> = emptyMap()

    init {
        observeNoteIndex()
        initJob = viewModelScope.launch {
            val enabled = settingsRepository.enabledTranslations.first()
            val default = settingsRepository.defaultTranslation.first()
            val lastTranslation = settingsRepository.lastBibleTranslation.first()
            val scale = settingsRepository.verseScale.first()
            val lastBookId = settingsRepository.lastBookId.first()
            val lastChapter = settingsRepository.lastChapter.first()
            val translation = when {
                lastTranslation.isNotEmpty() && lastTranslation in enabled -> lastTranslation
                default in enabled -> default
                else -> enabled.firstOrNull() ?: default
            }
            val books = bibleService.fetchAllBooks(translation)

            val nav = pendingNavigation
            val bookId = nav?.first ?: lastBookId
            val chapter = nav?.second ?: lastChapter

            isInitialized = true
            _uiState.update {
                it.copy(
                    bookId = bookId,
                    chapter = chapter,
                    translation = translation,
                    enabledTranslations = enabled,
                    books = books,
                    verseScale = scale
                )
            }
            loadChapter(bookId, chapter, translation)
        }
    }

    fun navigatePrev() {
        val s = _uiState.value
        if (!s.canGoPrev) return
        val newChapter = s.chapter - 1
        _uiState.update { it.copy(chapter = newChapter, selectedVerseNumbers = emptySet()) }
        persistPosition(s.bookId, newChapter)
        triggerLoad()
    }

    fun navigateNext() {
        val s = _uiState.value
        if (!s.canGoNext) return
        val newChapter = s.chapter + 1
        _uiState.update { it.copy(chapter = newChapter, selectedVerseNumbers = emptySet()) }
        persistPosition(s.bookId, newChapter)
        triggerLoad()
    }

    fun navigateTo(bookId: Int, chapter: Int) {
        persistPosition(bookId, chapter)
        if (!isInitialized) {
            pendingNavigation = bookId to chapter
            _uiState.update { it.copy(bookId = bookId, chapter = chapter, selectedVerseNumbers = emptySet()) }
            return
        }
        _uiState.update { it.copy(bookId = bookId, chapter = chapter, selectedVerseNumbers = emptySet()) }
        triggerLoad()
    }

    fun setTranslation(translation: String) {
        _uiState.update { it.copy(translation = translation) }
        viewModelScope.launch { settingsRepository.setLastBibleTranslation(translation) }
        triggerLoad()
    }

    fun toggleVerseSelection(verseNumber: Int) {
        _uiState.update {
            val updated = it.selectedVerseNumbers.toMutableSet()
            if (!updated.add(verseNumber)) updated.remove(verseNumber)
            it.copy(selectedVerseNumbers = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedVerseNumbers = emptySet()) }
    }

    fun applyHighlight(color: HighlightColor) {
        val s = _uiState.value
        if (s.selectedVerseNumbers.isEmpty()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            bibleService.deleteHighlights(s.bookId, s.chapter, s.selectedVerseNumbers)
            bibleService.saveHighlights(color, s.bookId, s.chapter, s.selectedVerseNumbers)
            _uiState.update { it.copy(selectedVerseNumbers = emptySet()) }
            loadChapter(s.bookId, s.chapter, s.translation)
        }
    }

    fun removeHighlights() {
        val s = _uiState.value
        if (s.selectedVerseNumbers.isEmpty()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            bibleService.deleteHighlights(s.bookId, s.chapter, s.selectedVerseNumbers)
            _uiState.update { it.copy(selectedVerseNumbers = emptySet()) }
            loadChapter(s.bookId, s.chapter, s.translation)
        }
    }

    fun copySelectedVerses(): String {
        val s = _uiState.value
        val sorted = s.verses
            .filter { s.selectedVerseNumbers.contains(it.first) }
            .sortedBy { it.first }
        val header = "${s.bookName} ${s.chapter}"
        return "$header\n${sorted.joinToString("\n") { "${it.first} ${it.second}" }}"
    }

    suspend fun fetchChapterCount(bookId: Int): Int =
        bibleService.fetchChapterCount(bookId, _uiState.value.translation)

    fun notesForVerse(verseNumber: Int): List<Note> = verseNotes[verseNumber].orEmpty()

    // Пересчитывается при смене главы/перевода и при любой эмиссии всего корпуса заметок —
    // так бейдж появляется сразу после сохранения ссылки в заметке, без ручного обновления
    // читалки. Ключ комбинации — сам список стихов (verses), а не только bookId/chapter:
    // после смены главы verses обновляется отдельной эмиссией (loadChapter грузит стихи
    // асинхронно), и если ключевать только на bookId/chapter, чтение _uiState.value.verses
    // внутри collectLatest могло бы поймать ещё старый список — на стыке смены главы это
    // исказило бы охват ссылок на главу целиком.
    private fun observeNoteIndex() {
        viewModelScope.launch {
            combine(
                repository.observeAllNotes(),
                _uiState.map { Triple(it.bookId, it.chapter, it.verses.map { pair -> pair.first }) }
                    .distinctUntilChanged()
            ) { notes, key -> notes to key }
                .collectLatest { (notes, key) ->
                    val (bookId, chapter, verseNumbers) = key
                    // Регулярка парсера (~360 альтернатив книг) прогоняется по всему корпусу
                    // заметок — CPU-нагрузка, не IO, поэтому Dispatchers.Default.
                    val index = withContext(Dispatchers.Default) {
                        NoteVerseIndexService.index(notes, bookId, chapter, verseNumbers, parser)
                    }
                    verseNotes = index
                    _uiState.update { it.copy(noteCounts = index.mapValues { (_, v) -> v.size }) }
                }
        }
    }

    private fun triggerLoad() {
        val s = _uiState.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadChapter(s.bookId, s.chapter, s.translation) }
    }

    private suspend fun loadChapter(bookId: Int, chapter: Int, translation: String) {
        _uiState.update { it.copy(isLoading = true) }
        val bookName = bibleService.fetchBookName(bookId, translation) ?: "Book $bookId"
        val chapterCount = bibleService.fetchChapterCount(bookId, translation)
        val rows = bibleService.fetchVersesWithHighlights(bookId, chapter, translation)
        val verses = rows.map { Pair(it.first, it.second) }
        val highlights = rows.mapNotNull { (num, _, color) -> color?.let { num to it } }.toMap()
        _uiState.update {
            it.copy(
                bookName = bookName,
                chapterCount = chapterCount,
                verses = verses,
                highlights = highlights,
                isLoading = false
            )
        }
    }

    private fun persistPosition(bookId: Int, chapter: Int) {
        viewModelScope.launch {
            settingsRepository.setLastBookId(bookId)
            settingsRepository.setLastChapter(chapter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        initJob?.cancel()
        loadJob?.cancel()
    }
}
