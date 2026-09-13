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
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.ui.viewmodels.NoteEditorViewModel
import ru.edgarakert.biblenote.ui.viewmodels.NotesViewModel
import ru.edgarakert.biblenote.ui.viewmodels.PrayerEditorViewModel
import ru.edgarakert.biblenote.ui.viewmodels.PrayerListViewModel
import ru.edgarakert.biblenote.ui.viewmodels.PrayerTodayViewModel
import ru.edgarakert.biblenote.data.bible.BibleDatabaseService
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.AppDatabase
import ru.edgarakert.biblenote.data.db.FolderDao
import ru.edgarakert.biblenote.data.db.NoteDao
import ru.edgarakert.biblenote.data.db.PrayerDao
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.ui.viewmodels.BibleReaderViewModel
import ru.edgarakert.biblenote.ui.viewmodels.BibleVerseSheetViewModel
import ru.edgarakert.biblenote.ui.viewmodels.FolderViewModel
import ru.edgarakert.biblenote.ui.viewmodels.SaveVersesToNoteViewModel
import ru.edgarakert.biblenote.ui.viewmodels.SettingsViewModel

private val Context.settingsDataStore by preferencesDataStore(name = "biblenote_settings")

val appModule = module {
    // Dispatchers
    single<CoroutineDispatcher>(named("IO")) { Dispatchers.IO }

    // Room
    single<AppDatabase> { AppDatabase.create(androidContext()) }

    single<NoteDao> { get<AppDatabase>().noteDao() }

    single<FolderDao> { get<AppDatabase>().folderDao() }

    single<PrayerDao> { get<AppDatabase>().prayerDao() }

    single<NoteRepository> {
        NoteRepository(
            noteDao = get<NoteDao>(),
            folderDao = get<FolderDao>()
        )
    }

    single<PrayerRepository> { PrayerRepository(get()) }

    // Bible
    single<BibleDatabaseService> {
        BibleDatabaseService(
            context = androidContext(),
            ioDispatcher = get(named("IO"))
        )
    }
    single<BibleReferenceParser> { BibleReferenceParser() }

    // Settings
    single<DataStore<Preferences>> { androidContext().settingsDataStore }

    single<SettingsRepository> { SettingsRepository(dataStore = get<DataStore<Preferences>>()) }

    // ViewModels
    viewModel {
        BibleReaderViewModel(
            bibleService = get<BibleDatabaseService>(),
            settingsRepository = get<SettingsRepository>(),
            parser = get<BibleReferenceParser>(),
            repository = get<NoteRepository>()
        )
    }
    viewModel { NotesViewModel(repository = get<NoteRepository>()) }

    viewModel { params ->
        NoteEditorViewModel(
            noteId = params.get<Long>(),
            repository = get<NoteRepository>()
        )
    }

    viewModel { params ->
        BibleVerseSheetViewModel(
            reference = params.get<BibleReference>(),
            bibleService = get<BibleDatabaseService>(),
            settingsRepository = get<SettingsRepository>()
        )
    }

    viewModel { SettingsViewModel(repository = get<SettingsRepository>()) }

    viewModel { PrayerTodayViewModel(repository = get<PrayerRepository>()) }

    viewModel { PrayerListViewModel(repository = get<PrayerRepository>()) }

    viewModel { params ->
        PrayerEditorViewModel(
            requestId = params.get<Long>(),
            repository = get<PrayerRepository>(),
            parser = get<BibleReferenceParser>()
        )
    }

    viewModel { params ->
        FolderViewModel(
            folderId = params.get<Long>(),
            repository = get<NoteRepository>()
        )
    }

    viewModel { params ->
        SaveVersesToNoteViewModel(
            bookName = params.get<String>(),
            chapter = params.get<Int>(),
            verseNumbers = params.get<List<Int>>(),
            repository = get<NoteRepository>()
        )
    }
}
