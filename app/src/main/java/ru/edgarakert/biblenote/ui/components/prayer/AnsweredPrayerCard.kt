package ru.edgarakert.biblenote.ui.components.prayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.prayer.AnsweredPrayers
import ru.edgarakert.biblenote.data.prayer.PrayerDuration
import ru.edgarakert.biblenote.data.prayer.PrayerDurationUnit

/** Карточка отвеченной молитвы: заголовок, сколько молились, текст ответа. */
@Composable
fun AnsweredPrayerCard(
    request: PrayerRequest,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val duration = AnsweredPrayers.duration(request.createdAt, request.answeredAt)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.verse_save_open),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = request.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (duration != null) {
                Text(
                    text = "${stringResource(R.string.prayer_answered_duration_prefix)} ${durationText(duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            request.answerText?.let { answer ->
                Text(
                    text = answer,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun durationText(duration: PrayerDuration): String {
    val plural = when (duration.unit) {
        PrayerDurationUnit.YEARS -> R.plurals.prayer_duration_years
        PrayerDurationUnit.MONTHS -> R.plurals.prayer_duration_months
        PrayerDurationUnit.WEEKS -> R.plurals.prayer_duration_weeks
        PrayerDurationUnit.DAYS -> R.plurals.prayer_duration_days
    }
    return pluralStringResource(plural, duration.amount, duration.amount)
}
