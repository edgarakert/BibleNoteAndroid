package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.data.bible.HighlightColor

@Composable
fun BibleVerse(
    verseNumber: Int,
    text: String,
    highlightColor: HighlightColor?,
    verseScale: Float,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val textSizeSp = (17f * verseScale).sp
    val numSizeSp = ((17f * verseScale) - 2f).coerceAtLeast(10f).sp
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> highlightColor?.lightColor ?: Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$verseNumber",
            fontSize = numSizeSp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 12.dp)
        )
        Text(
            text = text,
            fontSize = textSizeSp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = textSizeSp * 1.15f,
            modifier = Modifier
                .weight(1f)
                .padding(end = 24.dp)
        )
    }
}
