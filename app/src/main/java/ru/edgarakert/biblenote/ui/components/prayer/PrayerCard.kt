package ru.edgarakert.biblenote.ui.components.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.PrayerRequest

/**
 * Кнопка «Помолился» рисуется, только когда передан [onPray] — на экране «Все просьбы»
 * (задача 14.9) обработчик не передаётся, и кнопки там нет.
 */
@Composable
fun PrayerCard(
    request: PrayerRequest,
    hasPrayedToday: Boolean,
    modifier: Modifier = Modifier,
    onPray: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                PrayerCategoryBadge(category = request.category)
            }

            if (request.prayedDaysCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.prayer_days,
                        request.prayedDaysCount,
                        request.prayedDaysCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onPray != null) {
                Button(
                    onClick = onPray,
                    enabled = !hasPrayedToday,
                    modifier = Modifier
                        .align(Alignment.End)
                        .alpha(if (hasPrayedToday) 0.4f else 1f)
                ) {
                    Text(stringResource(R.string.prayer_card_pray_button))
                }
            }
        }
    }
}
