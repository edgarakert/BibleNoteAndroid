package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec

/**
 * append() только дописывает текст в конец, поэтому существующие диапазоны
 * форматирования остаются валидными и должны пережить копирование Note без изменений.
 */
class NoteContentWriterFormattingTest {

    @Test
    fun `appending preserves existing formatting unchanged`() {
        val note = Note(id = 1, title = "", content = "Старая мысль", formatting = "b:0-6")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("b:0-6", result.formatting)
    }

    @Test
    fun `appending to a legacy plain note leaves formatting null`() {
        val note = Note(id = 1, title = "", content = "Старая мысль", formatting = null)
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertNull(result.formatting)
    }

    @Test
    fun `makeNote never invents formatting`() {
        val note = NoteContentWriter.makeNote("Иоанна 3:16\n\n16 текст", "Иоанна 3:16", folderId = null, now = 7000L)
        assertNull(note.formatting)
    }

    @Test
    fun `appended text keeps existing formatting ranges within bounds`() {
        val note = Note(id = 1, title = "", content = "Старая мысль", formatting = "b:0-6")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)

        val decoded = NoteFormattingCodec.decode(result.formatting)
        assertEquals(decoded, NoteFormattingCodec.clampTo(decoded, result.content.length))
    }
}
