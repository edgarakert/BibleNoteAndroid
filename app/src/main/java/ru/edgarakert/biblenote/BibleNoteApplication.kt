package ru.edgarakert.biblenote

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.edgarakert.biblenote.data.prayer.PrayerReminderScheduler
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.di.appModule

class BibleNoteApplication : Application() {

    /** Живёт весь процесс — не привязан к жизненному циклу конкретного экрана. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidContext(this@BibleNoteApplication)
            modules(appModule)
        }

        // AlarmManager-будильники слетают не только при перезагрузке, но и при
        // принудительной остановке приложения — BootCompletedReceiver такое не ловит.
        // Перевзводим здесь при каждом холодном старте процесса для паритета с iOS
        // (там напоминание перевзводится при каждом возврате в приложение) и на всякий
        // случай, если будильник был потерян. Не runBlocking: DataStore читается в
        // фоновой корутине application-scope, старт приложения не блокируется.
        // ProcessLifecycleOwner/lifecycle-process намеренно не подключены — в проекте
        // такой зависимости ещё нет, а холодный старт процесса — единственный момент,
        // когда будильник действительно мог пропасть незамеченным (пока живой процесс
        // не убивают, взведённый AlarmManager-будильник переживает уходы в фон).
        val settingsRepository = koinApp.koin.get<SettingsRepository>()
        applicationScope.launch {
            if (settingsRepository.prayerReminderEnabled.first()) {
                val minutes = settingsRepository.prayerReminderMinutes.first()
                PrayerReminderScheduler.schedule(this@BibleNoteApplication, minutes)
            }
        }
    }
}
