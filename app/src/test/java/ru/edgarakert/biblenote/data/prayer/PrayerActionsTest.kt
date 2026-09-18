package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerActionsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun request() = PrayerRequest(id = 1, title = "О работе")

    @Test
    fun `hasPrayedToday is false when never prayed`() {
        assertFalse(PrayerActions.hasPrayedToday(request(), at(2026, 9, 5, 12, 0), zone))
    }

    @Test
    fun `markPrayedToday increments on the first call of the day`() {
        val now = at(2026, 9, 5, 8, 0)
        val result = PrayerActions.markPrayedToday(request(), now, zone)
        assertEquals(1, result.prayedDaysCount)
        assertEquals(now, result.lastPrayedAt)
    }

    @Test
    fun `markPrayedToday does not double count on the same day`() {
        val morning = at(2026, 9, 5, 8, 0)
        val evening = at(2026, 9, 5, 20, 0)
        val once = PrayerActions.markPrayedToday(request(), morning, zone)
        val twice = PrayerActions.markPrayedToday(once, evening, zone)

        assertEquals(1, twice.prayedDaysCount)
        // lastPrayedAt НЕ обновляется на более позднее время того же дня
        assertEquals(morning, twice.lastPrayedAt)
        assertSame(once, twice)
    }

    @Test
    fun `markPrayedToday counts again on a new day`() {
        val today = at(2026, 9, 5, 20, 0)
        val tomorrow = at(2026, 9, 6, 8, 0)
        val once = PrayerActions.markPrayedToday(request(), today, zone)
        val twice = PrayerActions.markPrayedToday(once, tomorrow, zone)
        assertEquals(2, twice.prayedDaysCount)
    }

    @Test
    fun `a gap of many days does not reset the count`() {
        val first = PrayerActions.markPrayedToday(request(), at(2026, 1, 1, 9, 0), zone)
        val later = PrayerActions.markPrayedToday(first, at(2026, 9, 5, 9, 0), zone)
        assertEquals(2, later.prayedDaysCount)
    }

    @Test
    fun `markAnswered rejects blank text and changes nothing`() {
        val original = request()
        val result = PrayerActions.markAnswered(original, "   ", at(2026, 9, 5, 9, 0))
        assertNull(result)
        assertEquals(PrayerStatus.ACTIVE, original.status)
        assertNull(original.answerText)
    }

    @Test
    fun `markAnswered stores the trimmed text and switches status`() {
        val at = at(2026, 9, 5, 9, 0)
        val result = PrayerActions.markAnswered(request(), "  Бог дал работу через две недели  ", at)!!
        assertEquals(PrayerStatus.ANSWERED, result.status)
        assertEquals("Бог дал работу через две недели", result.answerText)
        assertEquals(at, result.answeredAt)
    }

    @Test
    fun `markEntrusted sets the status without touching the answer fields`() {
        val result = PrayerActions.markEntrusted(request())
        assertEquals(PrayerStatus.ENTRUSTED, result.status)
        assertNull(result.answerText)
        assertNull(result.answeredAt)
    }
}
