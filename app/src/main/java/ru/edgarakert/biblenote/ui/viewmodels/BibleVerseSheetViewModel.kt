package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.HighlightColor
import ru.edgarakert.biblenote.data.settings.SettingsRepository

class BibleVerseSheetViewModel(
    private val reference: BibleReference,
    private val bibleService: BibleDatabaseService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    data class UiState(
        val title: String = "",
        val bookName: String = "",
        val verses: List<Pair<Int, String>> = emptyList(),
        val highlights: Map<Int, HighlightColor> = emptyMap(),
        val selectedVerses: Set<Int> = emptySet(),
        val enabledTranslations: List<String> = emptyList(),
        val selectedTranslation: String = "synodal",
        val verseScale: Float = 1.0f,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Стих, на котором открывается шторка: первый из указанных в ссылке. */
    val scrollTarget: Int? get() = _uiState.value.selectedVerses.minOrNull()

    private var loadJob: Job? = null

    init {
        // Ссылка на всю главу открывается с пустым выбором — coveredVerses для неё пуст.
        _uiState.update { it.copy(selectedVerses = reference.coveredVerses.toSet()) }

        loadJob = viewModelScope.launch {
            val enabled = settingsRepository.enabledTranslations.first()
            val default = settingsRepository.defaultTranslation.first()
            val scale = settingsRepository.verseScale.first()
            val initial = if (default in enabled) default else (enabled.firstOrNull() ?: default)
            _uiState.update {
                it.copy(
                    enabledTranslations = enabled,
                    selectedTranslation = initial,
                    verseScale = scale
                )
            }
            loadChapter(initial)
        }
    }

    fun setTranslation(translation: String) {
        _uiState.update { it.copy(selectedTranslation = translation) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadChapter(translation) }
    }

    /**
     * Тап по стиху немедленно переключает его выбор и пересчитывает заголовок от живого
     * выбора. Возвращает отсортированный список наружу — канал для задачи 13.5, которая
     * будет переписывать ссылку в тексте заметки; сама запись в текст здесь не делается.
     */
    fun toggleVerse(verseNumber: Int): List<Int> {
        val next = _uiState.value.selectedVerses.toMutableSet().apply {
            if (!add(verseNumber)) remove(verseNumber)
        }
        _uiState.update { it.copy(selectedVerses = next, title = buildTitle(it.bookName, next)) }
        return next.sorted()
    }

    /** Грузит главу целиком (не только стихи из ссылки) стихи + подсветки одним джойном. */
    private suspend fun loadChapter(translation: String) {
        _uiState.update { it.copy(isLoading = true) }
        val rows = bibleService.fetchVersesWithHighlights(reference.bookId, reference.chapter, translation)
        val bookName = bibleService.fetchBookName(reference.bookId, translation)
            ?: "Book ${reference.bookId}"
        _uiState.update { state ->
            state.copy(
                bookName = bookName,
                verses = rows.map { it.first to it.second },
                highlights = rows.mapNotNull { row -> row.third?.let { row.first to it } }.toMap(),
                title = buildTitle(bookName, state.selectedVerses),
                isLoading = false
            )
        }
    }

    /** Заголовок отражает живой выбор, а не исходную ссылку. En dash — только для показа. */
    private fun buildTitle(bookName: String, selected: Set<Int>): String {
        val spec = BibleReference.formatVerseSpec(selected.toList(), rangeSeparator = "–")
        return if (spec.isEmpty()) "$bookName ${reference.chapter}"
        else "$bookName ${reference.chapter}:$spec"
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}
