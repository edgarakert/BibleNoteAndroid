package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerSelectionServiceTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private fun at(d: Int, h: Int) =
        LocalDateTime.of(2026, 9, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private val now = at(5, 12)

    private fun req(
        title: String,
        status: PrayerStatus = PrayerStatus.ACTIVE,
        lastPrayedAt: Long? = null,
    ) = PrayerRequest(title = title, status = status, lastPrayedAt = lastPrayedAt)

    @Test
    fun `shows all active requests regardless of count`() {
        val requests = (1..20).map { req("Просьба $it") }
        assertEquals(20, PrayerSelectionService.todaysSelection(requests, now, zone).size)
    }

    @Test
    fun `excludes every non-active status`() {
        val requests = listOf(
            req("активная"),
            req("отвеченная", PrayerStatus.ANSWERED),
            req("доверенная", PrayerStatus.ENTRUSTED),
            req("архивная", PrayerStatus.ARCHIVED),
        )
        assertEquals(
            listOf("активная"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `not prayed today comes before prayed today`() {
        val requests = listOf(req("помолились", lastPrayedAt = at(5, 9)), req("не молились"))
        assertEquals(
            listOf("не молились", "помолились"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `prayed on an earlier day counts as not prayed today`() {
        val requests = listOf(req("сегодня", lastPrayedAt = at(5, 9)), req("вчера", lastPrayedAt = at(4, 9)))
        assertEquals(
            listOf("вчера", "сегодня"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `preserves input order within each bucket`() {
        val requests = listOf(
            req("a"), req("b"),
            req("c", lastPrayedAt = at(5, 9)), req("d", lastPrayedAt = at(5, 10))
        )
        assertEquals(
            listOf("a", "b", "c", "d"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }
}
