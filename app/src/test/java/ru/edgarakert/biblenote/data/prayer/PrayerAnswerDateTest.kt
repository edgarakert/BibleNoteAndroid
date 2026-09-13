package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerAnswerDateTest {

    @Test
    fun `pickerMillisForDate and dateForPickerMillis round trip`() {
        val date = LocalDate.of(2026, 3, 1)
        assertEquals(date, PrayerAnswerDate.dateForPickerMillis(PrayerAnswerDate.pickerMillisForDate(date)))
    }

    @Test
    fun `today selected in a negative offset zone resolves to the current moment`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = LocalDateTime.of(2026, 1, 15, 22, 30).atZone(zone).toInstant().toEpochMilli()
        val today = LocalDate.of(2026, 1, 15)
        val pickerMillis = PrayerAnswerDate.pickerMillisForDate(today)

        val resolved = PrayerAnswerDate.resolveAnsweredAt(pickerMillis, now, zone)

        assertEquals(now, resolved)
    }

    @Test
    fun `today selected in a positive offset zone resolves to the current moment`() {
        val zone = ZoneId.of("Asia/Tokyo")
        val now = LocalDateTime.of(2026, 1, 15, 7, 0).atZone(zone).toInstant().toEpochMilli()
        val today = LocalDate.of(2026, 1, 15)
        val pickerMillis = PrayerAnswerDate.pickerMillisForDate(today)

        val resolved = PrayerAnswerDate.resolveAnsweredAt(pickerMillis, now, zone)

        assertEquals(now, resolved)
    }

    /**
     * Это ровно тот баг, который поправка 1 к плану запрещает: если бы конвертация трактовала
     * `pickerMillis` (полночь UTC выбранного дня) как обычный `Instant` и сразу переводила его в
     * `America/Los_Angeles` (UTC-8), результат съехал бы на 14 января — сутками раньше того,
     * что выбрал пользователь.
     */
    @Test
    fun `a past date in a negative offset zone does not drift a day earlier`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val selected = LocalDate.of(2026, 1, 15)
        val pickerMillis = PrayerAnswerDate.pickerMillisForDate(selected)
        val now = LocalDateTime.of(2026, 1, 20, 9, 0).atZone(zone).toInstant().toEpochMilli()

        val resolved = PrayerAnswerDate.resolveAnsweredAt(pickerMillis, now, zone)
        val resolvedDate = Instant.ofEpochMilli(resolved).atZone(zone).toLocalDate()

        assertEquals(selected, resolvedDate)
    }

    @Test
    fun `a past date in a positive offset zone resolves to the same calendar day`() {
        val zone = ZoneId.of("Asia/Tokyo")
        val selected = LocalDate.of(2026, 1, 10)
        val pickerMillis = PrayerAnswerDate.pickerMillisForDate(selected)
        val now = LocalDateTime.of(2026, 1, 15, 9, 0).atZone(zone).toInstant().toEpochMilli()

        val resolved = PrayerAnswerDate.resolveAnsweredAt(pickerMillis, now, zone)
        val resolvedDate = Instant.ofEpochMilli(resolved).atZone(zone).toLocalDate()

        assertEquals(selected, resolvedDate)
    }

    @Test
    fun `a past date resolves to noon local time, not midnight`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val selected = LocalDate.of(2026, 1, 10)
        val pickerMillis = PrayerAnswerDate.pickerMillisForDate(selected)
        val now = LocalDateTime.of(2026, 1, 20, 9, 0).atZone(zone).toInstant().toEpochMilli()

        val resolved = PrayerAnswerDate.resolveAnsweredAt(pickerMillis, now, zone)
        val resolvedTime = Instant.ofEpochMilli(resolved).atZone(zone).toLocalTime()

        assertEquals(12, resolvedTime.hour)
        assertEquals(0, resolvedTime.minute)
    }
}
