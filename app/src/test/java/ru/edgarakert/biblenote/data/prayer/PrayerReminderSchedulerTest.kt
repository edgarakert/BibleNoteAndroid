package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerReminderSchedulerTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private fun at(d: Int, h: Int, m: Int) =
        LocalDateTime.of(2026, 9, d, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `next trigger is today when the time has not passed yet`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 7, 0), zone = zone
        )
        assertEquals(at(5, 9, 0), next)
    }

    @Test
    fun `next trigger is tomorrow when the time has already passed`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 10, 0), zone = zone
        )
        assertEquals(at(6, 9, 0), next)
    }

    @Test
    fun `a time equal to now counts as already passed`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 9, 0), zone = zone
        )
        assertEquals(at(6, 9, 0), next)
    }

    @Test
    fun `minutes since midnight map to hour and minute`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 21 * 60 + 30, now = at(5, 7, 0), zone = zone
        )
        assertEquals(at(5, 21, 30), next)
    }
}
