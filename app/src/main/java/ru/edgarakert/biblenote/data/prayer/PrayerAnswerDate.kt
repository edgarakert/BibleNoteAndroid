package ru.edgarakert.biblenote.data.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Конвертация даты ответа из Material 3 `DatePicker` (задача 14.10, поправка 1 к плану).
 *
 * `DatePickerState.selectedDateMillis` — это ВСЕГДА начало выбранного календарного дня в UTC,
 * а не локальный момент времени. Наивная трактовка этих миллисекунд как обычного `Instant` и
 * форматирование в локальной зоне сдвигает дату на сутки назад в зонах с отрицательным
 * смещением (например, `America/Los_Angeles`, UTC-8): полночь 15 января по UTC — это ещё
 * 16:00 14 января по местному времени.
 *
 * Правило здесь то же самое, которым живёт сам `DatePicker`: календарный день, который видел
 * пользователь, — это `LocalDate`, полученный через `.atZone(ZoneOffset.UTC).toLocalDate()`,
 * а не через локальную зону. [pickerMillisForDate]/[dateForPickerMillis] — конвертация в обе
 * стороны по этому правилу, [resolveAnsweredAt] — итоговый момент времени в локальной зоне.
 */
object PrayerAnswerDate {

    /** `LocalDate` → миллисекунды, которые `DatePicker` считает «этим днём» (полночь UTC). */
    fun pickerMillisForDate(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Обратная операция: то, что вернул `DatePickerState.selectedDateMillis`, → `LocalDate`. */
    fun dateForPickerMillis(pickerMillisUtc: Long): LocalDate =
        Instant.ofEpochMilli(pickerMillisUtc).atZone(ZoneOffset.UTC).toLocalDate()

    /**
     * Момент времени ответа в [zone]: если выбран сегодняшний (локальный) день — [now]
     * (ответ только что случился, время суток имеет смысл); если выбран прошлый день —
     * полдень этого дня в [zone] (время суток произвольно, но полдень не «уезжает» на
     * соседний календарный день ни при каком смещении зоны, в отличие от полуночи).
     */
    fun resolveAnsweredAt(
        pickerMillisUtc: Long,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val selectedDate = dateForPickerMillis(pickerMillisUtc)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return if (selectedDate == today) {
            now
        } else {
            selectedDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        }
    }
}
