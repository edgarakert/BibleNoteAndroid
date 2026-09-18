package ru.edgarakert.biblenote.data.prayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.edgarakert.biblenote.MainActivity
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.settings.SettingsRepository

/**
 * Срабатывает по будильнику от [PrayerReminderScheduler]. Показывает ОБЩЕЕ напоминание
 * (без количества просьб — это гарантия приватности: содержимое молитвенного журнала
 * не должно раскрываться на локскрине) и перевзводит будильник на следующий день.
 *
 * Настройки читаются из общего DataStore-синглтона через Koin ([SettingsRepository]),
 * а не из отдельного экземпляра — второй DataStore на тот же файл упал бы с
 * IllegalStateException. Koin гарантированно уже запущен: onCreate() приложения
 * выполняется раньше, чем система сможет доставить любой broadcast.
 */
class PrayerReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        // onReceive не suspend, а чтение DataStore — suspend: goAsync() продлевает жизнь
        // приёмника на время короткой корутины вместо runBlocking на потоке приёмника.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeout(TIMEOUT_MS) {
                    // Будильник мог уже быть в пути, когда пользователь выключил напоминание:
                    // cancel() не отзывает доставку, начатую системой. Без этой проверки
                    // приёмник показал бы уведомление и взвёл будильник на завтра при
                    // выключенной настройке — настройка и будильник разошлись бы.
                    if (!settings.prayerReminderEnabled.first()) return@withTimeout

                    showNotificationIfPermitted(appContext)
                    // Перевзводим независимо от того, показалось ли уведомление —
                    // разрешение POST_NOTIFICATIONS может быть выдано позже, а будильник
                    // должен продолжать тикать: план требует "одно напоминание за раз".
                    val minutes = settings.prayerReminderMinutes.first()
                    PrayerReminderScheduler.schedule(appContext, minutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotificationIfPermitted(context: Context) {
        ensureChannel(context)
        // Без разрешения не падаем — просто не показываем уведомление в этот раз;
        // перевзвод будильника вызывающий делает в любом случае.
        // Явная проверка POST_NOTIFICATIONS нужна поверх areNotificationsEnabled():
        // функционально на Android 13+ второе и так учитывает разрешение, но lint этого
        // не видит и помечает notify() как MissingPermission (уровень Error).
        // areNotificationsEnabled() остаётся — он ловит уведомления, выключенные
        // пользователем в настройках на любых версиях.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.prayer_reminder_title))
            .setContentText(context.getString(R.string.prayer_reminder_body_generic))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Текст и так общий, без деталей журнала, но VISIBILITY_PRIVATE — дополнительная
            // страховка на случай кастомных лаунчеров/локскринов, которые вытягивают extras.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Идемпотентно: createNotificationChannel можно вызывать повторно без побочных эффектов. */
    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.prayer_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.prayer_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "prayer_reminder"
        private const val NOTIFICATION_ID = 4209
        private const val REQUEST_CODE_CONTENT = 4210
        private const val TIMEOUT_MS = 10_000L
    }
}
