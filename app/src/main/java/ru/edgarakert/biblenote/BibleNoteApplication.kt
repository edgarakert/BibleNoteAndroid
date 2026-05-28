package ru.edgarakert.biblenote

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.edgarakert.biblenote.di.appModule

class BibleNoteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BibleNoteApplication)
            modules(appModule)
        }
    }
}
