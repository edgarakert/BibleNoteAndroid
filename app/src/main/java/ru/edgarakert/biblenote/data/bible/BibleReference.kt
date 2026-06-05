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

    companion object {
        fun compactVerseString(verses: List<Int>): String {
            val sorted = verses.sorted()
            val parts = mutableListOf<String>()
            var i = 0
            while (i < sorted.size) {
                val start = sorted[i]
                var end = start
                while (i + 1 < sorted.size && sorted[i + 1] == sorted[i] + 1) {
                    i++; end = sorted[i]
                }
                parts.add(if (end > start) "$start–$end" else "$start")
                i++
            }
            return parts.joinToString(",")
        }
    }
}