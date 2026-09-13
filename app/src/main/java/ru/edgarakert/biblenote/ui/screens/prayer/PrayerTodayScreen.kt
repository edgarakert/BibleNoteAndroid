package ru.edgarakert.biblenote.ui.screens.prayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.components.prayer.PrayerCard
import ru.edgarakert.biblenote.ui.viewmodels.PrayerTodayItem
import ru.edgarakert.biblenote.ui.viewmodels.PrayerTodayViewModel

/**
 * Экран «Сегодня» молитвенного журнала.
 *
 * Навигационные колбэки нарочно nullable и по умолчанию `null`: маршруты `prayers/list`,
 * `prayers/answered`, `prayers/reminder`, `prayers/editor/{id}` и `prayers/detail/{id}`
 * появятся только в задачах 14.9–14.13, поэтому сейчас `NavGraph` не передаёт ни одного из
 * них — соответствующие кнопки верхней панели, кнопка пустого состояния и клик по карточке
 * просто не рисуются, пока колбэк не подключат.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTodayScreen(
    onOpenList: (() -> Unit)? = null,
    onOpenAnswered: (() -> Unit)? = null,
    onOpenReminder: (() -> Unit)? = null,
    onCreateRequest: (() -> Unit)? = null,
    onOpenDetail: ((Long) -> Unit)? = null,
    viewModel: PrayerTodayViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    // Пересчёт «помолились ли сегодня» и порядка групп при возврате экрана на передний план —
    // без этого они остались бы вчерашними, если открыть экран и оставить его через полночь.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(viewModel::onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PrayerTodayTopBar(
                onOpenList = onOpenList,
                onOpenAnswered = onOpenAnswered,
                onOpenReminder = onOpenReminder,
                onCreateRequest = onCreateRequest
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (items.isEmpty()) {
                PrayerTodayEmptyState(onCreateRequest = onCreateRequest)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.request.id }) { item ->
                        PrayerTodayCardRow(
                            item = item,
                            onPray = { viewModel.markPrayed(item.request) },
                            onOpenDetail = onOpenDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTodayCardRow(
    item: PrayerTodayItem,
    onPray: () -> Unit,
    onOpenDetail: ((Long) -> Unit)?
) {
    val clickModifier = if (onOpenDetail != null) {
        Modifier.clickable(
            onClickLabel = stringResource(R.string.verse_save_open),
            onClick = { onOpenDetail(item.request.id) }
        )
    } else {
        Modifier
    }
    PrayerCard(
        request = item.request,
        hasPrayedToday = item.hasPrayedToday,
        modifier = clickModifier,
        onPray = onPray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerTodayTopBar(
    onOpenList: (() -> Unit)?,
    onOpenAnswered: (() -> Unit)?,
    onOpenReminder: (() -> Unit)?,
    onCreateRequest: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.prayer_today_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (onOpenList != null) {
                IconButton(onClick = onOpenList) {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = stringResource(R.string.prayer_list_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onOpenAnswered != null) {
                IconButton(onClick = onOpenAnswered) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.prayer_answered_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            if (onCreateRequest != null) {
                IconButton(onClick = onCreateRequest) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.prayer_today_new_button),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onOpenReminder != null) {
                IconButton(onClick = onOpenReminder) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = stringResource(R.string.prayer_reminder_settings_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun PrayerTodayEmptyState(onCreateRequest: (() -> Unit)?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.VolunteerActivism,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.prayer_today_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            if (onCreateRequest != null) {
                Spacer(Modifier.size(20.dp))
                Button(onClick = onCreateRequest) {
                    Text(stringResource(R.string.prayer_today_new_button))
                }
            }
        }
    }
}
