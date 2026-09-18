package ru.edgarakert.biblenote.data.bible

/**
 * Превращает выделение стихов в читалке в обычный текст для заметки.
 * Ничего не знает о БД — принимает уже загруженные стихи, поэтому тестируется без SQLite.
 *
 * Ссылка пишется обычным текстом, а не спаном: редактор заново разбирает заметку
 * при каждом рендере и подсвечивает ссылки сам.
 */
object VerseSnippetBuilder {

    data class Verse(val number: Int, val text: String)

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

    /** Ссылка отдельной строкой, пустая строка, затем пронумерованные стихи. */
    fun build(bookName: String, chapter: Int, verses: List<Verse>): String {
        val ordered = verses.sortedBy { it.number }.distinctBy { it.number }
        val header = reference(bookName, chapter, ordered.map { it.number })
        if (ordered.isEmpty()) return header

        return "$header\n\n${versesBody(ordered)}"
    }
}
