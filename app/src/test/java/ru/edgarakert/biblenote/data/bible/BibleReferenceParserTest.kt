package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
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
    fun `parses RU numbered book 1 Кор`() {
        val refs = parser.parse("1 Кор 3:16")
        assertEquals(1, refs.size)
        assertEquals(46, refs[0].bookId)
    }

    @Test
    fun `parses RU Откр as Revelation`() {
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

    // ── BibleReference helpers ────────────────────────────────────────────────

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
}
