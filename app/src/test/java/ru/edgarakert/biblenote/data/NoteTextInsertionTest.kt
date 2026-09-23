package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.edgarakert.biblenote.data.bible.VerseSnippetBuilder
import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType

class NoteTextInsertionTest {

    private fun apply(content: String, r: NoteTextInsertion.Replacement) =
        content.substring(0, r.start) + r.text + content.substring(r.end)

    private fun refRange(content: String, ref: String): Pair<Int, Int> {
        val s = content.indexOf(ref)
        return s to s + ref.length
    }

    @Test
    fun `a reference alone in the note becomes the quote followed by an empty line`() {
        val content = "Быт 1:1"
        val r = NoteTextInsertion.replaceOnOwnLine(content, 0, content.length, "1 В начале")
        assertEquals("1 В начале\n", apply(content, r))
        assertEquals(0, r.textOffset)
    }

    @Test
    fun `a reference after text on the same line moves to a new line`() {
        val content = "Смотри Быт 1:1"
        val (s, e) = refRange(content, "Быт 1:1")
        val r = NoteTextInsertion.replaceOnOwnLine(content, s, e, "1 В начале")
        assertEquals("Смотри\n1 В начале\n", apply(content, r))
        assertEquals(1, r.textOffset)
    }

    @Test
    fun `text after the reference continues on the next line`() {
        val content = "Смотри Быт 1:1 и думай"
        val (s, e) = refRange(content, "Быт 1:1")
        val r = NoteTextInsertion.replaceOnOwnLine(content, s, e, "1 В начале")
        assertEquals("Смотри\n1 В начале\nи думай", apply(content, r))
    }

    @Test
    fun `a reference on its own line adds no extra line breaks`() {
        val content = "первая\nБыт 1:1\nтретья"
        val (s, e) = refRange(content, "Быт 1:1")
        val r = NoteTextInsertion.replaceOnOwnLine(content, s, e, "1 В начале")
        assertEquals("первая\n1 В начале\nтретья", apply(content, r))
        assertEquals(0, r.textOffset)
    }

    @Test
    fun `a reference at the start of a line with text after it`() {
        val content = "Быт 1:1 — мысль"
        val (s, e) = refRange(content, "Быт 1:1")
        val r = NoteTextInsertion.replaceOnOwnLine(content, s, e, "1 В начале")
        assertEquals("1 В начале\n— мысль", apply(content, r))
    }

    // ── removeQuoteAbove ────────────────────────────────────────────────────────

    /** Вставляет цитату на место ссылки так же, как экран редактора; возвращает текст и стили. */
    private fun insertQuote(content: String, ref: String, caption: String): Pair<String, List<FormatRun>> {
        val quote = VerseSnippetBuilder.quote(listOf(VerseSnippetBuilder.Verse(1, "В начале")), caption)
        val (s, e) = refRange(content, ref)
        val r = NoteTextInsertion.replaceOnOwnLine(content, s, e, quote.text)
        val runs = quote.formatting.map {
            it.copy(start = it.start + r.start + r.textOffset, end = it.end + r.start + r.textOffset)
        }
        return apply(content, r) to runs
    }

    @Test
    fun `removing an inserted quote leaves the bare reference`() {
        val (text, runs) = insertQuote("Смотри Быт 1:1 и думай", "Быт 1:1", "Бытие 1:1")
        assertEquals("Смотри\n1 В начале\nБытие 1:1\nи думай", text)

        val (s, e) = refRange(text, "Бытие 1:1")
        val r = NoteTextInsertion.removeQuoteAbove(text, runs, s, e)!!
        assertEquals("Смотри\nБытие 1:1\nи думай", apply(text, r))
    }

    @Test
    fun `a quote split by typing inside it is removed whole`() {
        val (text, runs) = insertQuote("Быт 1:1", "Быт 1:1", "Бытие 1:1")
        val quote = runs.single { it.type == FormatType.QUOTE }
        val split = runs - quote + listOf(
            quote.copy(end = quote.start + 3),
            quote.copy(start = quote.start + 3),
        )

        val (s, e) = refRange(text, "Бытие 1:1")
        val r = NoteTextInsertion.removeQuoteAbove(text, split, s, e)!!
        assertEquals("Бытие 1:1\n", apply(text, r))
    }

    @Test
    fun `an ordinary reference has nothing to remove`() {
        val content = "первая\nБыт 1:1"
        val (s, e) = refRange(content, "Быт 1:1")
        assertNull(NoteTextInsertion.removeQuoteAbove(content, emptyList(), s, e))
    }

    @Test
    fun `a caption without a quote right above it has nothing to remove`() {
        val content = "обычный текст\nБытие 1:1"
        val (s, e) = refRange(content, "Бытие 1:1")
        val runs = listOf(FormatRun(FormatType.CAPTION, s, e))
        assertNull(NoteTextInsertion.removeQuoteAbove(content, runs, s, e))
    }

    @Test
    fun `out of range bounds are clamped instead of throwing`() {
        val content = "Быт 1:1"
        val r = NoteTextInsertion.replaceOnOwnLine(content, -5, 999, "1 В начале")
        assertEquals("1 В начале\n", apply(content, r))
    }
}
