package ru.edgarakert.biblenote.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.FolderWithCount
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.ui.components.FolderRow
import ru.edgarakert.biblenote.ui.components.MoveFolderSheet
import ru.edgarakert.biblenote.ui.components.SelectionActionBar
import ru.edgarakert.biblenote.ui.components.noteListSection
import ru.edgarakert.biblenote.ui.viewmodels.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: Long,
    onBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit,
    onNavigateToFolder: (Long) -> Unit = {},
    viewModel: FolderViewModel = koinViewModel(parameters = { parametersOf(folderId) })
) {
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val pinnedNotes by viewModel.pinnedNotes.collectAsStateWithLifecycle()
    val unpinnedNotes by viewModel.unpinnedNotes.collectAsStateWithLifecycle()
    val subfolders by viewModel.subfolders.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val isSelectMode by viewModel.isSelectMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    val isSubfolder = folder?.parentId != null

    var showMoveSheet by remember { mutableStateOf(false) }
    var showNewSubfolderDialog by remember { mutableStateOf(false) }
    var newSubfolderName by remember { mutableStateOf("") }
    var showDeleteSelfDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameText by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<FolderWithCount?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.navigateToNote.collect { id -> onNavigateToNote(id) }
    }
    LaunchedEffect(viewModel) {
        viewModel.navigateUp.collect { onBack() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FolderTopBar(
                title = folder?.name ?: "",
                isSelectMode = isSelectMode,
                isSubfolder = isSubfolder,
                canEnterSelectMode = notes.isNotEmpty(),
                onBack = onBack,
                onCreateNote = viewModel::createNote,
                onEnterSelectMode = viewModel::enterSelectMode,
                onCancelSelect = viewModel::exitSelectMode,
                onRenameThis = {
                    folderToRename = folder
                    renameText = folder?.name ?: ""
                },
                onNewSubfolder = { newSubfolderName = ""; showNewSubfolderDialog = true },
                onDeleteThis = { showDeleteSelfDialog = true }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                SelectionActionBar(
                    selectedCount = selectedIds.size,
                    onDelete = { showDeleteSelectedConfirm = true },
                    onMove = { showMoveSheet = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isEmpty = notes.isEmpty() && (isSubfolder || subfolders.isEmpty())
            if (isEmpty) {
                FolderEmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!isSubfolder) {
                        items(subfolders, key = { it.folder.id }) { fwc ->
                            SubfolderListItem(
                                folderWithCount = fwc,
                                onClick = { onNavigateToFolder(fwc.folder.id) },
                                onRename = { folderToRename = fwc.folder; renameText = fwc.folder.name },
                                onDelete = { folderToDelete = fwc }
                            )
                        }
                    }
                    val onNoteClick = { note: Note ->
                        if (isSelectMode) viewModel.toggleSelection(note.id)
                        else onNavigateToNote(note.id)
                    }

                    noteListSection(
                        notes = pinnedNotes,
                        keyPrefix = "pinned",
                        isSelectMode = isSelectMode,
                        selectedIds = selectedIds,
                        onNoteClick = onNoteClick,
                        onTogglePin = viewModel::togglePin,
                        onDeleteNote = viewModel::deleteNote
                    )
                    noteListSection(
                        notes = unpinnedNotes,
                        keyPrefix = "note",
                        isSelectMode = isSelectMode,
                        selectedIds = selectedIds,
                        onNoteClick = onNoteClick,
                        onTogglePin = viewModel::togglePin,
                        onDeleteNote = viewModel::deleteNote
                    )
                }
            }
        }
    }

    if (showMoveSheet) {
        MoveFolderSheet(
            allFolders = allFolders,
            onDismiss = { showMoveSheet = false },
            onMove = { targetId -> viewModel.moveSelectedNotes(targetId); showMoveSheet = false },
            onCreateFolderAndMove = { name -> viewModel.createFolderAndMoveSelected(name); showMoveSheet = false }
        )
    }

    if (showNewSubfolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubfolderDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(stringResource(R.string.folder_subfolder_new), color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                OutlinedTextField(
                    value = newSubfolderName,
                    onValueChange = { newSubfolderName = it },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newSubfolderName.trim()
                        if (trimmed.isNotEmpty()) viewModel.createSubfolder(trimmed)
                        showNewSubfolderDialog = false
                    },
                    enabled = newSubfolderName.isNotBlank()
                ) { Text(stringResource(R.string.folder_new), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubfolderDialog = false }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    folderToRename?.let { f ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.folder_rename), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotEmpty()) viewModel.renameFolder(f.id, trimmed)
                        folderToRename = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text(stringResource(R.string.folder_rename), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    folderToDelete?.let { fwc ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    stringResource(R.string.folder_delete_title, fwc.folder.name),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = if (fwc.noteCount > 0) {
                {
                    Text(
                        stringResource(R.string.folder_delete_notes_warning),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSubfolder(fwc.folder); folderToDelete = null }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteSelfDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelfDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    stringResource(R.string.folder_delete_title, folder?.name ?: ""),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = if (notes.isNotEmpty()) {
                {
                    Text(
                        stringResource(R.string.folder_delete_notes_warning),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { viewModel.deleteThisFolder(); showDeleteSelfDialog = false }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelfDialog = false }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    stringResource(R.string.notes_selection_delete_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.note_delete_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelectedNotes(); showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderTopBar(
    title: String,
    isSelectMode: Boolean,
    isSubfolder: Boolean,
    canEnterSelectMode: Boolean,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onEnterSelectMode: () -> Unit,
    onCancelSelect: () -> Unit,
    onRenameThis: () -> Unit,
    onNewSubfolder: () -> Unit,
    onDeleteThis: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            if (isSelectMode) {
                TextButton(onClick = onCancelSelect) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = onCreateNote) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_rename)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = { menuExpanded = false; onRenameThis() }
                        )
                        if (!isSubfolder) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.folder_subfolder_new)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.NoteAdd,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = { menuExpanded = false; onNewSubfolder() }
                            )
                        }
                        if (canEnterSelectMode) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.notes_select)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = { menuExpanded = false; onEnterSelectMode() }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.folder_delete_confirm),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menuExpanded = false; onDeleteThis() }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun SubfolderListItem(
    folderWithCount: FolderWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { menuExpanded = true }
                )
            }
    ) {
        FolderRow(folderWithCount = folderWithCount)

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.folder_rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { menuExpanded = false; onRename() }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.folder_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun FolderEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.NoteAdd,
                null,
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.notes_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}