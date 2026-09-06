package ru.edgarakert.biblenote.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.ui.components.BibleEditText
import ru.edgarakert.biblenote.ui.components.BibleVerseSheet
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
    val parser = remember { BibleReferenceParser() }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var tappedReference by remember { mutableStateOf<BibleReference?>(null) }
    // Диапазон, который правка из шторки должна заменить: инициализируется живыми
    // индексами тапнутой ссылки, а после каждой правки смещается на длину замены.
    var activeRange by remember { mutableStateOf<IntRange?>(null) }
    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var editToken by remember { mutableLongStateOf(0L) }
    var savedCursorPosition by rememberSaveable { mutableIntStateOf(-1) }

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
                onTextChanged = viewModel::setContent,
                onReferenceTapped = {
                    tappedReference = it
                    activeRange = it.startIndex until it.endIndex
                },
                parser = parser,
                placeholder = stringResource(R.string.editor_content_placeholder),
                initialCursorPosition = savedCursorPosition,
                onCursorPositionChanged = { savedCursorPosition = it },
                pendingEdit = pendingEdit,
                onPendingEditApplied = { pendingEdit = null },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
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
            }
        )
    }
}
