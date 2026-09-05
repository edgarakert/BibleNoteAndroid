package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    noteCount: Int = 0,
    onNoteBadgeClick: (() -> Unit)? = null,
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
                .padding(end = if (noteCount > 0) 0.dp else 24.dp)
        )
        if (noteCount > 0 && onNoteBadgeClick != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                    .clickable(onClick = onNoteBadgeClick)
                    .padding(horizontal = 4.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((10f * verseScale).dp)
                )
                Text(
                    text = "$noteCount",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (11f * verseScale).sp
                )
            }
        }
    }
}
