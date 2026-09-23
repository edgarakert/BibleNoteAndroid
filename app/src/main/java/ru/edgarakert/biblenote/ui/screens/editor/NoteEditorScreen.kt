package ru.edgarakert.biblenote.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.NoteTextInsertion
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.bible.VerseSnippetBuilder
import ru.edgarakert.biblenote.data.db.FormatType
import ru.edgarakert.biblenote.ui.components.BibleEditText
import ru.edgarakert.biblenote.ui.components.BibleVerseSheet
import ru.edgarakert.biblenote.ui.components.FormatCommand
import ru.edgarakert.biblenote.ui.components.FormattingToolbar
import ru.edgarakert.biblenote.ui.components.PendingEdit
import ru.edgarakert.biblenote.ui.viewmodels.NoteEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onOpenChapter: (BibleReference) -> Unit = {},
    viewModel: NoteEditorViewModel = koinViewModel(parameters = { parametersOf(noteId) })
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val formatting by viewModel.formatting.collectAsStateWithLifecycle()
    val parser = remember { BibleReferenceParser() }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // rememberSaveable, а не remember: иначе поворот экрана молча закрывал открытую
    // шторку стиха — как и savedCursorPosition ниже, это состояние должно пережить
    // пересоздание Activity.
    var tappedReference by rememberSaveable(stateSaver = BibleReferenceSaver) {
        mutableStateOf<BibleReference?>(null)
    }
    // Диапазон, который правка из шторки должна заменить: инициализируется живыми
    // индексами тапнутой ссылки, а после каждой правки смещается на длину замены.
    var activeRange by rememberSaveable(stateSaver = IntRangeSaver) {
        mutableStateOf<IntRange?>(null)
    }
    // Номер открытия шторки — её ключ ViewModel (см. sessionKey у BibleVerseSheet): каждый тап
    // по ссылке получает свежий выбор стихов, а поворот экрана сохраняет текущий.
    var sheetSession by rememberSaveable { mutableIntStateOf(0) }
    // pendingEdit намеренно НЕ сохраняется: у BibleEditText счётчик уже применённых
    // токенов живёт в обычном remember и после поворота сбрасывается, так что
    // восстановленная правка применилась бы во второй раз и продублировала замену.
    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    // Общий счётчик токенов для pendingEdit И pendingFormatCommand: у каждого из двух каналов
    // в BibleEditText свой независимый "последний применённый" токен (lastAppliedToken и
    // lastAppliedFormatToken), поэтому делить один монотонный источник уникальности безопасно —
    // это проще, чем заводить второй rememberSaveable счётчик только ради форматирования.
    var editToken by rememberSaveable { mutableLongStateOf(0L) }
    var savedCursorPosition by rememberSaveable { mutableIntStateOf(-1) }

    // Чисто UI-состояние тулбара форматирования — не во ViewModel, тем же способом, каким
    // pendingEdit/activeRange уже локальны для этого экрана (задача 16.4).
    var activeFormats by remember { mutableStateOf<Set<FormatType>>(emptySet()) }
    var pendingFormatCommand by remember { mutableStateOf<FormatCommand?>(null) }
    // Панель показывается только пока поле заметки в фокусе — нажатие кнопки не забирает
    // фокус у поля (проверено на устройстве: клавиатура не закрывается при тапе по тулбару),
    // поэтому панель не мигает при собственных нажатиях.
    var isContentFocused by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.navigateBack.collect { onBack() }
    }

    Scaffold(
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
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.editor_menu),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.folder_move)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                },
                                onClick = { showMenu = false /* Phase 9 */ }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.note_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { showMenu = false; showDeleteConfirm = true }
                            )
                        }
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
                .background(MaterialTheme.colorScheme.background)
                // Поднимает тулбар форматирования над клавиатурой вместо того, чтобы клавиатура
                // ложилась поверх него: Scaffold сам не учитывает IME-инсеты в innerPadding.
                .imePadding()
        ) {
            BasicTextField(
                value = title,
                onValueChange = viewModel::setTitle,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                singleLine = false,
                // Заголовок растёт до четырёх строк, дальше поле скроллится внутри себя,
                // а не выдавливает содержимое заметки с экрана.
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_title_placeholder),
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
            )

            BibleEditText(
                text = content,
                formatting = formatting,
                onContentChanged = viewModel::setContent,
                onReferenceTapped = {
                    sheetSession += 1
                    tappedReference = it
                    activeRange = it.startIndex until it.endIndex
                },
                parser = parser,
                placeholder = stringResource(R.string.editor_content_placeholder),
                initialCursorPosition = savedCursorPosition,
                onCursorPositionChanged = { savedCursorPosition = it },
                onActiveFormatsChanged = { activeFormats = it },
                onFocusChanged = { isContentFocused = it },
                formatCommand = pendingFormatCommand,
                onFormatCommandApplied = { pendingFormatCommand = null },
                pendingEdit = pendingEdit,
                onPendingEditApplied = { _, applied ->
                    pendingEdit = null
                    // Правка не легла (текст изменился под нами, диапазон больше не тот) —
                    // закрываем шторку. Продолжать нельзя: activeRange уже сдвинут в расчёте
                    // на успех и теперь мимо. Пользователь тапнет по ссылке заново и получит
                    // свежий живой диапазон.
                    if (!applied) {
                        tappedReference = null
                        activeRange = null
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            )

            if (isContentFocused) {
                FormattingToolbar(
                    isBoldActive = FormatType.BOLD in activeFormats,
                    isItalicActive = FormatType.ITALIC in activeFormats,
                    isSizeActive = FormatType.SIZE in activeFormats,
                    onBold = {
                        editToken += 1
                        pendingFormatCommand = FormatCommand(editToken, FormatType.BOLD)
                    },
                    onItalic = {
                        editToken += 1
                        pendingFormatCommand = FormatCommand(editToken, FormatType.ITALIC)
                    },
                    onSize = {
                        editToken += 1
                        pendingFormatCommand = FormatCommand(editToken, FormatType.SIZE)
                    }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.note_delete)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteNote()
                }) {
                    Text(
                        stringResource(R.string.folder_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.folder_cancel))
                }
            }
        )
    }

    tappedReference?.let { ref ->
        BibleVerseSheet(
            reference = ref,
            sessionKey = "${ref.id}#$sheetSession",
            onDismiss = {
                tappedReference = null
                activeRange = null
            },
            onOpenChapter = { r ->
                tappedReference = null
                activeRange = null
                onOpenChapter(r)
            },
            onVersesChanged = { verses ->
                val range = activeRange ?: (ref.startIndex until ref.endIndex)
                val newText = BibleReference.replacementText(ref.displayText, verses)
                editToken += 1
                pendingEdit = PendingEdit(editToken, range.first, range.last + 1, newText)
                // Длина замены меняется с каждым тапом — следующая правка целится в новый диапазон.
                activeRange = range.first until (range.first + newText.length)
            },
            onInsertVerses = { verses, verseReference ->
                // Текст стихов встаёт на место самой ссылки, а ссылка — мелкой подписью под ними
                // (полное название книги и ровно вставленные стихи).
                val quote = VerseSnippetBuilder.quote(verses, verseReference)
                if (quote.text.isNotEmpty()) {
                    // activeRange, а не границы ref: шторка могла уже переписать ссылку
                    // (onVersesChanged), и её длина в тексте изменилась.
                    val range = activeRange ?: (ref.startIndex until ref.endIndex)
                    // Цитата — на своей строке, см. NoteTextInsertion.replaceOnOwnLine.
                    val r = NoteTextInsertion.replaceOnOwnLine(
                        content, range.first, range.last + 1, quote.text
                    )
                    editToken += 1
                    pendingEdit = PendingEdit(
                        editToken, r.start, r.end, r.text,
                        formatting = quote.formatting.map {
                            it.copy(start = it.start + r.textOffset, end = it.end + r.textOffset)
                        },
                        // Каретка — за вставкой: иначе она могла остаться где угодно, в том числе
                        // вплотную к цитате, и набранное продолжило бы её курсив и цвет.
                        moveCursorToEnd = true
                    )
                }
                tappedReference = null
                activeRange = null
            },
            // Считается по живым тексту и стилям на каждой рекомпозиции: шторка могла уже
            // переписать ссылку (onVersesChanged), и её диапазон сдвинулся.
            onRemoveVerses = run {
                val range = activeRange ?: (ref.startIndex until ref.endIndex)
                NoteTextInsertion.removeQuoteAbove(content, formatting, range.first, range.last + 1)
            }?.let { r ->
                {
                    editToken += 1
                    // Пустой список стилей — подпись снова становится обычной ссылкой.
                    pendingEdit = PendingEdit(
                        editToken, r.start, r.end, r.text,
                        formatting = emptyList(),
                        moveCursorToEnd = true
                    )
                    tappedReference = null
                    activeRange = null
                }
            }
        )
    }
}


/** Сохраняет ссылку между пересозданиями Activity: все поля — примитивы и список чисел. */
private val BibleReferenceSaver: Saver<BibleReference?, Any> = listSaver(
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

private val IntRangeSaver: Saver<IntRange?, Any> = listSaver(
    save = { range -> if (range == null) emptyList() else listOf(range.first, range.last) },
    restore = { saved -> if (saved.isEmpty()) null else saved[0]..saved[1] }
)
