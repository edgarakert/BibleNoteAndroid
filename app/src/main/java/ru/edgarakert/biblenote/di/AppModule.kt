package ru.edgarakert.biblenote.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.AppDatabase
import ru.edgarakert.biblenote.data.db.FolderDao
import ru.edgarakert.biblenote.data.db.NoteDao
import ru.edgarakert.biblenote.data.settings.SettingsRepository

private val Context.settingsDataStore by preferencesDataStore(name = "biblenote_settings")

val appModule = module {
    // Room
    single<AppDatabase> { AppDatabase.create(androidContext()) }
    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<NoteRepository> { NoteRepository(get(), get()) }

    // Bible
    single<BibleDatabaseService> { BibleDatabaseService(androidContext()) }
    single<BibleReferenceParser> { BibleReferenceParser() }

    // Settings
    single<DataStore<Preferences>> { androidContext().settingsDataStore }
    single<SettingsRepository> { SettingsRepository(get()) }
}
