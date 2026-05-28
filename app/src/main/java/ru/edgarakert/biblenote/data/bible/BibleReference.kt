package ru.edgarakert.biblenote.data.bible

data class BibleReference(
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int?,
    val verseEnd: Int?,
    val displayText: String,
    val startIndex: Int,
    val endIndex: Int
) {
    val isWholeChapter get() = verseStart == null
    val isSingleVerse  get() = verseStart != null && verseEnd == null
    val id get() = "$bookId/$chapter/${verseStart ?: 0}/${verseEnd ?: 0}"
}