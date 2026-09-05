package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note

class NoteVerseIndexServiceTest {

    private val parser = BibleReferenceParser()
    private val genesis1Verses = (1..31).toList()

    private fun note(id: Long, content: String) = Note(id = id, title = "", content = content)

    private fun index(notes: List<Note>) =
        NoteVerseIndexService.index(
            notes = notes,
            bookId = 1,
            chapter = 1,
            verseNumbers = genesis1Verses,
            parser = parser
        )

    @Test
    fun `a single-verse reference indexes only that verse`() {
        val result = index(listOf(note(1, "Мысль о Быт 1:1 и всём начале")))
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertNull(result[2])
    }

    @Test
    fun `a range reference indexes every verse in the range`() {
        val result = index(listOf(note(1, "Быт 1:1-3")))
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertEquals(listOf(1L), result[2]?.map { it.id })
        assertEquals(listOf(1L), result[3]?.map { it.id })
        assertNull(result[4])
    }

    @Test
    fun `a whole-chapter reference indexes every verse of the chapter`() {
        val result = index(listOf(note(1, "Бытие 1 целиком")))
        assertEquals(31, result.size)
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertEquals(listOf(1L), result[31]?.map { it.id })
    }

    @Test
    fun `a comma reference indexes exactly the listed verses`() {
        val result = index(listOf(note(1, "Быт 1:2,5")))
        assertEquals(listOf(1L), result[2]?.map { it.id })
        assertEquals(listOf(1L), result[5]?.map { it.id })
        assertNull(result[3])
    }

    @Test
    fun `two notes on the same verse both appear`() {
        val result = index(listOf(note(1, "Быт 1:1"), note(2, "Ещё раз Быт 1:1")))
        assertEquals(listOf(1L, 2L), result[1]?.map { it.id })
    }

    @Test
    fun `a note referencing the same verse twice is counted once`() {
        val result = index(listOf(note(1, "Быт 1:1 и снова Быт 1:1")))
        assertEquals(1, result[1]?.size)
    }

    @Test
    fun `a reference to another chapter yields an empty index`() {
        assertTrue(index(listOf(note(1, "Быт 2:1"))).isEmpty())
    }

    @Test
    fun `a note without references yields an empty index`() {
        assertTrue(index(listOf(note(1, "Просто текст без ссылок"))).isEmpty())
    }
}
