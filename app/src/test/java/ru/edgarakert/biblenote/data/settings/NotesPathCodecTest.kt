package ru.edgarakert.biblenote.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesPathCodecTest {

    @Test
    fun `empty path encodes to an empty string`() {
        assertEquals("", NotesPathCodec.encode(emptyList()))
    }

    @Test
    fun `empty string decodes to an empty path`() {
        assertEquals(emptyList<NotesPathEntry>(), NotesPathCodec.decode(""))
    }

    @Test
    fun `a folder then a note round trips`() {
        val path = listOf(NotesPathEntry.Folder(12), NotesPathEntry.Note(34))
        assertEquals(path, NotesPathCodec.decode(NotesPathCodec.encode(path)))
    }

    @Test
    fun `a nested folder path round trips in order`() {
        val path = listOf(
            NotesPathEntry.Folder(1),
            NotesPathEntry.Folder(2),
            NotesPathEntry.Note(3)
        )
        assertEquals("f:1,f:2,n:3", NotesPathCodec.encode(path))
        assertEquals(path, NotesPathCodec.decode("f:1,f:2,n:3"))
    }

    @Test
    fun `malformed segments are skipped rather than throwing`() {
        assertEquals(
            listOf(NotesPathEntry.Folder(1), NotesPathEntry.Note(3)),
            NotesPathCodec.decode("f:1,garbage,x:2,n:3,f:")
        )
    }
}
