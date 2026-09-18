package ru.edgarakert.biblenote.ui.components.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.PrayerCategory

/**
 * Единственное место, где [PrayerCategory] превращается в локализованную строку — переиспользуй
 * это, а не заводи собственный `when` в редакторе/деталях/списке просьб (задачи 14.9–14.13).
 */
@Composable
fun prayerCategoryLabel(category: PrayerCategory): String = stringResource(
    when (category) {
        PrayerCategory.FAMILY -> R.string.prayer_category_family
        PrayerCategory.CHURCH -> R.string.prayer_category_church
        PrayerCategory.HEALTH -> R.string.prayer_category_health
        PrayerCategory.WORK -> R.string.prayer_category_work
        PrayerCategory.GRATITUDE -> R.string.prayer_category_gratitude
        PrayerCategory.PERSONAL -> R.string.prayer_category_personal
        PrayerCategory.WORLD -> R.string.prayer_category_world
        PrayerCategory.OTHER -> R.string.prayer_category_other
    }
)

@Composable
fun PrayerCategoryBadge(
    category: PrayerCategory,
    modifier: Modifier = Modifier
) {
    Text(
        text = prayerCategoryLabel(category),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
