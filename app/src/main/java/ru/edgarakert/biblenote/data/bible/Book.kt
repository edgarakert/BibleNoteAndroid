package ru.edgarakert.biblenote.data.bible

data class Book(
    val id: Int,
    val nameRu: String,
    val nameEn: String,
    val abbreviation: String
) {
    fun name(translation: String) =
        if (translation == "kjv" /* || translation == "niv" */) nameEn else nameRu
}