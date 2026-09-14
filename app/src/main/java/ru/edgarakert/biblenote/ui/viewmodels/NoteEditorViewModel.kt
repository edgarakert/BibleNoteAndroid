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
import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    private val noteId: Long,
    private val repository: NoteRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _formatting = MutableStateFlow<List<FormatRun>>(emptyList())
    val formatting: StateFlow<List<FormatRun>> = _formatting.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private var currentNote: Note? = null
    private var isDirty = false

    init {
        viewModelScope.launch {
            currentNote = repository.getNoteById(noteId)
            currentNote?.let {
                _title.value = it.title
                _content.value = it.content
                _formatting.value = NoteFormattingCodec.decode(it.formatting)
            }
        }
        viewModelScope.launch {
            combine(_title, _content, _formatting) { t, c, f -> Triple(t, c, f) }
                .debounce(500)
                .collect { (t, c, f) ->
                    if (isDirty) saveInternal(t, c, f)
                }
        }
    }

    fun setTitle(value: String) {
        _title.value = value
        isDirty = true
    }

    fun setContent(text: String, runs: List<FormatRun>) {
        _content.value = text
        _formatting.value = runs
        isDirty = true
    }

    fun deleteNote() {
        val note = currentNote ?: return
        viewModelScope.launch {
            repository.deleteNote(note)
            // Без этого сброса удалённая заметка воскресает: и onCleared(), и ещё не
            // отработавший debounce-коллектор увидели бы isDirty и пересохранили строку
            // через upsert с тем же id. Оба пути гасятся здесь — saveInternal тоже
            // выходит по currentNote ?: return.
            currentNote = null
            isDirty = false
            _navigateBack.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val note = currentNote ?: return
        val t = _title.value
        val c = _content.value
        val f = _formatting.value
        // viewModelScope is cancelled at this point; use a short-lived scope that cancels itself
        val saveScope = CoroutineScope(SupervisorJob())
        saveScope.launch {
            try {
                if (t.isBlank() && c.isBlank()) {
                    repository.deleteNote(note)
                } else if (isDirty) {
                    repository.saveNote(
                        note.copy(
                            title = t,
                            content = c,
                            formatting = NoteFormattingCodec.encode(f),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } finally {
                saveScope.cancel()
            }
        }
    }

    private suspend fun saveInternal(title: String, content: String, formatting: List<FormatRun>) {
        val note = currentNote ?: return
        val updated = note.copy(
            title = title,
            content = content,
            formatting = NoteFormattingCodec.encode(formatting),
            updatedAt = System.currentTimeMillis()
        )
        repository.saveNote(updated)
        currentNote = updated
        isDirty = false
    }
}
