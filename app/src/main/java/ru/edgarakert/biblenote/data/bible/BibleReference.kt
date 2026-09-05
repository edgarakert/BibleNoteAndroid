package ru.edgarakert.biblenote.data.bible

data class BibleReference(
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int?,
    val verseEnd: Int?,
    val verseList: List<Int> = emptyList(),
    val displayText: String,
    val startIndex: Int,
    val endIndex: Int
) {
    val isWholeChapter get() = verseStart == null
    val isSingleVerse  get() = verseStart != null && verseEnd == null && verseList.isEmpty()
    val isNonContiguous get() = verseList.isNotEmpty()

    val verseDisplaySuffix: String get() = when {
        verseList.isNotEmpty() -> compactVerseString(verseList)
        verseStart == null -> ""
        verseEnd != null -> "$verseStart–$verseEnd"
        else -> "$verseStart"
    }

    val id get() = when {
        verseList.isNotEmpty() -> "$bookId/$chapter/${verseList.joinToString(",")}"
        else -> "$bookId/$chapter/${verseStart ?: 0}/${verseEnd ?: 0}"
    }

    /**
     * Номера стихов, которые покрывает ссылка.
     * Ссылка на всю главу возвращает пустой список (номера стихов главы ей неизвестны) —
     * потребитель, которому нужны все стихи главы, подставляет их сам.
     * verseList авторитетен: для "Быт 2:14-15,20" парсер заполняет verseStart/verseEnd
     * только первым сегментом, а полный набор кладёт в verseList.
     */
    val coveredVerses: List<Int>
        get() = when {
            verseList.isNotEmpty() -> verseList.sorted()
            verseStart == null -> emptyList()
            else -> (verseStart..(verseEnd ?: verseStart)).toList()
        }

    companion object {
        /**
         * Собирает список номеров стихов в компактную запись: "14-16,20".
         *
         * rangeSeparator по умолчанию — ASCII-дефис (U+002D): это единственный разделитель,
         * который принимает BibleReferenceParser, поэтому всё, что пишется в текст заметки,
         * должно использовать его. En dash (U+2013) допустим только в заголовках на экране.
         */
        fun formatVerseSpec(verses: List<Int>, rangeSeparator: String = "-"): String {
            val sorted = verses.toSortedSet().toList()
            if (sorted.isEmpty()) return ""

            val parts = mutableListOf<String>()
            var runStart = sorted.first()
            var runEnd = runStart

            for (verse in sorted.drop(1)) {
                if (verse == runEnd + 1) {
                    runEnd = verse
                } else {
                    parts += formatRun(runStart, runEnd, rangeSeparator)
                    runStart = verse
                    runEnd = verse
                }
            }
            parts += formatRun(runStart, runEnd, rangeSeparator)
            return parts.joinToString(",")
        }

        private fun formatRun(start: Int, end: Int, separator: String): String =
            if (start == end) "$start" else "$start$separator$end"

        /** Запись для показа на экране — с en dash. */
        fun compactVerseString(verses: List<Int>): String =
            formatVerseSpec(verses, rangeSeparator = "–")

        /**
         * Пересобирает текст ссылки под новый набор стихов, сохраняя сокращение книги
         * ровно так, как его набрал пользователь ("быт.3:2" → "быт.3:7").
         * Пустой набор схлопывает ссылку до главы целиком.
         *
         * Ожидает displayText в грамматике BibleReferenceParser ("книга глава[:стихи]"):
         * первое двоеточие считается разделителем главы и стихов, всё после него отбрасывается.
         */
        fun replacementText(displayText: String, verses: List<Int>): String {
            val head = displayText.substringBefore(':')
            val spec = formatVerseSpec(verses)
            return if (spec.isEmpty()) head else "$head:$spec"
        }
    }
}
