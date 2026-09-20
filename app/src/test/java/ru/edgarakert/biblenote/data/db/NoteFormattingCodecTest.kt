package ru.edgarakert.biblenote.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFormattingCodecTest {

    @Test
    fun `an empty run list encodes to an empty string`() {
        assertEquals("", NoteFormattingCodec.encode(emptyList()))
    }

    @Test
    fun `an empty string decodes to an empty run list`() {
        assertEquals(emptyList<FormatRun>(), NoteFormattingCodec.decode(""))
    }

    @Test
    fun `null decodes to an empty run list`() {
        assertEquals(emptyList<FormatRun>(), NoteFormattingCodec.decode(null))
    }

    @Test
    fun `bold and italic round trip`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 0, 10),
            FormatRun(FormatType.ITALIC, 12, 20)
        )
        assertEquals("b:0-10;i:12-20", NoteFormattingCodec.encode(runs))
        assertEquals(runs, NoteFormattingCodec.decode("b:0-10;i:12-20"))
    }

    @Test
    fun `a size run carries its scale`() {
        val runs = listOf(FormatRun(FormatType.SIZE, 22, 30, scale = 1.25f))
        assertEquals("s1.25:22-30", NoteFormattingCodec.encode(runs))
        assertEquals(runs, NoteFormattingCodec.decode("s1.25:22-30"))
    }

    @Test
    fun `all three types round trip together`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 0, 4),
            FormatRun(FormatType.ITALIC, 5, 9),
            FormatRun(FormatType.SIZE, 10, 14, scale = 1.5f)
        )
        assertEquals(runs, NoteFormattingCodec.decode(NoteFormattingCodec.encode(runs)))
    }

    @Test
    fun `malformed segments are skipped rather than throwing`() {
        assertEquals(
            listOf(FormatRun(FormatType.BOLD, 0, 4), FormatRun(FormatType.ITALIC, 10, 12)),
            NoteFormattingCodec.decode("b:0-4;мусор;x:1-2;b:abc-4;s:5-6;i:10-12")
        )
    }

    @Test
    fun `runs with a non-positive length are dropped on encode`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 5, 5),
            FormatRun(FormatType.ITALIC, 9, 4),
            FormatRun(FormatType.BOLD, 0, 3)
        )
        assertEquals("b:0-3", NoteFormattingCodec.encode(runs))
    }

    @Test
    fun `decoded runs out of the text bounds are clamped away by the caller helper`() {
        val runs = NoteFormattingCodec.decode("b:0-4;i:100-120")
        assertEquals(listOf(FormatRun(FormatType.BOLD, 0, 4)), NoteFormattingCodec.clampTo(runs, textLength = 10))
    }

    @Test
    fun `a run that starts inside the text but ends past it is trimmed, not dropped`() {
        val runs = listOf(FormatRun(FormatType.BOLD, 5, 15))
        assertEquals(listOf(FormatRun(FormatType.BOLD, 5, 10)), NoteFormattingCodec.clampTo(runs, textLength = 10))
    }

    @Test
    fun `quote and caption runs survive a round trip`() {
        val runs = listOf(
            FormatRun(FormatType.QUOTE, 2, 40),
            FormatRun(FormatType.CAPTION, 41, 52)
        )
        assertEquals("q:2-40;c:41-52", NoteFormattingCodec.encode(runs))
        assertEquals(runs, NoteFormattingCodec.decode("q:2-40;c:41-52"))
    }
}
