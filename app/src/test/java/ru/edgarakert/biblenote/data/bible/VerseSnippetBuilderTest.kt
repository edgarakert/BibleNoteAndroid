package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
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
}
