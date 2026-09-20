package ru.edgarakert.biblenote.data.bible

import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType

/**
 * Превращает выделение стихов в читалке в обычный текст для заметки.
 * Ничего не знает о БД — принимает уже загруженные стихи, поэтому тестируется без SQLite.
 *
 * Ссылка пишется обычным текстом, а не спаном: редактор заново разбирает заметку
 * при каждом рендере и подсвечивает ссылки сам.
 */
object VerseSnippetBuilder {

    data class Verse(val number: Int, val text: String)

    /** Текст с диапазонами форматирования; смещения — от начала [text]. */
    data class StyledSnippet(val text: String, val formatting: List<FormatRun>)

    /** "Иоанна 3:16-17,20". Пустой выбор схлопывается в ссылку на главу. */
    fun reference(bookName: String, chapter: Int, verseNumbers: List<Int>): String {
        // ASCII-дефис: парсер не понимает en dash.
        val spec = BibleReference.formatVerseSpec(verseNumbers)
        return if (spec.isEmpty()) "$bookName $chapter" else "$bookName $chapter:$spec"
    }

    /**
     * Только пронумерованные стихи, без строки ссылки — для вставки под ссылку, которая
     * в тексте заметки уже есть (шторка стиха в редакторе). Дублировать её там незачем.
     */
    fun versesBody(verses: List<Verse>): String = verses
        .sortedBy { it.number }
        .distinctBy { it.number }
        .joinToString("\n") { "${it.number} ${it.text.trim()}" }

    /**
     * Цитата для вставки в заметку, как в iOS: пронумерованные стихи курсивом приглушённым
     * цветом, под ними — [reference] мелким шрифтом. Цвет ссылки не задаётся: редактор сам
     * подсветит её янтарём и сделает кликабельной, как любую ссылку в тексте.
     */
    fun quote(verses: List<Verse>, reference: String): StyledSnippet {
        val body = versesBody(verses)
        if (body.isEmpty()) return StyledSnippet("", emptyList())

        val captionStart = body.length + 1
        val text = "$body\n$reference"
        return StyledSnippet(
            text = text,
            formatting = listOf(
                FormatRun(FormatType.ITALIC, 0, body.length),
                FormatRun(FormatType.QUOTE, 0, body.length),
                FormatRun(FormatType.CAPTION, captionStart, text.length),
            ).filter { it.end > it.start }
        )
    }

    /** Ссылка отдельной строкой, пустая строка, затем пронумерованные стихи. */
    fun build(bookName: String, chapter: Int, verses: List<Verse>): String {
        val ordered = verses.sortedBy { it.number }.distinctBy { it.number }
        val header = reference(bookName, chapter, ordered.map { it.number })
        if (ordered.isEmpty()) return header

        return "$header\n\n${versesBody(ordered)}"
    }
}
