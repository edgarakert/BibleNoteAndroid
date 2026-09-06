package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.bible.VerseSnippetBuilder
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.ui.viewmodels.SaveVersesToNoteViewModel

/**
 * Шторка «Стихи в заметку»: текст сниппета предзаполнен из VerseSnippetBuilder и полностью
 * редактируем (пользователь может урезать его до одной ссылки), плюс выбор назначения через
 * NoteDestinationPicker. Текст заметки — единственный источник истины, поэтому сниппет — это
 * обычный текст, а не что-то структурированное: редактор сам подсветит ссылку при рендере.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveVersesToNoteSheet(
    bookName: String,
    chapter: Int,
    verses: List<VerseSnippetBuilder.Verse>,
    onDismiss: () -> Unit,
    onSaved: (noteId: Long, noteTitle: String) -> Unit,
    repository: NoteRepository = koinInject(),
    // Явный key (по духу — как у BibleVerseSheetViewModel с key = reference.id): без него
    // koinViewModel() кеширует единственный инстанс на этот call site и на второй показ
    // шторки (для другого выделения стихов) возвращает СТАРЫЙ инстанс, проигнорировав
    // новые parameters — заголовок новой заметки тогда собирался бы по данным первого
    // открытия шторки, а не текущего.
    viewModel: SaveVersesToNoteViewModel = koinViewModel(
        key = "$bookName|$chapter|${verses.joinToString(",") { it.number.toString() }}",
        parameters = { parametersOf(bookName, chapter, verses.map { it.number }) }
    )
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var text by remember { mutableStateOf(VerseSnippetBuilder.build(bookName, chapter, verses)) }
    var destination by remember { mutableStateOf(NoteDestination()) }
    var showPicker by remember { mutableStateOf(false) }

    val destinationNote by produceState<Note?>(initialValue = null, destination.noteId) {
        value = destination.noteId?.let { repository.getNoteById(it) }
    }
    val destinationFolder by produceState<Folder?>(initialValue = null, destination.folderId, destination.noteId) {
        value = if (destination.noteId == null) {
            destination.folderId?.let { repository.observeFolderById(it).first() }
        } else {
            null
        }
    }

    val destinationLabel = when {
        destinationNote != null -> destinationNote!!.title.ifEmpty { stringResource(R.string.notes_untitled) }
        destinationFolder != null -> stringResource(R.string.share_new_note_in, destinationFolder!!.name)
        else -> stringResource(R.string.share_new_note_in, stringResource(R.string.notes_title))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.verse_save_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        viewModel.save(text, destination) { noteId, noteTitle -> onSaved(noteId, noteTitle) }
                    },
                    enabled = text.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.common_save), color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = stringResource(R.string.verse_save_text),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.share_save_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = destinationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        NoteDestinationPicker(
            destination = destination,
            onDismiss = { showPicker = false },
            onSelect = { newDestination ->
                destination = newDestination
                showPicker = false
            }
        )
    }
}
