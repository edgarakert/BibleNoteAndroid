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
        val verses: List<Pair<Int, String>> = emptyList(),
        val highlights: Map<Int, HighlightColor> = emptyMap(),
        val enabledTranslations: List<String> = emptyList(),
        val selectedTranslation: String = "synodal",
        val verseScale: Float = 1.0f,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
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
            loadContent(initial)
        }
    }

    fun setTranslation(translation: String) {
        _uiState.update { it.copy(selectedTranslation = translation) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadContent(translation) }
    }

    private suspend fun loadContent(translation: String) {
        _uiState.update { it.copy(isLoading = true) }
        val bookName = bibleService.fetchBookName(reference.bookId, translation)
            ?: "Book ${reference.bookId}"
        val verses = bibleService.fetchVerses(reference, translation)
        val highlights = bibleService.fetchHighlights(reference.bookId, reference.chapter)
        val title = buildTitle(bookName)
        _uiState.update {
            it.copy(title = title, verses = verses, highlights = highlights, isLoading = false)
        }
    }

    private fun buildTitle(bookName: String): String {
        val suffix = reference.verseDisplaySuffix
        return if (suffix.isEmpty()) "$bookName ${reference.chapter}"
               else "$bookName ${reference.chapter}:$suffix"
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}
