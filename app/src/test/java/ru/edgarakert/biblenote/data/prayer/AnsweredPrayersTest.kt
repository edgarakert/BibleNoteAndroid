package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.LocalDateTime
import java.time.ZoneId

class AnsweredPrayersTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12): Long =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private fun answered(title: String, createdAt: Long, answeredAt: Long?) = PrayerRequest(
        title = title,
        status = PrayerStatus.ANSWERED,
        createdAt = createdAt,
        answeredAt = answeredAt,
        answerText = "ответ",
    )

    // ── группировка ───────────────────────────────────────────

    @Test
    fun `only answered requests are shown`() {
        val requests = listOf(
            answered("отвеченная", at(2026, 1, 1), at(2026, 2, 1)),
            PrayerRequest(title = "активная", status = PrayerStatus.ACTIVE),
            PrayerRequest(title = "доверенная", status = PrayerStatus.ENTRUSTED),
        )
        val groups = AnsweredPrayers.groupByYear(requests, zone)
        assertEquals(listOf("отвеченная"), groups.flatMap { g -> g.requests.map { it.title } })
    }

    @Test
    fun `requests are ordered by answer date, newest first`() {
        val requests = listOf(
            answered("март", at(2026, 1, 1), at(2026, 3, 1)),
            answered("май", at(2026, 1, 1), at(2026, 5, 1)),
            answered("апрель", at(2026, 1, 1), at(2026, 4, 1)),
        )
        val titles = AnsweredPrayers.groupByYear(requests, zone).single().requests.map { it.title }
        assertEquals(listOf("май", "апрель", "март"), titles)
    }

    @Test
    fun `groups are years in descending order`() {
        val requests = listOf(
            answered("2024", at(2024, 1, 1), at(2024, 6, 1)),
            answered("2026", at(2026, 1, 1), at(2026, 6, 1)),
            answered("2025", at(2025, 1, 1), at(2025, 6, 1)),
        )
        assertEquals(listOf(2026, 2025, 2024), AnsweredPrayers.groupByYear(requests, zone).map { it.year })
    }

    @Test
    fun `a missing answer date falls back to the creation date for grouping`() {
        val requests = listOf(answered("без даты", createdAt = at(2023, 5, 1), answeredAt = null))
        assertEquals(2023, AnsweredPrayers.groupByYear(requests, zone).single().year)
    }

    @Test
    fun `the year boundary follows the local zone, not UTC`() {
        // 1 января 00:30 по Москве — это ещё 31 декабря по UTC.
        val newYearNight = LocalDateTime.of(2026, 1, 1, 0, 30).atZone(zone).toInstant().toEpochMilli()
        val requests = listOf(answered("в новогоднюю ночь", at(2025, 6, 1), newYearNight))
        assertEquals(2026, AnsweredPrayers.groupByYear(requests, zone).single().year)
    }

    // ── длительность ──────────────────────────────────────────

    @Test
    fun `three months shows as months`() {
        assertEquals(
            PrayerDuration(PrayerDurationUnit.MONTHS, 3),
            AnsweredPrayers.duration(at(2026, 6, 13), at(2026, 9, 13), zone)
        )
    }

    @Test
    fun `thirteen months shows as one year`() {
        assertEquals(
            PrayerDuration(PrayerDurationUnit.YEARS, 1),
            AnsweredPrayers.duration(at(2025, 8, 1), at(2026, 9, 1), zone)
        )
    }

    @Test
    fun `ten days shows as one week`() {
        assertEquals(
            PrayerDuration(PrayerDurationUnit.WEEKS, 1),
            AnsweredPrayers.duration(at(2026, 9, 1), at(2026, 9, 11), zone)
        )
    }

    @Test
    fun `six days shows as days`() {
        assertEquals(
            PrayerDuration(PrayerDurationUnit.DAYS, 6),
            AnsweredPrayers.duration(at(2026, 9, 1), at(2026, 9, 7), zone)
        )
    }

    @Test
    fun `days are calendar days, not 24-hour windows`() {
        // 23:00 → следующий день 01:00: прошло два часа, но это другой календарный день.
        assertEquals(
            PrayerDuration(PrayerDurationUnit.DAYS, 1),
            AnsweredPrayers.duration(at(2026, 9, 1, 23), at(2026, 9, 2, 1), zone)
        )
    }

    @Test
    fun `an answer on the same day shows no duration`() {
        assertNull(AnsweredPrayers.duration(at(2026, 9, 13, 8), at(2026, 9, 13, 20), zone))
    }

    @Test
    fun `a missing answer date shows no duration`() {
        assertNull(AnsweredPrayers.duration(at(2026, 9, 1), null, zone))
    }

    @Test
    fun `an answer dated before creation shows no duration rather than a negative one`() {
        assertNull(AnsweredPrayers.duration(at(2026, 9, 10), at(2026, 9, 1), zone))
    }
}
