package ru.edgarakert.biblenote.ui.screens.prayer

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.db.PrayerEntry
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import ru.edgarakert.biblenote.data.prayer.PrayerAnswerDate
import ru.edgarakert.biblenote.ui.components.BibleVerseSheet
import ru.edgarakert.biblenote.ui.components.prayer.PrayerCategoryBadge
import ru.edgarakert.biblenote.ui.viewmodels.PrayerDetailUiState
import ru.edgarakert.biblenote.ui.viewmodels.PrayerDetailViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Карточка просьбы (задача 14.10) — самый насыщенный экран фазы. Порядок блоков сверху вниз
 * фиксирован планом: заголовок+бейдж → тело (если непусто) → стихи (если хоть один разобрался)
 * → блок ответа (только `ANSWERED`) → кнопки действий (только `ACTIVE`) → дописки (всегда).
 *
 * [uiState] закрывает экран сам, как только просьба пропадает из БД (поправка 3 к плану) —
 * неважно, удалили её отсюда через меню или откуда-то ещё. Пока не пришло ни одного значения,
 * показывается прогресс, а не пустой экран/краш.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerDetailScreen(
    requestId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenChapter: (BibleReference) -> Unit = {},
    viewModel: PrayerDetailViewModel = koinViewModel(parameters = { parametersOf(requestId) })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val verseReferences by viewModel.verseReferences.collectAsStateWithLifecycle()
    val entryText by viewModel.entryText.collectAsStateWithLifecycle()
    val showAnswerSheet by viewModel.showAnswerSheet.collectAsStateWithLifecycle()
    val answerText by viewModel.answerText.collectAsStateWithLifecycle()

    // Пересчёт «помолились ли сегодня» при возврате экрана на передний план — тот же приём,
    // что в PrayerTodayScreen (поправка 2 к плану).
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(viewModel::onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState) {
        if (uiState is PrayerDetailUiState.NotFound) onBack()
    }

    // rememberSaveable, а не remember: та же ловушка, что была с BibleVerseSheet в
    // NoteEditorScreen на повороте экрана в фазе 13 — открытая шторка не должна пропадать.
    var tappedReference by rememberSaveable(stateSaver = PrayerVerseReferenceSaver) {
        mutableStateOf<BibleReference?>(null)
    }
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val loaded = uiState as? PrayerDetailUiState.Loaded

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.prayer_actions),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (loaded.request.status == PrayerStatus.ACTIVE) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.folder_rename)) },
                                    onClick = { menuOpen = false; onEdit(requestId) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.prayer_detail_entrust_menu)) },
                                    onClick = { menuOpen = false; viewModel.entrust() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.prayer_delete)) },
                                onClick = { menuOpen = false; showDeleteConfirm = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (loaded == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            PrayerDetailContent(
                request = loaded.request,
                hasPrayedToday = loaded.hasPrayedToday,
                verseReferences = verseReferences,
                entries = entries,
                entryText = entryText,
                onEntryTextChange = viewModel::setEntryText,
                onAddEntry = viewModel::addEntry,
                onVerseTap = { tappedReference = it },
                onMarkPrayedToday = viewModel::markPrayedToday,
                onOpenAnswerSheet = viewModel::openAnswerSheet,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (loaded != null && showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(stringResource(R.string.prayer_delete), color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    stringResource(R.string.note_delete_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Без onVersesChanged: это режим чтения ссылки, а не правки текста заметки (поправка 4 к
    // плану) — второй парсер здесь не нужен, используется тот же BibleVerseSheet, что и в
    // редакторе заметок. key = reference.id внутри самого BibleVerseSheet гарантирует, что
    // повторное открытие для ДРУГОЙ ссылки не покажет содержимое первой (та же ловушка Koin,
    // что уже была поймана в фазе 13).
    tappedReference?.let { ref ->
        BibleVerseSheet(
            reference = ref,
            onDismiss = { tappedReference = null },
            onOpenChapter = { r ->
                tappedReference = null
                onOpenChapter(r)
            }
        )
    }

    if (showAnswerSheet && loaded != null) {
        PrayerAnswerSheet(
            request = loaded.request,
            answerText = answerText,
            onAnswerTextChange = viewModel::setAnswerText,
            onDismiss = viewModel::dismissAnswerSheet,
            onSave = { pickerMillis -> viewModel.saveAnswer(pickerMillis) }
        )
    }
}

@Composable
private fun PrayerDetailContent(
    request: PrayerRequest,
    hasPrayedToday: Boolean,
    verseReferences: List<BibleReference>,
    entries: List<PrayerEntry>,
    entryText: String,
    onEntryTextChange: (String) -> Unit,
    onAddEntry: () -> Unit,
    onVerseTap: (BibleReference) -> Unit,
    onMarkPrayedToday: () -> Unit,
    onOpenAnswerSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Заголовок + бейдж категории.
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = request.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            PrayerCategoryBadge(category = request.category)
        }

        // 2. Тело — только если непусто.
        if (request.body.isNotBlank()) {
            Text(
                text = request.body,
                fontFamily = FontFamily.Serif,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 3. Стихи — только если хоть одна ссылка разобралась.
        if (verseReferences.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                verseReferences.forEach { ref ->
                    Text(
                        text = ref.displayText,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            onClickLabel = stringResource(R.string.verse_save_open),
                            role = Role.Button
                        ) { onVerseTap(ref) }
                    )
                }
            }
        }

        // 4. Блок ответа — только при ANSWERED && answerText != null.
        val answerText = request.answerText
        if (request.status == PrayerStatus.ANSWERED && answerText != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_answer_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = answerText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 5. Кнопки действий — только при ACTIVE.
        if (request.status == PrayerStatus.ACTIVE) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    // IntrinsicSize.Min + fillMaxHeight: «Помолился сегодня» на узком экране
                    // переносится на две строки, и без этого кнопки выходили разной высоты.
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    // «Помолился сегодня» — мягкая, выключена если уже молились сегодня.
                    Button(
                        onClick = onMarkPrayedToday,
                        enabled = !hasPrayedToday,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text(stringResource(R.string.prayer_detail_pray_today_button))
                    }
                    // «Бог ответил» — сплошная, никогда не выключена.
                    Button(
                        onClick = onOpenAnswerSheet,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text(stringResource(R.string.prayer_detail_answered_button))
                    }
                }
            }
        }

        // 6. Дописки — всегда, при любом статусе.
        PrayerEntriesSection(
            entryText = entryText,
            onEntryTextChange = onEntryTextChange,
            onAddEntry = onAddEntry,
            entries = entries
        )
    }
}

@Composable
private fun PrayerEntriesSection(
    entryText: String,
    onEntryTextChange: (String) -> Unit,
    onAddEntry: () -> Unit,
    entries: List<PrayerEntry>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.prayer_detail_entries_header).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = entryText,
                onValueChange = onEntryTextChange,
                placeholder = { Text(stringResource(R.string.prayer_detail_add_entry_placeholder)) },
                modifier = Modifier.weight(1f)
            )
            // Выключена, пока текст пуст после trim.
            TextButton(onClick = onAddEntry, enabled = entryText.trim().isNotEmpty()) {
                Text(stringResource(R.string.prayer_detail_add_entry_button))
            }
        }
        entries.forEach { entry ->
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = formatEntryDate(entry.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = entry.text,
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Шторка ответа. Дата — `DatePicker` только с датой (без времени): начальный выбор — сегодня,
 * ограничение через `SelectableDates` — не позже сегодня и не раньше даты создания просьбы
 * (поправка 1 к плану, иначе на экране «Отвеченные» 14.12 получилась бы отрицательная
 * длительность). Момент времени ответа считает `PrayerAnswerDate.resolveAnsweredAt`, а не
 * наивная трактовка `selectedDateMillis` как обычного `Instant`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerAnswerSheet(
    request: PrayerRequest,
    answerText: String,
    onAnswerTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Long) -> Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }
    val createdLocalDate = remember(request.createdAt, zone) {
        Instant.ofEpochMilli(request.createdAt).atZone(zone).toLocalDate()
    }
    val todayLocalDate = remember(zone) { LocalDate.now(zone) }
    val selectableDates = remember(createdLocalDate, todayLocalDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = PrayerAnswerDate.dateForPickerMillis(utcTimeMillis)
                return !date.isBefore(createdLocalDate) && !date.isAfter(todayLocalDate)
            }

            override fun isSelectableYear(year: Int): Boolean =
                year in createdLocalDate.year..todayLocalDate.year
        }
    }
    // rememberDatePickerState сохраняет выбор через rememberSaveable сам по себе — шторка
    // не теряет выбранную дату при повороте, пока остаётся в композиции (а остаётся, так как
    // showAnswerSheet живёт во ViewModel — поправка 8 к плану).
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = PrayerAnswerDate.pickerMillisForDate(todayLocalDate),
        selectableDates = selectableDates
    )
    val canSave = answerText.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SheetHorizontalPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.prayer_answer_title),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = answerText,
                onValueChange = onAnswerTextChange,
                placeholder = { Text(stringResource(R.string.prayer_answer_placeholder)) },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.prayer_answer_date_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                // Фон шторки вместо стандартного лавандового контейнера Material —
                // иначе календарь выпадает из пергаментной палитры.
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                // Сетке DatePicker (7 колонок по 48dp плюс поля) на узком телефоне нужны все
                // 360dp, а колонка шторки отдаёт ей ширину минус отступы 20dp с каждой стороны —
                // колонка воскресенья обрезалась. Даём календарю выйти за эти отступы.
                modifier = Modifier.layout { measurable, constraints ->
                    val bleed = SheetHorizontalPadding.roundToPx() * 2
                    val width = constraints.maxWidth + bleed
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(-bleed / 2, 0)
                    }
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                            ?: PrayerAnswerDate.pickerMillisForDate(todayLocalDate)
                        onSave(millis)
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.prayer_answer_save))
                }
            }
        }
    }
}

private fun formatEntryDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))

/** Сохраняет тапнутую ссылку между пересозданиями Activity — см. аналог в NoteEditorScreen. */
private val PrayerVerseReferenceSaver: Saver<BibleReference?, Any> = listSaver(
    save = { ref ->
        if (ref == null) emptyList() else listOf(
            ref.bookId, ref.chapter, ref.verseStart ?: -1, ref.verseEnd ?: -1,
            ref.verseList, ref.displayText, ref.startIndex, ref.endIndex
        )
    },
    restore = { saved ->
        if (saved.isEmpty()) null else {
            @Suppress("UNCHECKED_CAST")
            BibleReference(
                bookId = saved[0] as Int,
                chapter = saved[1] as Int,
                verseStart = (saved[2] as Int).takeIf { it >= 0 },
                verseEnd = (saved[3] as Int).takeIf { it >= 0 },
                verseList = saved[4] as List<Int>,
                displayText = saved[5] as String,
                startIndex = saved[6] as Int,
                endIndex = saved[7] as Int
            )
        }
    }
)

/** Боковой отступ шторки ответа; календарь выходит за него, см. DatePicker выше. */
private val SheetHorizontalPadding = 20.dp
