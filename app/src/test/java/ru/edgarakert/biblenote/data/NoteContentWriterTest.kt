package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note

class NoteContentWriterTest {

    @Test
    fun `appending to an empty note adds no leading blank line`() {
        val note = Note(id = 1, title = "", content = "")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Иоанна 3:16", result.content)
    }

    @Test
    fun `appending to a non-empty note separates with a blank line`() {
        val note = Note(id = 1, title = "", content = "Старая мысль")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Старая мысль\n\nИоанна 3:16", result.content)
    }

    @Test
    fun `appending bumps updatedAt`() {
        val note = Note(id = 1, title = "", content = "Текст", updatedAt = 1000L)
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `appending an empty snippet changes nothing`() {
        val note = Note(id = 1, title = "Заголовок", content = "Текст", updatedAt = 1000L)
        assertEquals(note, NoteContentWriter.append("", note, now = 5000L))
    }

    @Test
    fun `appending a blank snippet changes nothing`() {
        val note = Note(id = 1, title = "", content = "Текст", updatedAt = 1000L)
        assertEquals(note, NoteContentWriter.append("   \n ", note, now = 5000L))
    }

    @Test
    fun `appending to a whitespace-only note does not start it with a blank line`() {
        val note = Note(id = 1, title = "", content = "   ")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Иоанна 3:16", result.content)
    }

    @Test
    fun `appending does not accumulate trailing newlines`() {
        val note = Note(id = 1, title = "", content = "Старая мысль\n\n")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Старая мысль\n\nИоанна 3:16", result.content)
    }

    @Test
    fun `makeNote stores the title and the snippet`() {
        val note = NoteContentWriter.makeNote("Иоанна 3:16\n\n16 текст", "Иоанна 3:16", folderId = null, now = 7000L)
        assertEquals("Иоанна 3:16", note.title)
        assertEquals("Иоанна 3:16\n\n16 текст", note.content)
        assertEquals(null, note.folderId)
        assertTrue(note.id == 0L)
    }

    @Test
    fun `makeNote keeps the folder it was given`() {
        val note = NoteContentWriter.makeNote("текст", "Заголовок", folderId = 42L, now = 7000L)
        assertEquals(42L, note.folderId)
    }
}
