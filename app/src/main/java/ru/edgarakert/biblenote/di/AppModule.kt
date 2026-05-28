package ru.edgarakert.biblenote.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.ui.viewmodels.NotesViewModel
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.AppDatabase
import ru.edgarakert.biblenote.data.db.FolderDao
import ru.edgarakert.biblenote.data.db.NoteDao
import ru.edgarakert.biblenote.data.settings.SettingsRepository

private val Context.settingsDataStore by preferencesDataStore(name = "biblenote_settings")

val appModule = module {
    // Dispatchers
    single<CoroutineDispatcher>(named("IO")) { Dispatchers.IO }

    // Room
    single<AppDatabase> { AppDatabase.create(androidContext()) }
    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<NoteRepository> { NoteRepository(get(), get()) }

    // Bible
    single<BibleDatabaseService> { BibleDatabaseService(androidContext(), get(named("IO"))) }
    single<BibleReferenceParser> { BibleReferenceParser() }

    // Settings
    single<DataStore<Preferences>> { androidContext().settingsDataStore }
    single<SettingsRepository> { SettingsRepository(get()) }

    // ViewModels
    viewModel { NotesViewModel(get()) }
}
