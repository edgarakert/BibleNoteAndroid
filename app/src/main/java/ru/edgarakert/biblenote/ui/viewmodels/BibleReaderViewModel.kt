package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.Book
import ru.edgarakert.biblenote.data.bible.HighlightColor
import ru.edgarakert.biblenote.data.settings.SettingsRepository

class BibleReaderViewModel(
    private val bibleService: BibleDatabaseService,
    private val settingsRepository: SettingsRepository
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
        val isLoading: Boolean = true
    ) {
        val canGoPrev: Boolean get() = chapter > 1
        val canGoNext: Boolean get() = chapter < chapterCount
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var initJob: Job? = null
    private var loadJob: Job? = null

    init {
        initJob = viewModelScope.launch {
            val enabled = settingsRepository.enabledTranslations.first()
            val default = settingsRepository.defaultTranslation.first()
            val lastTranslation = settingsRepository.lastBibleTranslation.first()
            val scale = settingsRepository.verseScale.first()
            val bookId = settingsRepository.lastBookId.first()
            val chapter = settingsRepository.lastChapter.first()
            val translation = when {
                lastTranslation.isNotEmpty() && lastTranslation in enabled -> lastTranslation
                default in enabled -> default
                else -> enabled.firstOrNull() ?: default
            }
            val books = bibleService.fetchAllBooks(translation)
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
        _uiState.update { it.copy(bookId = bookId, chapter = chapter, selectedVerseNumbers = emptySet()) }
        persistPosition(bookId, chapter)
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
