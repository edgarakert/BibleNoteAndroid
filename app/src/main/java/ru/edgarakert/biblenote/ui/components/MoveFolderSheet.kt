package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveFolderSheet(
    allFolders: List<Folder>,
    onDismiss: () -> Unit,
    onMove: (targetFolderId: Long?) -> Unit,
    onCreateFolderAndMove: (name: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val rootFolders = remember(allFolders) { allFolders.filter { it.parentId == null } }
    val subfoldersByParent = remember(allFolders) {
        allFolders.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = stringResource(R.string.folder_move_to),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn {
            item {
                MoveSheetRow(
                    icon = {
                        Icon(
                            Icons.Outlined.Inbox,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = stringResource(R.string.folder_no_folder),
                    onClick = { onMove(null) }
                )
            }

            rootFolders.forEach { folder ->
                item(key = folder.id) {
                    MoveSheetRow(
                        icon = {
                            Icon(
                                Icons.Filled.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = folder.name,
                        onClick = { onMove(folder.id) }
                    )
                }
                val subs = subfoldersByParent[folder.id].orEmpty()
                items(subs, key = { "sub_${it.id}" }) { sub ->
                    MoveSheetRow(
                        icon = {
                            Icon(
                                Icons.Outlined.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = sub.name,
                        indent = 20.dp,
                        onClick = { onMove(sub.id) }
                    )
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                MoveSheetRow(
                    icon = {
                        Icon(
                            Icons.Filled.CreateNewFolder,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = stringResource(R.string.folder_new),
                    labelColor = MaterialTheme.colorScheme.primary,
                    onClick = { newFolderName = ""; showNewFolderDialog = true }
                )
                Spacer(Modifier.size(16.dp))
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.folder_new), color = MaterialTheme.colorScheme.onSurface) },
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
                        if (trimmed.isNotEmpty()) {
                            onCreateFolderAndMove(trimmed)
                            showNewFolderDialog = false
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) { Text(stringResource(R.string.common_save), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun MoveSheetRow(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: Color? = null,
    indent: Dp = 0.dp,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp + indent, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
