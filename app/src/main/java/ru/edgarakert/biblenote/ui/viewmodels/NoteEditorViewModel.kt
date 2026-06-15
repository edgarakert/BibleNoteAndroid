package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.db.Note

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

    // Formatting state (reported by BibleEditText on selection change)
    private val _isBold = MutableStateFlow(false)
    val isBold: StateFlow<Boolean> = _isBold.asStateFlow()

    private val _isItalic = MutableStateFlow(false)
    val isItalic: StateFlow<Boolean> = _isItalic.asStateFlow()

    private val _isLarge = MutableStateFlow(false)
    val isLarge: StateFlow<Boolean> = _isLarge.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Trigger counters: increment to signal BibleEditText to perform action
    private val _boldTrigger = MutableStateFlow(0)
    val boldTrigger: StateFlow<Int> = _boldTrigger.asStateFlow()

    private val _italicTrigger = MutableStateFlow(0)
    val italicTrigger: StateFlow<Int> = _italicTrigger.asStateFlow()

    private val _largeTrigger = MutableStateFlow(0)
    val largeTrigger: StateFlow<Int> = _largeTrigger.asStateFlow()

    private val _undoTrigger = MutableStateFlow(0)
    val undoTrigger: StateFlow<Int> = _undoTrigger.asStateFlow()

    private val _redoTrigger = MutableStateFlow(0)
    val redoTrigger: StateFlow<Int> = _redoTrigger.asStateFlow()

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
        _isBold.value = bold
        _isItalic.value = italic
        _isLarge.value = large
    }

    fun setUndoState(canUndo: Boolean, canRedo: Boolean) {
        _canUndo.value = canUndo
        _canRedo.value = canRedo
    }

    fun toggleBold() = _boldTrigger.value++

    fun toggleItalic() = _italicTrigger.value++

    fun toggleLarge() = _largeTrigger.value++

    fun undo() = _undoTrigger.value++

    fun redo() = _redoTrigger.value++

    fun deleteNote() {
        viewModelScope.launch {
            currentNote?.let { repository.deleteNote(it) }
            _navigateBack.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
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