package ru.edgarakert.biblenote.data.bible

data class Verse(
    val id: Int?,
    val translation: String,
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val text: String
)

data class VerseRow(
    val verseNumber: Int,
    val text: String,
    val colorName: String?
)
