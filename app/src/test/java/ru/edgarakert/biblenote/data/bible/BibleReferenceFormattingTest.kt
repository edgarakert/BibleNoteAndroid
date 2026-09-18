package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleReferenceFormattingTest {

    private fun ref(
        bookId: Int = 1,
        chapter: Int = 2,
        verseStart: Int? = null,
        verseEnd: Int? = null,
        verseList: List<Int> = emptyList(),
        displayText: String = "Быт 2"
    ) = BibleReference(bookId, chapter, verseStart, verseEnd, verseList, displayText, 0, displayText.length)

    // ── formatVerseSpec ────────────────────────────────────────

    @Test
    fun `formatVerseSpec of empty list is empty string`() {
        assertEquals("", BibleReference.formatVerseSpec(emptyList()))
    }

    @Test
    fun `formatVerseSpec of single verse is bare number`() {
        assertEquals("14", BibleReference.formatVerseSpec(listOf(14)))
    }

    @Test
    fun `formatVerseSpec collapses a consecutive run`() {
        assertEquals("14-18", BibleReference.formatVerseSpec(listOf(14, 15, 16, 17, 18)))
    }

    @Test
    fun `formatVerseSpec mixes a run and a single`() {
        assertEquals("14-16,20", BibleReference.formatVerseSpec(listOf(14, 15, 16, 20)))
    }

    @Test
    fun `formatVerseSpec sorts unsorted input`() {
        assertEquals("14-15,18", BibleReference.formatVerseSpec(listOf(18, 14, 15)))
    }

    @Test
    fun `formatVerseSpec deduplicates`() {
        assertEquals("14-15", BibleReference.formatVerseSpec(listOf(14, 14, 15)))
    }

    @Test
    fun `formatVerseSpec honours a custom separator`() {
        assertEquals("14–15", BibleReference.formatVerseSpec(listOf(14, 15), rangeSeparator = "–"))
    }

    @Test
    fun `formatVerseSpec defaults to an ASCII hyphen so the parser can read it back`() {
        val spec = BibleReference.formatVerseSpec(listOf(14, 15, 16))
        assertTrue(spec.contains('-'))
        assertFalse(spec.contains('–'))
    }

    // ── coveredVerses ──────────────────────────────────────────

    @Test
    fun `coveredVerses of a whole chapter is empty`() {
        assertEquals(emptyList<Int>(), ref().coveredVerses)
    }

    @Test
    fun `coveredVerses of a single verse is that verse`() {
        assertEquals(listOf(14), ref(verseStart = 14).coveredVerses)
    }

    @Test
    fun `coveredVerses of a range expands it`() {
        assertEquals(listOf(14, 15, 16), ref(verseStart = 14, verseEnd = 16).coveredVerses)
    }

    @Test
    fun `coveredVerses prefers verseList over the stale range fields`() {
        // Парсер для "Быт 2:14-15,20" заполняет и verseStart/verseEnd (только первый сегмент),
        // и verseList (полный набор). Авторитетен verseList.
        val r = ref(verseStart = 14, verseEnd = 15, verseList = listOf(14, 15, 20))
        assertEquals(listOf(14, 15, 20), r.coveredVerses)
    }

    // ── replacementText ────────────────────────────────────────

    @Test
    fun `replacementText adds a spec to a whole-chapter reference`() {
        assertEquals("Быт 2:14-16", BibleReference.replacementText("Быт 2", listOf(14, 15, 16)))
    }

    @Test
    fun `replacementText replaces an existing spec`() {
        assertEquals("Быт 2:14", BibleReference.replacementText("Быт 2:5", listOf(14)))
    }

    @Test
    fun `replacementText with an empty selection degrades to the whole chapter`() {
        assertEquals("Быт 2", BibleReference.replacementText("Быт 2:14-18", emptyList()))
    }

    @Test
    fun `replacementText preserves the abbreviation exactly as typed`() {
        assertEquals("быт.3:7", BibleReference.replacementText("быт.3:2", listOf(7)))
    }

    @Test
    fun `replacementText survives a numeric book prefix`() {
        assertEquals("1 Кор 13:4-8", BibleReference.replacementText("1 Кор 13:4-7", listOf(4, 5, 6, 7, 8)))
    }

    // ── round trip through the real parser ─────────────────────

    @Test
    fun `replacement text re-parses to the same verses`() {
        val written = BibleReference.replacementText("Быт 2", listOf(14, 15, 16, 20))
        val parsed = BibleReferenceParser().parse(written)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed[0].bookId)
        assertEquals(2, parsed[0].chapter)
        assertEquals(listOf(14, 15, 16, 20), parsed[0].coveredVerses)
    }

    @Test
    fun `an emptied selection re-parses as a whole chapter`() {
        val written = BibleReference.replacementText("Быт 2:14-18", emptyList())
        val parsed = BibleReferenceParser().parse(written)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].isWholeChapter)
        assertEquals(emptyList<Int>(), parsed[0].coveredVerses)
    }
}
