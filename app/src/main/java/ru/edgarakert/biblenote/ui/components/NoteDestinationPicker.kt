package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.NoteRepository

/**
 * Назначение сохранения стихов: несёт только идентификаторы, объекты (Note/Folder)
 * разрешает вызывающий — так пикер не тянет за собой доменные модели, а вызывающему
 * не нужно ждать пикер, чтобы узнать, во что сохраняет.
 */
data class NoteDestination(val folderId: Long? = null, val noteId: Long? = null) {
    val isNewNote: Boolean get() = noteId == null
}

private data class PickerLevel(val folderId: Long?, val name: String)

/**
 * Пикер назначения для сохранения стихов в заметку. Показывает один уровень дерева
 * папок/заметок за раз (по духу — как MoveFolderSheet), но с проваливанием в подпапки
 * через локальный стек уровней, а не плоским списком.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDestinationPicker(
    destination: NoteDestination,
    onDismiss: () -> Unit,
    onSelect: (NoteDestination) -> Unit,
    repository: NoteRepository = koinInject()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val rootName = stringResource(R.string.notes_title)
    var path by remember { mutableStateOf(listOf(PickerLevel(folderId = null, name = rootName))) }
    val current = path.last()

    val subfolders by remember(current.folderId) {
        if (current.folderId == null) repository.observeRootFoldersWithCount()
        else repository.observeSubfoldersWithCount(current.folderId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val notes by remember(current.folderId) {
        if (current.folderId == null) repository.observeRootNotes()
        else repository.observeNotesInFolder(current.folderId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (path.size > 1) {
                    IconButton(onClick = { path = path.dropLast(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = current.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                item {
                    val isSelected = destination.noteId == null && destination.folderId == current.folderId
                    PickerRow(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = stringResource(R.string.share_new_note_in, current.name),
                        labelColor = MaterialTheme.colorScheme.primary,
                        selected = isSelected,
                        onClick = { onSelect(NoteDestination(folderId = current.folderId, noteId = null)) }
                    )
                }

                if (subfolders.isNotEmpty()) {
                    item { PickerSectionHeader(stringResource(R.string.share_folders_section)) }
                    items(subfolders, key = { "folder_${it.folder.id}" }) { fwc ->
                        PickerRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = fwc.folder.name,
                            selected = false,
                            onClick = { path = path + PickerLevel(fwc.folder.id, fwc.folder.name) }
                        )
                    }
                }

                if (notes.isNotEmpty()) {
                    item { PickerSectionHeader(stringResource(R.string.notes_title)) }
                    items(notes, key = { "note_${it.id}" }) { note ->
                        val isSelected = destination.noteId == note.id
                        PickerRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = note.title.ifEmpty { stringResource(R.string.notes_untitled) },
                            subtitle = note.content.take(50).ifEmpty { null },
                            selected = isSelected,
                            onClick = { onSelect(NoteDestination(noteId = note.id)) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PickerSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun PickerRow(
    icon: @Composable () -> Unit,
    label: String,
    subtitle: String? = null,
    labelColor: Color? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
