package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.Parchment

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedCount > 0) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Parchment,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMove) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = Amber)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.folder_move), color = Amber)
                }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.folder_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
