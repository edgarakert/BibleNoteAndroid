package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Действия над просьбой. Чистые функции над копиями: сохраняет вызывающий.
 * Здесь нет «серий», «просрочек» и «пропусков» — только факты, это продуктовое требование.
 */
object PrayerActions {

    /** Сравнение календарных дней в локальной зоне, а не окна в 24 часа. */
    fun hasPrayedToday(
        request: PrayerRequest,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val last = request.lastPrayedAt ?: return false
        val lastDay = Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return lastDay == today
    }

    /**
     * Идемпотентно в рамках календарного дня: повторный вызов в тот же день
     * не увеличивает счётчик и не сдвигает lastPrayedAt на более позднее время.
     */
    fun markPrayedToday(
        request: PrayerRequest,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): PrayerRequest {
        if (hasPrayedToday(request, now, zone)) return request
        return request.copy(lastPrayedAt = now, prayedDaysCount = request.prayedDaysCount + 1)
    }

    /**
     * Возвращает null, если текст ответа пуст после trim — и тогда НИЧЕГО не меняется.
     * Это инвариант уровня модели: кнопка «Сохранить» блокируется поверх него, а не вместо.
     */
    fun markAnswered(
        request: PrayerRequest,
        answerText: String,
        answeredAt: Long = System.currentTimeMillis(),
    ): PrayerRequest? {
        val trimmed = answerText.trim()
        if (trimmed.isEmpty()) return null
        return request.copy(
            answerText = trimmed,
            answeredAt = answeredAt,
            status = PrayerStatus.ANSWERED
        )
    }

    /** «Доверить Богу»: меняется только статус, поля ответа остаются пустыми. */
    fun markEntrusted(request: PrayerRequest): PrayerRequest =
        request.copy(status = PrayerStatus.ENTRUSTED)
}
