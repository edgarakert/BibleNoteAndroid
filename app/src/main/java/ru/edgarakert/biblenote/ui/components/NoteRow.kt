package ru.edgarakert.biblenote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.WarmGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NoteRow(
    note: Note,
    modifier: Modifier = Modifier,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Amber else WarmGray.copy(alpha = 0.5f),
        animationSpec = tween(150),
        label = "select_tint"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(22.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 14.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = note.title.ifEmpty { stringResource(R.string.notes_untitled) },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                color = if (note.title.isEmpty()) WarmGray else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note.content.take(100),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val formattedDate = remember(note.updatedAt) { formatNoteDate(note.updatedAt) }
            Spacer(Modifier.height(6.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = Amber
            )
        }
    }
}

private fun formatNoteDate(epochMs: Long): String {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return if (epochMs >= todayStart) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
    } else {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMs))
    }
}
