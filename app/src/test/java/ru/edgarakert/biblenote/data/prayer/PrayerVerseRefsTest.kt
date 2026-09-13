package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser

class PrayerVerseRefsTest {

    private val parser = BibleReferenceParser()

    // ── Разбор смешанного ввода — ключевой пример из поправок к плану ─────────

    @Test
    fun `mixed input keeps the comma-attached verse list on the second reference`() {
        val refs = PrayerVerseRefs.fromInput("Флп 4:6-7, Быт 1:2,5", parser)
        assertEquals(listOf("Флп 4:6-7", "Быт 1:2,5"), refs)
    }

    // ── Пустой ввод и мусор ─────────────────────────────────────────────────

    @Test
    fun `empty input returns empty list`() {
        assertTrue(PrayerVerseRefs.fromInput("", parser).isEmpty())
    }

    @Test
    fun `blank input returns empty list`() {
        assertTrue(PrayerVerseRefs.fromInput("   ", parser).isEmpty())
    }

    @Test
    fun `garbage without any reference returns empty list`() {
        assertTrue(PrayerVerseRefs.fromInput("просто текст без ссылок, ещё текст", parser).isEmpty())
    }

    // ── Лишние пробелы и запятые ────────────────────────────────────────────

    @Test
    fun `extra spaces and stray commas around references are tolerated`() {
        val refs = PrayerVerseRefs.fromInput("  Ин 3:16 ,,  Флп 4:6-7   ", parser)
        assertEquals(listOf("Ин 3:16", "Флп 4:6-7"), refs)
    }

    @Test
    fun `single reference with no separators parses to itself`() {
        val refs = PrayerVerseRefs.fromInput("Рим 8:28", parser)
        assertEquals(listOf("Рим 8:28"), refs)
    }

    // ── Дубликаты ───────────────────────────────────────────────────────────

    @Test
    fun `duplicate references are collapsed keeping first occurrence order`() {
        val refs = PrayerVerseRefs.fromInput("Ин 3:16, Рим 8:28, Ин 3:16", parser)
        assertEquals(listOf("Ин 3:16", "Рим 8:28"), refs)
    }

    // ── toInput ─────────────────────────────────────────────────────────────

    @Test
    fun `toInput joins references with a comma and space`() {
        assertEquals("Ин 3:16, Флп 4:6-7", PrayerVerseRefs.toInput(listOf("Ин 3:16", "Флп 4:6-7")))
    }

    @Test
    fun `toInput of empty list is empty string`() {
        assertEquals("", PrayerVerseRefs.toInput(emptyList()))
    }

    // ── Round-trip toInput → fromInput ──────────────────────────────────────

    @Test
    fun `round trip toInput then fromInput yields the same list`() {
        val original = listOf("Флп 4:6-7", "Быт 1:2,5", "Ин 3:16")
        val roundTripped = PrayerVerseRefs.fromInput(PrayerVerseRefs.toInput(original), parser)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `round trip of a single non-contiguous reference`() {
        val original = listOf("Быт 1:2,5")
        val roundTripped = PrayerVerseRefs.fromInput(PrayerVerseRefs.toInput(original), parser)
        assertEquals(original, roundTripped)
    }
}
