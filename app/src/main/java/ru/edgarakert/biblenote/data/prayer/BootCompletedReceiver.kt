package ru.edgarakert.biblenote.data.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.edgarakert.biblenote.data.settings.SettingsRepository

/**
 * AlarmManager-будильники сбрасываются перезагрузкой устройства и — на многих устройствах —
 * обновлением приложения. Перевзвод при холодном старте (BibleNoteApplication) обновление не
 * покрывает: пока пользователь не откроет приложение, напоминание молчало бы. Поэтому ловим и
 * MY_PACKAGE_REPLACED — система шлёт его только самому обновлённому приложению.
 * Если напоминание включено — перевзводим; иначе ничего не делаем. Идемпотентно: фиксированный
 * request code в [PrayerReminderScheduler] просто перезаписывает будильник.
 */
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {

    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeout(TIMEOUT_MS) {
                    val enabled = settings.prayerReminderEnabled.first()
                    if (enabled) {
                        val minutes = settings.prayerReminderMinutes.first()
                        PrayerReminderScheduler.schedule(appContext, minutes)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
