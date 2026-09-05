package ru.edgarakert.biblenote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Note

/**
 * Одна секция списка заметок. Закреплённые и обычные заметки рендерятся двумя вызовами
 * этой функции (с разными [keyPrefix]), а не одним пересортированным списком: пересортировка
 * внутри одной секции даёт артефакт анимации «схлопнулось и появилось заново» при закреплении
 * (в iOS-референсе это же решение принято по той же причине).
 *
 * Инкапсулирует поведение строки заметки, общее для NotesListScreen и FolderScreen (устраняет
 * дублирование, риск R6): режим выбора с чекбоксом, свайп влево для удаления, и long-press меню
 * с единственным пунктом «Закрепить/Открепить» — это единственный дискаверабельный путь к
 * закреплению (риск R7), поэтому оно обязательно даже с уже существующим свайпом на удаление.
 *
 * [keyPrefix] обязателен и должен различаться у секций, иначе ключи `items()` столкнутся.
 */
fun LazyListScope.noteListSection(
    notes: List<Note>,
    keyPrefix: String,
    isSelectMode: Boolean,
    selectedIds: Set<Long>,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
) {
    items(notes, key = { "${keyPrefix}_${it.id}" }) { note ->
        NoteListEntry(
            note = note,
            isSelectMode = isSelectMode,
            isSelected = note.id in selectedIds,
            onClick = { onNoteClick(note) },
            onTogglePin = { onTogglePin(note) },
            onDelete = { onDeleteNote(note) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListEntry(
    note: Note,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    if (isSelectMode) {
        NoteRow(
            note = note,
            isSelectMode = true,
            isSelected = isSelected,
            modifier = Modifier.clickable { onClick() }
        )
        return
    }

    var menuOpen by remember { mutableStateOf(false) }
    val deleteTriggered = remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && !deleteTriggered.value) {
            deleteTriggered.value = true
            onDelete()
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
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(12.dp))
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
                .pointerInput(note.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { menuOpen = true }
                    )
                }
        ) {
            NoteRow(note = note)

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (note.isPinned) R.string.notes_unpin else R.string.notes_pin
                            )
                        )
                    },
                    onClick = { menuOpen = false; onTogglePin() }
                )
            }
        }
    }
}
