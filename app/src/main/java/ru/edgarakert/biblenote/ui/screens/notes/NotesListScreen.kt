package ru.edgarakert.biblenote.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.FolderWithCount
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.ui.components.FolderRow
import ru.edgarakert.biblenote.ui.components.NoteRow
import ru.edgarakert.biblenote.ui.components.SelectionActionBar
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.AmberSoft
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray
import ru.edgarakert.biblenote.ui.viewmodels.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onNavigateToNote: (Long) -> Unit = {},
    onNavigateToFolder: (Long) -> Unit = {},
    viewModel: NotesViewModel = koinViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSelectMode by viewModel.isSelectMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val rootFolders by viewModel.rootFolders.collectAsStateWithLifecycle()
    val rootNotes by viewModel.rootNotes.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<FolderWithCount?>(null) }
    var renameText by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<FolderWithCount?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.navigateToNote.collect { id -> onNavigateToNote(id) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Parchment,
        topBar = {
            if (isSearchActive) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onClose = { isSearchActive = false; viewModel.setSearchQuery("") }
                )
            } else {
                NotesTopBar(
                    isSelectMode = isSelectMode,
                    canEnterSelectMode = rootNotes.size > 1,
                    onSearchClick = { isSearchActive = true },
                    onCreateNote = viewModel::createNote,
                    onNewFolder = { newFolderName = ""; showNewFolderDialog = true },
                    onEnterSelectMode = viewModel::enterSelectMode,
                    onCancelSelect = viewModel::exitSelectMode
                )
            }
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
                    onMove = {}
                )
            }
        }
    ) { innerPadding ->
        val isSearching = searchQuery.isNotBlank()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isSearching && searchResults.isEmpty() -> EmptyState(
                    icon = { Icon(Icons.Filled.Search, null, tint = AmberSoft, modifier = Modifier.size(48.dp)) },
                    message = stringResource(R.string.search_no_results)
                )

                !isSearching && rootFolders.isEmpty() && rootNotes.isEmpty() -> EmptyState(
                    icon = { Icon(Icons.Filled.Edit, null, tint = AmberSoft, modifier = Modifier.size(48.dp)) },
                    message = stringResource(R.string.notes_empty_state)
                )

                else -> {
                    val displayNotes = if (isSearching) searchResults else rootNotes
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!isSearching) {
                            items(rootFolders, key = { it.folder.id }) { folderWithCount ->
                                FolderListItem(
                                    folderWithCount = folderWithCount,
                                    onClick = { onNavigateToFolder(folderWithCount.folder.id) },
                                    onRename = {
                                        folderToRename = folderWithCount
                                        renameText = folderWithCount.folder.name
                                    },
                                    onDelete = { folderToDelete = folderWithCount }
                                )
                            }
                        }
                        items(displayNotes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                isSelectMode = isSelectMode,
                                isSelected = note.id in selectedIds,
                                onClick = {
                                    if (isSelectMode) viewModel.toggleSelection(note.id)
                                    else onNavigateToNote(note.id)
                                },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = Parchment,
            title = { Text(stringResource(R.string.folder_new), color = Ink) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newFolderName.trim()
                        if (trimmed.isNotEmpty()) viewModel.createFolder(trimmed)
                        showNewFolderDialog = false
                    },
                    enabled = newFolderName.isNotBlank()
                ) { Text(stringResource(R.string.common_save), color = Amber) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(stringResource(R.string.folder_cancel), color = WarmGray)
                }
            }
        )
    }

    folderToRename?.let { f ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            containerColor = Parchment,
            title = { Text(stringResource(R.string.folder_rename), color = Ink) },
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
                        if (trimmed.isNotEmpty()) viewModel.renameFolder(f.folder.id, trimmed)
                        folderToRename = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text(stringResource(R.string.folder_rename), color = Amber) }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(stringResource(R.string.folder_cancel), color = WarmGray)
                }
            }
        )
    }

    folderToDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = Parchment,
            title = { Text(stringResource(R.string.folder_delete_title, f.folder.name), color = Ink) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(f.folder); folderToDelete = null }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.folder_cancel), color = WarmGray)
                }
            }
        )
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            containerColor = Parchment,
            title = { Text(stringResource(R.string.notes_selection_delete_title), color = Ink) },
            text = { Text(stringResource(R.string.note_delete_message), color = WarmGray) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelectedNotes(); showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text(stringResource(R.string.folder_cancel), color = WarmGray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    isSelectMode: Boolean,
    canEnterSelectMode: Boolean,
    onSearchClick: () -> Unit,
    onCreateNote: () -> Unit,
    onNewFolder: () -> Unit,
    onEnterSelectMode: () -> Unit,
    onCancelSelect: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.notes_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
        },
        actions = {
            if (isSelectMode) {
                TextButton(onClick = onCancelSelect) {
                    Text(stringResource(R.string.folder_cancel), color = Amber)
                }
            } else {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Amber)
                }
                IconButton(onClick = onCreateNote) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Amber)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Amber)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_new)) },
                            leadingIcon = { Icon(Icons.Filled.Folder, null, tint = Amber) },
                            onClick = { menuExpanded = false; onNewFolder() }
                        )
                        if (canEnterSelectMode) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.notes_select)) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Amber) },
                                onClick = { menuExpanded = false; onEnterSelectMode() }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder), color = WarmGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    cursorColor = Amber
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Amber)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListItem(
    note: Note,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        NoteRow(
            note = note,
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
private fun FolderListItem(
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
                text = { Text(stringResource(R.string.folder_delete_confirm), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: @Composable () -> Unit,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.size(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = WarmGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}
