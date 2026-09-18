package ru.edgarakert.biblenote.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    enum class AppearanceMode { SYSTEM, LIGHT, DARK }

    companion object {
        val ALL_TRANSLATIONS = listOf("synodal", /*"nrt",*/ "kjv" /*, "niv"*/)

        private val KEY_TRANSLATION = stringPreferencesKey("defaultTranslation")
        private val KEY_ENABLED_TRANSLATIONS = stringSetPreferencesKey("enabledTranslations")
        private val KEY_APPEARANCE = stringPreferencesKey("appearanceMode")
        private val KEY_VERSE_SCALE = floatPreferencesKey("verseScale")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboardingCompleted")
        private val KEY_LAST_BOOK_ID = intPreferencesKey("bible.lastBookId")
        private val KEY_LAST_CHAPTER = intPreferencesKey("bible.lastChapter")
        private val KEY_LAST_BIBLE_TRANSLATION = stringPreferencesKey("bible.lastTranslation")
        private val KEY_NOTES_LAST_PATH = stringPreferencesKey("notes.lastPath")
    }

    val defaultTranslation: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TRANSLATION] ?: "synodal"
    }

    val enabledTranslations: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_TRANSLATIONS]
            ?.filter { it in ALL_TRANSLATIONS }
            ?.takeIf { it.isNotEmpty() }
            ?: ALL_TRANSLATIONS
    }

    val appearanceMode: Flow<AppearanceMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_APPEARANCE]) {
            "light" -> AppearanceMode.LIGHT
            "dark" -> AppearanceMode.DARK
            else -> AppearanceMode.SYSTEM
        }
    }

    val verseScale: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_VERSE_SCALE]?.takeIf { it > 0f } ?: 1.0f
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING] ?: false
    }

    val lastBookId: Flow<Int> = dataStore.data.map { prefs -> prefs[KEY_LAST_BOOK_ID] ?: 1 }

    val lastChapter: Flow<Int> = dataStore.data.map { prefs -> prefs[KEY_LAST_CHAPTER] ?: 1 }

    val lastBibleTranslation: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_BIBLE_TRANSLATION] ?: ""
    }

    val notesLastPath: Flow<List<NotesPathEntry>> = dataStore.data
        .map { NotesPathCodec.decode(it[KEY_NOTES_LAST_PATH] ?: "") }

    suspend fun setDefaultTranslation(translation: String) {
        dataStore.edit { it[KEY_TRANSLATION] = translation }
    }

    suspend fun setEnabledTranslations(translations: List<String>) {
        dataStore.edit { it[KEY_ENABLED_TRANSLATIONS] = translations.toSet() }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        dataStore.edit { it[KEY_APPEARANCE] = mode.name.lowercase() }
    }

    suspend fun setVerseScale(scale: Float) {
        dataStore.edit { it[KEY_VERSE_SCALE] = scale.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[KEY_ONBOARDING] = true }
    }

    suspend fun setLastBookId(bookId: Int) {
        dataStore.edit { it[KEY_LAST_BOOK_ID] = bookId }
    }

    suspend fun setLastChapter(chapter: Int) {
        dataStore.edit { it[KEY_LAST_CHAPTER] = chapter }
    }

    suspend fun setLastBibleTranslation(translation: String) {
        dataStore.edit { it[KEY_LAST_BIBLE_TRANSLATION] = translation }
    }

    suspend fun setNotesLastPath(path: List<NotesPathEntry>) {
        dataStore.edit { it[KEY_NOTES_LAST_PATH] = NotesPathCodec.encode(path) }
    }
}