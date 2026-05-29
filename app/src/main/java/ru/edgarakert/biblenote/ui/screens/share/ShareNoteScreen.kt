package ru.edgarakert.biblenote.ui.screens.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.DarkAmber
import ru.edgarakert.biblenote.ui.theme.DarkHairline
import ru.edgarakert.biblenote.ui.theme.DarkInk
import ru.edgarakert.biblenote.ui.theme.DarkWarmGray
import ru.edgarakert.biblenote.ui.theme.Hairline
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.WarmGray
import ru.edgarakert.biblenote.ui.viewmodels.ShareMode
import ru.edgarakert.biblenote.ui.viewmodels.ShareUiState

@Composable
fun ShareNoteSheetContent(
    uiState: ShareUiState,
    notes: List<Note>,
    darkTheme: Boolean,
    onModeChange: (ShareMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectNote: (Long) -> Unit,
    onSaveNewNote: () -> Unit,
    onAppendToNote: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (darkTheme) DarkInk else Ink
    val secondaryColor = if (darkTheme) DarkWarmGray else WarmGray
    val accentColor = if (darkTheme) DarkAmber else Amber
    val hairlineColor = if (darkTheme) DarkHairline else Hairline
    val previewBg = if (darkTheme) Color(0xFF2E271F) else Color(0xFFF0E8DA)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.share_title),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = textColor
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.sharedText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBg)
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.sharedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        ShareModeOption(
            label = stringResource(R.string.share_mode_new),
            selected = uiState.mode == ShareMode.NEW,
            onClick = { onModeChange(ShareMode.NEW) },
            textColor = textColor,
            accentColor = accentColor
        )
        HorizontalDivider(color = hairlineColor, thickness = 0.5.dp)
        ShareModeOption(
            label = stringResource(R.string.share_mode_existing),
            selected = uiState.mode == ShareMode.EXISTING,
            onClick = { onModeChange(ShareMode.EXISTING) },
            textColor = textColor,
            accentColor = accentColor
        )

        if (uiState.mode == ShareMode.EXISTING) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(stringResource(R.string.search_placeholder), color = secondaryColor)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = hairlineColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = accentColor
                )
            )

            Spacer(Modifier.height(8.dp))

            val filtered = if (uiState.searchQuery.isBlank()) notes
            else notes.filter {
                it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                    it.content.contains(uiState.searchQuery, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(filtered, key = { it.id }) { note ->
                    NotePickerRow(
                        note = note,
                        isSelected = uiState.selectedNoteId == note.id,
                        onClick = { onSelectNote(note.id) },
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor,
                        hairlineColor = hairlineColor
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }

        if (uiState.hasError) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.share_error_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor),
                border = BorderStroke(1.dp, hairlineColor)
            ) {
                Text(stringResource(R.string.share_cancel))
            }

            val canSave = !uiState.isSaving &&
                (uiState.mode == ShareMode.NEW || uiState.selectedNoteId != null)

            Button(
                onClick = {
                    if (uiState.mode == ShareMode.NEW) onSaveNewNote()
                    else uiState.selectedNoteId?.let { onAppendToNote(it) }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                enabled = canSave
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.common_save), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ShareModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick = null: the Row's selectable is the sole tap target, preventing double-fire
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

@Composable
private fun NotePickerRow(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    hairlineColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onClick = null: the Row's selectable is the sole tap target
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(color = hairlineColor, thickness = 0.5.dp)
    }
}