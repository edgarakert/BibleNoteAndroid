package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.db.Note

enum class ShareMode { NEW, EXISTING }

data class ShareUiState(
    val sharedText: String = "",
    val mode: ShareMode = ShareMode.NEW,
    val searchQuery: String = "",
    val selectedNoteId: Long? = null,
    val isSaving: Boolean = false,
    val hasError: Boolean = false
)

class ShareNoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _dismiss = MutableSharedFlow<Unit>()
    val dismiss: SharedFlow<Unit> = _dismiss.asSharedFlow()

    // WhileSubscribed(0): short-lived Activity — release DB cursor immediately on unsubscribe
    val allNotes: StateFlow<List<Note>> = repository.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    fun setSharedText(text: String) {
        _uiState.update { it.copy(sharedText = text) }
    }

    fun setMode(mode: ShareMode) {
        _uiState.update { it.copy(mode = mode, searchQuery = "", selectedNoteId = null) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedNote(id: Long?) {
        _uiState.update { it.copy(selectedNoteId = id) }
    }

    fun saveAsNewNote() {
        val state = _uiState.value
        if (state.sharedText.isBlank()) {
            _uiState.update { it.copy(hasError = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            try {
                repository.saveNote(
                    Note(
                        title = extractTitle(state.sharedText),
                        content = state.sharedText
                    )
                )
                _dismiss.emit(Unit)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun appendToNote(noteId: Long) {
        val state = _uiState.value
        if (state.sharedText.isBlank()) {
            _uiState.update { it.copy(hasError = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            try {
                val existing = repository.getNoteById(noteId)
                if (existing == null) {
                    _uiState.update { it.copy(hasError = true) }
                    return@launch
                }
                val updatedContent = existing.content.trimEnd() + "\n\n" + state.sharedText
                repository.saveNote(
                    existing.copy(
                        content = updatedContent,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _dismiss.emit(Unit)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun extractTitle(text: String): String =
        text.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(60)
            ?: ""
}