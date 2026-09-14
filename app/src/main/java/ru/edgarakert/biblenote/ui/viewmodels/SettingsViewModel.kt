package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import java.util.Locale

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    data class TranslationInfo(
        val id: String,
        val nameResId: Int,
        val subtitleResId: Int
    )

    data class TranslationGroup(
        val titleResId: Int,
        val translations: List<TranslationInfo>
    )

    data class UiState(
        val defaultTranslation: String = "synodal",
        val enabledTranslations: List<String> = SettingsRepository.ALL_TRANSLATIONS,
        val appearanceMode: SettingsRepository.AppearanceMode = SettingsRepository.AppearanceMode.SYSTEM,
        val verseScale: Float = 1.0f,
        val translationGroups: List<TranslationGroup> = emptyList()
    )

    private val ruGroup = TranslationGroup(
        titleResId = R.string.translations_group_russian,
        translations = listOf(
            TranslationInfo("synodal", R.string.translation_synodal_name, R.string.translation_synodal_subtitle),
            TranslationInfo("nrt", R.string.translation_nrt_name, R.string.translation_nrt_subtitle)
        )
    )
    private val enGroup = TranslationGroup(
        titleResId = R.string.translations_group_english,
        translations = listOf(
            TranslationInfo("kjv", R.string.translation_kjv_name, R.string.translation_kjv_subtitle),
            // TranslationInfo("niv", R.string.translation_niv_name, R.string.translation_niv_subtitle)
        )
    )

    private val orderedGroups: List<TranslationGroup> =
        if (Locale.getDefault().language == "ru") listOf(ruGroup, enGroup)
        else listOf(enGroup, ruGroup)

    private val toggleMutex = Mutex()

    val uiState: StateFlow<UiState> = combine(
        repository.defaultTranslation,
        repository.enabledTranslations,
        repository.appearanceMode,
        repository.verseScale
    ) { default, enabled, appearance, scale ->
        UiState(
            defaultTranslation = default,
            enabledTranslations = enabled,
            appearanceMode = appearance,
            verseScale = scale,
            translationGroups = orderedGroups
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(translationGroups = orderedGroups)
    )

    fun setDefaultTranslation(translation: String) {
        viewModelScope.launch { repository.setDefaultTranslation(translation) }
    }

    fun toggleEnabledTranslation(id: String) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val current = repository.enabledTranslations.first()
                if (id in current) {
                    if (current.size <= 1) return@withLock
                    val updated = current.filter { it != id }
                    repository.setEnabledTranslations(updated)
                    if (repository.defaultTranslation.first() == id) {
                        repository.setDefaultTranslation(updated.first())
                    }
                } else {
                    val ordered = SettingsRepository.ALL_TRANSLATIONS.filter { it == id || it in current }
                    repository.setEnabledTranslations(ordered)
                }
            }
        }
    }

    fun setAppearanceMode(mode: SettingsRepository.AppearanceMode) {
        viewModelScope.launch { repository.setAppearanceMode(mode) }
    }

    fun setVerseScale(scale: Float) {
        viewModelScope.launch { repository.setVerseScale(scale) }
    }

    // suspend so the caller can await completion before navigating away
    suspend fun saveOnboardingSelections(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) return
        val ordered = SettingsRepository.ALL_TRANSLATIONS.filter { it in selectedIds }
        if (ordered.isEmpty()) return
        repository.setEnabledTranslations(ordered)
        if (repository.defaultTranslation.first() !in selectedIds) {
            repository.setDefaultTranslation(ordered.first())
        }
        repository.setOnboardingCompleted()
    }
}
