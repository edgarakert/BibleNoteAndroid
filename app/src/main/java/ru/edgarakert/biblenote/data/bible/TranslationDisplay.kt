package ru.edgarakert.biblenote.data.bible

fun translationDisplayName(translation: String) = when (translation) {
    "synodal" -> "Синодальный"
    "nrt" -> "НРП"
    "kjv" -> "KJV"
    // "niv" -> "NIV"
    else -> translation.uppercase()
}

fun translationShortName(translation: String) = when (translation) {
    "synodal" -> "Синод."
    "nrt" -> "НРП"
    "kjv" -> "KJV"
    // "niv" -> "NIV"
    else -> translation.uppercase()
}
