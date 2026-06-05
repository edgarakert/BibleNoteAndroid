package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleReferenceParserTest {

    private val parser = BibleReferenceParser()

    // ── Empty / no-op ────────────────────────────────────────────────────────

    @Test
    fun `empty string returns empty list`() {
        assertTrue(parser.parse("").isEmpty())
    }

    @Test
    fun `plain sentence without references returns empty list`() {
        assertTrue(parser.parse("This is a regular sentence with no Bible references.").isEmpty())
    }

    // ── English references ───────────────────────────────────────────────────

    @Test
    fun `parses EN reference with chapter and verse`() {
        val refs = parser.parse("I read John 3:16 today")
        assertEquals(1, refs.size)
        with(refs[0]) {
            assertEquals(43, bookId)
            assertEquals(3, chapter)
            assertEquals(16, verseStart)
            assertNull(verseEnd)
            assertTrue(verseList.isEmpty())
        }
    }

    @Test
    fun `parses EN abbreviation Matt`() {
        val refs = parser.parse("Matt 5:3")
        assertEquals(1, refs.size)
        assertEquals(40, refs[0].bookId)
        assertEquals(5, refs[0].chapter)
        assertEquals(3, refs[0].verseStart)
    }

    @Test
    fun `parses EN Revelation as last book`() {
        val refs = parser.parse("Rev 22:21")
        assertEquals(1, refs.size)
        assertEquals(66, refs[0].bookId)
        assertEquals(22, refs[0].chapter)
        assertEquals(21, refs[0].verseStart)
    }

    @Test
    fun `parses EN Genesis as first book`() {
        val refs = parser.parse("Gen 1:1")
        assertEquals(1, refs.size)
        assertEquals(1, refs[0].bookId)
        assertEquals(1, refs[0].chapter)
        assertEquals(1, refs[0].verseStart)
    }

    @Test
    fun `parses EN chapter-only reference`() {
        val refs = parser.parse("See John 3 for details")
        assertEquals(1, refs.size)
        assertEquals(43, refs[0].bookId)
        assertEquals(3, refs[0].chapter)
        assertNull(refs[0].verseStart)
        assertTrue(refs[0].isWholeChapter)
    }

    @Test
    fun `parses EN verse range`() {
        val refs = parser.parse("John 3:16-18")
        assertEquals(1, refs.size)
        assertEquals(16, refs[0].verseStart)
        assertEquals(18, refs[0].verseEnd)
        assertTrue(refs[0].verseList.isEmpty())
    }

    @Test
    fun `parses EN numbered book 1 Cor`() {
        val refs = parser.parse("1 Cor 3:16")
        assertEquals(1, refs.size)
        assertEquals(46, refs[0].bookId)
    }

    @Test
    fun `parses EN numbered book 2Tim without space`() {
        val refs = parser.parse("2Tim 3:16")
        assertEquals(1, refs.size)
        assertEquals(55, refs[0].bookId)
    }

    // ── Russian references ───────────────────────────────────────────────────

    @Test
    fun `parses RU reference with dot separator`() {
        val refs = parser.parse("Сегодня читал Ин. 3:16")
        assertEquals(1, refs.size)
        assertEquals(43, refs[0].bookId)
        assertEquals(3, refs[0].chapter)
        assertEquals(16, refs[0].verseStart)
    }

    @Test
    fun `parses RU full book name`() {
        val refs = parser.parse("Иоанна 3:16")
        assertEquals(1, refs.size)
        assertEquals(43, refs[0].bookId)
    }

    @Test
    fun `parses RU Matthew abbreviation`() {
        val refs = parser.parse("Мф 5:3")
        assertEquals(1, refs.size)
        assertEquals(40, refs[0].bookId)
    }

    @Test
    fun `parses RU numbered book 1 Corinthians`() {
        val refs = parser.parse("1 Кор 3:16")
        assertEquals(1, refs.size)
        assertEquals(46, refs[0].bookId)
    }

    @Test
    fun `parses RU Revelations as Revelation`() {
        val refs = parser.parse("Откр 22:1")
        assertEquals(1, refs.size)
        assertEquals(66, refs[0].bookId)
        assertEquals(22, refs[0].chapter)
        assertEquals(1, refs[0].verseStart)
    }

    // ── Multiple references ──────────────────────────────────────────────────

    @Test
    fun `finds two references in one text`() {
        val refs = parser.parse("John 3:16 and Matt 5:3 are inspiring")
        assertEquals(2, refs.size)
        assertEquals(43, refs[0].bookId)
        assertEquals(40, refs[1].bookId)
    }

    @Test
    fun `finds RU and EN references in mixed text`() {
        val refs = parser.parse("Compare John 3:16 with Ин. 3:16")
        assertEquals(2, refs.size)
    }

    // ── Case sensitivity ─────────────────────────────────────────────────────

    @Test
    fun `uppercase EN book name is matched`() {
        val refs = parser.parse("JOHN 3:16")
        assertEquals(1, refs.size)
        assertEquals(43, refs[0].bookId)
    }

    @Test
    fun `lowercase EN book name is matched`() {
        val refs = parser.parse("john 3:16")
        assertEquals(1, refs.size)
        assertEquals(43, refs[0].bookId)
    }

    // ── Word boundary guard ──────────────────────────────────────────────────

    @Test
    fun `does not match book abbreviation embedded in a word`() {
        val refs = parser.parse("Johnson 3:16")
        assertTrue(refs.isEmpty())
    }

    // ── Display text and indices ─────────────────────────────────────────────

    @Test
    fun `displayText matches the matched substring`() {
        val refs = parser.parse("See John 3:16 for details")
        assertEquals("John 3:16", refs[0].displayText)
    }

    @Test
    fun `startIndex and endIndex span the matched text`() {
        val text = "See John 3:16 for details"
        val refs = parser.parse(text)
        assertEquals(4, refs[0].startIndex)
        assertEquals(13, refs[0].endIndex)
    }

    // ── BibleReference flags (existing) ──────────────────────────────────────

    @Test
    fun `isWholeChapter is true when no verse`() {
        val refs = parser.parse("John 3")
        assertTrue(refs[0].isWholeChapter)
    }

    @Test
    fun `isSingleVerse is true for single verse reference`() {
        val refs = parser.parse("John 3:16")
        assertTrue(refs[0].isSingleVerse)
    }

    @Test
    fun `isSingleVerse is false for verse range`() {
        val refs = parser.parse("John 3:16-18")
        assertFalse(refs[0].isSingleVerse)
    }

    @Test
    fun `isNonContiguous false for plain range`() {
        val refs = parser.parse("John 3:16-18")
        assertFalse(refs[0].isNonContiguous)
    }

    // ── verseDisplaySuffix (no commas) ────────────────────────────────────────

    @Test
    fun `verseDisplaySuffix is empty for whole chapter`() {
        val refs = parser.parse("John 3")
        assertEquals("", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `verseDisplaySuffix is verse number for single verse`() {
        val refs = parser.parse("John 3:16")
        assertEquals("16", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `verseDisplaySuffix uses en-dash for verse range`() {
        val refs = parser.parse("John 3:16-18")
        assertEquals("16–18", refs[0].verseDisplaySuffix)
    }

    // ── Comma-separated verses ────────────────────────────────────────────────

    @Test
    fun `parses RU comma-separated two verses`() {
        val refs = parser.parse("Быт 1:2,5")
        assertEquals(1, refs.size)
        with(refs[0]) {
            assertEquals(1, bookId)
            assertEquals(1, chapter)
            assertEquals(2, verseStart)
            assertNull(verseEnd)
            assertEquals(listOf(2, 5), verseList)
            assertTrue(isNonContiguous)
        }
    }

    @Test
    fun `parses RU range then comma verse`() {
        val refs = parser.parse("Быт 1:2-3,5")
        assertEquals(1, refs.size)
        with(refs[0]) {
            assertEquals(2, verseStart)
            assertEquals(3, verseEnd)
            assertEquals(listOf(2, 3, 5), verseList)
            assertTrue(isNonContiguous)
        }
    }

    @Test
    fun `parses RU three comma-separated verses`() {
        val refs = parser.parse("Быт 1:2,5,19")
        assertEquals(1, refs.size)
        assertEquals(listOf(2, 5, 19), refs[0].verseList)
    }

    @Test
    fun `parses EN comma-separated verses`() {
        val refs = parser.parse("Gen 1:1,3,5")
        assertEquals(1, refs.size)
        with(refs[0]) {
            assertEquals(1, bookId)
            assertEquals(listOf(1, 3, 5), verseList)
        }
    }

    @Test
    fun `parses EN range comma extra verse`() {
        val refs = parser.parse("John 3:16-18,20")
        assertEquals(1, refs.size)
        with(refs[0]) {
            assertEquals(43, bookId)
            assertEquals(3, chapter)
            assertEquals(16, verseStart)
            assertEquals(18, verseEnd)
            assertEquals(listOf(16, 17, 18, 20), verseList)
        }
    }

    @Test
    fun `parses comma tail that contains a range`() {
        val refs = parser.parse("Быт 1:1,3-5,7")
        assertEquals(1, refs.size)
        assertEquals(listOf(1, 3, 4, 5, 7), refs[0].verseList)
    }

    @Test
    fun `isSingleVerse false for comma ref`() {
        val refs = parser.parse("Быт 1:2,5")
        assertFalse(refs[0].isSingleVerse)
    }

    @Test
    fun `isWholeChapter false for comma ref`() {
        val refs = parser.parse("Быт 1:2,5")
        assertFalse(refs[0].isWholeChapter)
    }

    @Test
    fun `displayText includes comma tail`() {
        val refs = parser.parse("Смотри Быт 1:2,5 здесь")
        assertEquals("Быт 1:2,5", refs[0].displayText)
    }

    @Test
    fun `displayText includes range and comma tail`() {
        val refs = parser.parse("Быт 1:2-3,5")
        assertEquals("Быт 1:2-3,5", refs[0].displayText)
    }

    @Test
    fun `startIndex and endIndex span comma tail`() {
        val text = "Смотри Быт 1:2,5 здесь"
        val refs = parser.parse(text)
        val start = text.indexOf("Быт")
        assertEquals(start, refs[0].startIndex)
        assertEquals(start + "Быт 1:2,5".length, refs[0].endIndex)
    }

    @Test
    fun `verseDisplaySuffix for two non-contiguous verses`() {
        val refs = parser.parse("Быт 1:2,5")
        assertEquals("2,5", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `verseDisplaySuffix for range plus comma verse`() {
        val refs = parser.parse("Быт 1:2-3,5")
        assertEquals("2–3,5", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `verseDisplaySuffix for three non-contiguous verses`() {
        val refs = parser.parse("Быт 1:2,5,19")
        assertEquals("2,5,19", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `verseDisplaySuffix compresses consecutive verses from comma tail into range`() {
        val refs = parser.parse("Быт 1:1,3-5,7")
        assertEquals("1,3–5,7", refs[0].verseDisplaySuffix)
    }

    @Test
    fun `two separate references each with commas`() {
        val refs = parser.parse("Быт 1:2,5 и Ин 3:16,18")
        assertEquals(2, refs.size)
        assertEquals(listOf(2, 5), refs[0].verseList)
        assertEquals(listOf(16, 18), refs[1].verseList)
    }

    @Test
    fun `comma reference followed by plain reference`() {
        val refs = parser.parse("Быт 1:2,5 а также Ин 3:16")
        assertEquals(2, refs.size)
        assertEquals(listOf(2, 5), refs[0].verseList)
        assertTrue(refs[1].verseList.isEmpty())
    }

    // ── BibleReference.compactVerseString ────────────────────────────────────

    @Test
    fun `compactVerseString single verse`() {
        assertEquals("5", BibleReference.compactVerseString(listOf(5)))
    }

    @Test
    fun `compactVerseString two non-adjacent verses`() {
        assertEquals("2,5", BibleReference.compactVerseString(listOf(2, 5)))
    }

    @Test
    fun `compactVerseString consecutive verses become range`() {
        assertEquals("2–4", BibleReference.compactVerseString(listOf(2, 3, 4)))
    }

    @Test
    fun `compactVerseString range plus isolated`() {
        assertEquals("2–3,5", BibleReference.compactVerseString(listOf(2, 3, 5)))
    }

    @Test
    fun `compactVerseString isolated plus range plus isolated`() {
        assertEquals("1,3–5,7", BibleReference.compactVerseString(listOf(1, 3, 4, 5, 7)))
    }

    @Test
    fun `compactVerseString sorts input`() {
        assertEquals("2,5,19", BibleReference.compactVerseString(listOf(19, 2, 5)))
    }
}