package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTextInsertionTest {

    @Test
    fun `a reference alone in the note inserts at the very end`() {
        val content = "Быт 2:14"
        assertEquals(content.length, NoteTextInsertion.lineEndAfter(content, content.length))
    }

    @Test
    fun `text after the reference on the same line is not split`() {
        // Вставка должна идти после «— важная мысль», а не разрезать фразу.
        val content = "Быт 2:14 — важная мысль\n\nследующий абзац"
        assertEquals(content.indexOf('\n'), NoteTextInsertion.lineEndAfter(content, 8))
    }

    @Test
    fun `a reference at the end of its line inserts right there`() {
        val content = "Быт 2:14\nвторая строка"
        assertEquals(8, NoteTextInsertion.lineEndAfter(content, 8))
    }

    @Test
    fun `only the reference's own line matters, not later ones`() {
        val content = "первая строка\nБыт 2:14 и мысль\nтретья строка"
        val refEnd = content.indexOf("Быт") + "Быт 2:14".length
        assertEquals(content.indexOf('\n', refEnd), NoteTextInsertion.lineEndAfter(content, refEnd))
    }

    @Test
    fun `an empty note inserts at zero`() {
        assertEquals(0, NoteTextInsertion.lineEndAfter("", 0))
    }

    @Test
    fun `an index past the end is clamped instead of throwing`() {
        val content = "Быт 2:14"
        assertEquals(content.length, NoteTextInsertion.lineEndAfter(content, 999))
    }

    @Test
    fun `a negative index is clamped instead of throwing`() {
        val content = "Быт 2:14\nвторая строка"
        assertEquals(8, NoteTextInsertion.lineEndAfter(content, -5))
    }
}
