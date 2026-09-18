package ru.edgarakert.biblenote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.edgarakert.biblenote.data.bible.translationDisplayNameRes
import ru.edgarakert.biblenote.data.bible.translationShortNameRes

@Composable
fun translationDisplayName(translation: String): String =
    translationDisplayNameRes(translation)?.let { stringResource(it) } ?: translation.uppercase()

@Composable
fun translationShortName(translation: String): String =
    translationShortNameRes(translation)?.let { stringResource(it) } ?: translation.uppercase()
