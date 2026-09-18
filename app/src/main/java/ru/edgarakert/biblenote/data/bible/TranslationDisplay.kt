package ru.edgarakert.biblenote.data.bible

import androidx.annotation.StringRes
import ru.edgarakert.biblenote.R

// Names come from string resources so they follow the device locale;
// unknown ids fall back to the raw id in upper case.
@StringRes
fun translationDisplayNameRes(translation: String): Int? = when (translation) {
    "synodal" -> R.string.translation_synodal_name
    "nrt" -> R.string.translation_nrt_name
    "kjv" -> R.string.translation_kjv_name
    "niv" -> R.string.translation_niv_name
    else -> null
}

@StringRes
fun translationShortNameRes(translation: String): Int? = when (translation) {
    "synodal" -> R.string.translation_synodal_short
    "nrt" -> R.string.translation_nrt_name
    "kjv" -> R.string.translation_kjv_name
    "niv" -> R.string.translation_niv_name
    else -> null
}
