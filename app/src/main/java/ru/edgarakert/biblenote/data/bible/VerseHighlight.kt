package ru.edgarakert.biblenote.data.bible

data class VerseHighlight(
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val colorName: String,
    val createdAt: Long
)