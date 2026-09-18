package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

enum class PrayerDurationUnit { YEARS, MONTHS, WEEKS, DAYS }

/** Сколько молились: одна самая крупная ненулевая единица («3 месяца», а не «3 месяца 4 дня»). */
data class PrayerDuration(val unit: PrayerDurationUnit, val amount: Int)

/** Год ответа и отвеченные в этом году просьбы, свежие сверху. */
data class AnsweredYearGroup(val year: Int, val requests: List<PrayerRequest>)

/**
 * Логика экрана «Отвеченные молитвы» (задача 14.12). Чистые функции — всё, что зависит от
 * календаря, считается в переданной зоне, чтобы тесты не зависели от зоны машины.
 */
object AnsweredPrayers {

    /**
     * Только ANSWERED, по дате ответа от свежих к старым, группами по году — годы по убыванию.
     * Дата ответа может отсутствовать у данных, записанных до появления поля, — тогда для
     * сортировки и выбора года берётся дата создания.
     */
    fun groupByYear(
        requests: List<PrayerRequest>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<AnsweredYearGroup> = requests
        .filter { it.status == PrayerStatus.ANSWERED }
        .sortedByDescending { it.effectiveAnsweredAt }
        .groupBy { localDate(it.effectiveAnsweredAt, zone).year }
        .map { (year, items) -> AnsweredYearGroup(year, items) }
        .sortedByDescending { it.year }

    /**
     * От создания до ответа в календарных днях локальной зоны — так же, как «помолились сегодня»
     * в [PrayerActions]. Возвращает null, когда показывать нечего: ответа нет, ответ в тот же
     * день («молились 0 дней» звучит странно) или дата ответа раньше создания — у старых данных;
     * выбор такой даты в интерфейсе заблокирован, но отрицательную длительность не показываем.
     */
    fun duration(
        createdAt: Long,
        answeredAt: Long?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PrayerDuration? {
        if (answeredAt == null) return null
        val from = localDate(createdAt, zone)
        val to = localDate(answeredAt, zone)
        if (!to.isAfter(from)) return null

        val period = Period.between(from, to)
        return when {
            period.years > 0 -> PrayerDuration(PrayerDurationUnit.YEARS, period.years)
            period.months > 0 -> PrayerDuration(PrayerDurationUnit.MONTHS, period.months)
            period.days >= 7 -> PrayerDuration(PrayerDurationUnit.WEEKS, period.days / 7)
            else -> PrayerDuration(PrayerDurationUnit.DAYS, period.days)
        }
    }

    private val PrayerRequest.effectiveAnsweredAt: Long get() = answeredAt ?: createdAt

    private fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
}
