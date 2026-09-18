package ru.edgarakert.biblenote.data.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

object PrayerReminderScheduler {

    /** Один будильник за раз — фиксированный request code, повторный вызов перезаписывает его. */
    private const val REQUEST_CODE = 4208

    /** Не короче 10 минут: на Android 14+ система сама расширяет более узкие окна до этого минимума. */
    private const val DELIVERY_WINDOW_MS = 15L * 60 * 1000

    /**
     * Ближайшее срабатывание для времени, заданного минутами от полуночи.
     * Момент, равный «сейчас», считается уже прошедшим — переносим на завтра.
     */
    fun nextTriggerAt(
        minutesSinceMidnight: Int,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        val candidate = nowZoned
            .withHour(minutesSinceMidnight / 60)
            .withMinute(minutesSinceMidnight % 60)
            .withSecond(0)
            .withNano(0)

        val target = if (candidate.toInstant().toEpochMilli() > now) candidate else candidate.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    fun schedule(context: Context, minutesSinceMidnight: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // setWindow с окном 15 минут — решение владельца продукта. Было setAndAllowWhileIdle:
        // система растягивала его окно до часа (dumpsys alarm: window=+1h), и напоминание на
        // 09:00 могло прийти в 09:50. Точный будильник требует SCHEDULE_EXACT_ALARM, который
        // Google Play разрешает только будильникам и календарям. Цена setWindow: он не
        // срабатывает в режиме Doze и откладывается до ближайшего окна обслуживания, если
        // телефон долго лежит неподвижно.
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt(minutesSinceMidnight),
            DELIVERY_WINDOW_MS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PrayerReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
