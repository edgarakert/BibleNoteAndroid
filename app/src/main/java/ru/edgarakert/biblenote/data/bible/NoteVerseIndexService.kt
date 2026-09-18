package ru.edgarakert.biblenote.data.bible

import ru.edgarakert.biblenote.data.db.Note

/**
 * Сопоставляет номера стихов главы с заметками, которые на них ссылаются,
 * прогоняя по тексту заметок тот же парсер, что подсвечивает ссылки в редакторе.
 *
 * Чистая функция без обращений к БД — вызывающий сам грузит заметки и номера стихов главы.
 */
object NoteVerseIndexService {

    fun index(
        notes: List<Note>,
        bookId: Int,
        chapter: Int,
        verseNumbers: List<Int>,
        parser: BibleReferenceParser,
    ): Map<Int, List<Note>> {
        val result = mutableMapOf<Int, MutableList<Note>>()

        for (note in notes) {
            val refs = parser.parse(note.content)
                .filter { it.bookId == bookId && it.chapter == chapter }
            if (refs.isEmpty()) continue

            // Множество: заметка попадает под стих один раз, сколько бы ссылок на него ни было.
            val matched = mutableSetOf<Int>()
            for (ref in refs) {
                if (ref.isWholeChapter) {
                    // Ссылка на главу целиком относится ко всем её стихам.
                    matched += verseNumbers
                } else {
                    matched += ref.coveredVerses
                }
            }

            for (verse in matched) {
                result.getOrPut(verse) { mutableListOf() } += note
            }
        }

        return result
    }
}
