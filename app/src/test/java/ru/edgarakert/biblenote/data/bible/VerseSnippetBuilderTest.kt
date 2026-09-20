package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType
import org.junit.Test

class VerseSnippetBuilderTest {

    private fun verse(n: Int, text: String) = VerseSnippetBuilder.Verse(n, text)

    @Test
    fun `reference of a single verse`() {
        assertEquals("Иоанна 3:16", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16)))
    }

    @Test
    fun `reference of a consecutive range`() {
        assertEquals("Иоанна 3:16-17", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16, 17)))
    }

    @Test
    fun `reference of a disjoint selection`() {
        assertEquals("Иоанна 3:16-17,20", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16, 17, 20)))
    }

    @Test
    fun `reference of an empty selection degrades to the chapter`() {
        assertEquals("Иоанна 3", VerseSnippetBuilder.reference("Иоанна", 3, emptyList()))
    }

    @Test
    fun `build puts the reference, a blank line, then numbered verses`() {
        val result = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(16, "Ибо так возлюбил Бог мир"), verse(17, "Ибо не послал Бог Сына"))
        )
        assertEquals(
            "Иоанна 3:16-17\n\n16 Ибо так возлюбил Бог мир\n17 Ибо не послал Бог Сына",
            result
        )
    }

    @Test
    fun `build orders verses by number and the header follows`() {
        val result = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(20, "Всякий, делающий злое"), verse(16, "Ибо так возлюбил Бог мир"))
        )
        assertEquals(
            "Иоанна 3:16,20\n\n16 Ибо так возлюбил Бог мир\n20 Всякий, делающий злое",
            result
        )
    }

    @Test
    fun `build trims surrounding whitespace of verse text`() {
        val result = VerseSnippetBuilder.build("Иоанна", 3, listOf(verse(16, "  Ибо так возлюбил Бог мир\n")))
        assertEquals("Иоанна 3:16\n\n16 Ибо так возлюбил Бог мир", result)
    }

    @Test
    fun `build of an empty verse list is just the chapter reference`() {
        assertEquals("Иоанна 3", VerseSnippetBuilder.build("Иоанна", 3, emptyList()))
    }

    // ── versesBody: текст стихов без строки ссылки ──────────────

    @Test
    fun `versesBody numbers each verse without a reference line`() {
        assertEquals(
            "16 Ибо так возлюбил Бог мир\n17 Ибо не послал Бог Сына",
            VerseSnippetBuilder.versesBody(
                listOf(verse(16, "Ибо так возлюбил Бог мир"), verse(17, "Ибо не послал Бог Сына"))
            )
        )
    }

    @Test
    fun `versesBody orders verses by number`() {
        assertEquals(
            "16 первый\n20 второй",
            VerseSnippetBuilder.versesBody(listOf(verse(20, "второй"), verse(16, "первый")))
        )
    }

    @Test
    fun `versesBody trims surrounding whitespace of verse text`() {
        assertEquals("16 текст", VerseSnippetBuilder.versesBody(listOf(verse(16, "  текст\n"))))
    }

    @Test
    fun `versesBody of an empty list is an empty string`() {
        assertEquals("", VerseSnippetBuilder.versesBody(emptyList()))
    }

    @Test
    fun `build is the reference, a blank line and versesBody`() {
        val verses = listOf(verse(16, "первый"), verse(17, "второй"))
        assertEquals(
            VerseSnippetBuilder.reference("Иоанна", 3, listOf(16, 17)) + "\n\n" +
                VerseSnippetBuilder.versesBody(verses),
            VerseSnippetBuilder.build("Иоанна", 3, verses)
        )
    }

    @Test
    fun `the built snippet re-parses into a tappable reference`() {
        val snippet = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(16, "текст"), verse(17, "текст"), verse(20, "текст"))
        )
        val parsed = BibleReferenceParser().parse(snippet)
        assertEquals(43, parsed[0].bookId)
        assertEquals(3, parsed[0].chapter)
        assertEquals(listOf(16, 17, 20), parsed[0].coveredVerses)
    }

    @Test
    fun `quote puts italic muted verses above a small reference caption`() {
        val result = VerseSnippetBuilder.quote(
            listOf(verse(17, "Ибо не послал Бог"), verse(16, "Ибо так возлюбил Бог мир")),
            "Иоанна 3:16-17"
        )

        val body = "16 Ибо так возлюбил Бог мир\n17 Ибо не послал Бог"
        assertEquals("$body\nИоанна 3:16-17", result.text)
        assertEquals(
            listOf(
                FormatRun(FormatType.ITALIC, 0, body.length),
                FormatRun(FormatType.QUOTE, 0, body.length),
                FormatRun(FormatType.CAPTION, body.length + 1, result.text.length),
            ),
            result.formatting
        )
    }

    @Test
    fun `quote caption stays a tappable reference`() {
        val result = VerseSnippetBuilder.quote(listOf(verse(16, "Ибо так")), "Иоанна 3:16")
        val caption = result.formatting.single { it.type == FormatType.CAPTION }

        val parsed = BibleReferenceParser().parse(result.text)
        assertEquals(1, parsed.size)
        assertEquals(caption.start, parsed[0].startIndex)
        assertEquals(caption.end, parsed[0].endIndex)
    }

    @Test
    fun `quote of nothing is empty`() {
        assertEquals(VerseSnippetBuilder.StyledSnippet("", emptyList()), VerseSnippetBuilder.quote(emptyList(), "Иоанна 3"))
    }
}
