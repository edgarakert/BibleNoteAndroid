package ru.edgarakert.biblenote.ui.screens.prayer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.PrayerCategory
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import ru.edgarakert.biblenote.data.prayer.PrayerListFilter
import ru.edgarakert.biblenote.data.prayer.PrayerListSection
import ru.edgarakert.biblenote.ui.components.prayer.PrayerCard
import ru.edgarakert.biblenote.ui.components.prayer.prayerCategoryLabel
import ru.edgarakert.biblenote.ui.viewmodels.PrayerListViewModel

/**
 * Экран «Все просьбы» (задача 14.9): все просьбы, сгруппированные по категориям, с фильтром
 * по статусу и поиском. Группировка/фильтрация/поиск — чистая функция `PrayerListGrouping`
 * (поправка 1 к плану), здесь только UI поверх готовых секций из [PrayerListViewModel].
 *
 * [onOpenDetail] nullable и по умолчанию `null` (поправка 3 к плану): маршрут
 * `prayers/detail/{id}` появится в задаче 14.10. Пока колбэк не передан, строка не кликабельна
 * по тапу — долгое нажатие (меню) и свайп (удаление) работают независимо от него.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerListScreen(
    onBack: () -> Unit,
    onOpenDetail: ((Long) -> Unit)? = null,
    viewModel: PrayerListViewModel = koinViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prayer_list_title),
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            PrayerListFilterRow(
                filter = filter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.size(8.dp))

            if (sections.isEmpty()) {
                PrayerListEmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    sections.forEach { section ->
                        prayerListSection(
                            section = section,
                            onOpenDetail = onOpenDetail,
                            onEntrust = viewModel::entrust,
                            onDelete = viewModel::delete
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerListFilterRow(
    filter: PrayerListFilter,
    onFilterSelected: (PrayerListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(
        PrayerListFilter.ACTIVE to stringResource(R.string.prayer_list_filter_active),
        PrayerListFilter.ANSWERED to stringResource(R.string.prayer_list_filter_answered),
        PrayerListFilter.ALL to stringResource(R.string.prayer_list_filter_all),
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        filters.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = filter == value,
                onClick = { onFilterSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(text = label, fontSize = 13.sp)
            }
        }
    }
}

private fun LazyListScope.prayerListSection(
    section: PrayerListSection,
    onOpenDetail: ((Long) -> Unit)?,
    onEntrust: (PrayerRequest) -> Unit,
    onDelete: (PrayerRequest) -> Unit,
) {
    item(key = "header_${section.category}") {
        PrayerCategorySectionHeader(category = section.category)
    }
    items(section.requests, key = { "prayer_${it.id}" }) { request ->
        PrayerListRow(
            request = request,
            onOpenDetail = onOpenDetail,
            onEntrust = onEntrust,
            onDelete = onDelete
        )
    }
}

@Composable
private fun PrayerCategorySectionHeader(category: PrayerCategory) {
    Text(
        text = prayerCategoryLabel(category),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Одна строка списка. `PrayerCard` рисуется без `onPray` — кнопки «Помолился» здесь нет по
 * контракту `PrayerCard` (см. её KDoc). Свайп влево и пункт меню «Удалить просьбу» ведут к ОДНОМУ
 * и тому же диалогу подтверждения (поправка 2 к плану: одно действие на направление свайпа, как
 * в `NoteListSection` из задачи 12.4); «Доверить Богу» доступно только для активных просьб и не
 * требует подтверждения. `combinedClickable`, а не `pointerInput { detectTapGestures }` — та же
 * причина, что в `NoteListSection`: без него долгое нажатие недостижимо для TalkBack.
 *
 * Если пользователь отменяет удаление — `dismissState.reset()` возвращает строку на место, а не
 * оставляет её сдвинутой.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PrayerListRow(
    request: PrayerRequest,
    onOpenDetail: ((Long) -> Unit)?,
    onEntrust: (PrayerRequest) -> Unit,
    onDelete: (PrayerRequest) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            showDeleteConfirm = true
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "prayer_swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onOpenDetail?.invoke(request.id) },
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = stringResource(R.string.prayer_actions)
                )
        ) {
            PrayerCard(request = request, hasPrayedToday = false)

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (request.status == PrayerStatus.ACTIVE) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.prayer_list_entrust_button)) },
                        onClick = { menuOpen = false; onEntrust(request) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.prayer_delete)) },
                    onClick = { menuOpen = false; showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                coroutineScope.launch { dismissState.reset() }
            },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.prayer_delete), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    stringResource(R.string.note_delete_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(request)
                    }
                ) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch { dismissState.reset() }
                    }
                ) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/** Один и тот же текст для любого фильтра — так велит поправленный план задачи 14.9. */
@Composable
private fun PrayerListEmptyState() {
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
                text = stringResource(R.string.prayer_list_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}
