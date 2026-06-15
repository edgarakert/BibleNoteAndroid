package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.db.Note

data class FormattingState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val large: Boolean = false,
)

data class UndoRedoState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

sealed class FormattingCommand {
    object Bold : FormattingCommand()
    object Italic : FormattingCommand()
    object Large : FormattingCommand()
    object Undo : FormattingCommand()
    object Redo : FormattingCommand()
}

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    private val noteId: Long,
    private val repository: NoteRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _contentHtml = MutableStateFlow<String?>(null)
    val contentHtml: StateFlow<String?> = _contentHtml.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val _formattingState = MutableStateFlow(FormattingState())
    val formattingState: StateFlow<FormattingState> = _formattingState.asStateFlow()

    private val _undoRedoState = MutableStateFlow(UndoRedoState())
    val undoRedoState: StateFlow<UndoRedoState> = _undoRedoState.asStateFlow()

    private val _formattingCommands = Channel<FormattingCommand>(Channel.UNLIMITED)
    val formattingCommands: Flow<FormattingCommand> = _formattingCommands.receiveAsFlow()

    private var currentNote: Note? = null
    private var isDirty = false

    init {
        viewModelScope.launch {
            currentNote = repository.getNoteById(noteId)
            currentNote?.let {
                _title.value = it.title
                _content.value = it.content
                _contentHtml.value = it.contentHtml
            }
        }
        viewModelScope.launch {
            combine(_title, _content, _contentHtml) { t, c, ch -> Triple(t, c, ch) }
                .debounce(500)
                .collect { (t, c, ch) ->
                    if (isDirty) saveInternal(t, c, ch)
                }
        }
    }

    fun setTitle(value: String) {
        _title.value = value
        isDirty = true
    }

    fun setContent(plain: String, html: String?) {
        _content.value = plain
        _contentHtml.value = html
        isDirty = true
    }

    fun setFormattingState(bold: Boolean, italic: Boolean, large: Boolean) {
        _formattingState.value = FormattingState(bold, italic, large)
    }

    fun setUndoState(canUndo: Boolean, canRedo: Boolean) {
        _undoRedoState.value = UndoRedoState(canUndo, canRedo)
    }

    fun toggleBold() = _formattingCommands.trySend(FormattingCommand.Bold)

    fun toggleItalic() = _formattingCommands.trySend(FormattingCommand.Italic)

    fun toggleLarge() = _formattingCommands.trySend(FormattingCommand.Large)

    fun undo() = _formattingCommands.trySend(FormattingCommand.Undo)

    fun redo() = _formattingCommands.trySend(FormattingCommand.Redo)

    fun deleteNote() {
        viewModelScope.launch {
            currentNote?.let { repository.deleteNote(it) }
            _navigateBack.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _formattingCommands.close()
        val note = currentNote ?: return

        val title = _title.value
        val content = _content.value
        val contentHtml = _contentHtml.value

        val saveScope = CoroutineScope(SupervisorJob())
        saveScope.launch {
            try {
                if (title.isBlank() && content.isBlank()) {
                    repository.deleteNote(note)
                } else if (isDirty) {
                    repository.saveNote(
                        note.copy(
                            title = title,
                            content = content,
                            contentHtml = contentHtml,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } finally {
                saveScope.cancel()
            }
        }
    }

    private suspend fun saveInternal(title: String, content: String, contentHtml: String?) {
        val note = currentNote ?: return
        val updated = note.copy(
            title = title,
            content = content,
            contentHtml = contentHtml,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveNote(updated)
        currentNote = updated
        isDirty = false
    }
}