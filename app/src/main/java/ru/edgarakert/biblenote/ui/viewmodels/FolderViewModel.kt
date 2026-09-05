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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.FolderWithCount
import ru.edgarakert.biblenote.data.db.Note

class FolderViewModel(
    private val folderId: Long,
    private val repository: NoteRepository
) : ViewModel() {

    // Reactive — always reflects DB state; also survives rename without manual patching
    val folder: StateFlow<Folder?> = repository.observeFolderById(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isSelectMode = MutableStateFlow(false)
    val isSelectMode: StateFlow<Boolean> = _isSelectMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _navigateToNote = MutableSharedFlow<Long>()
    val navigateToNote: SharedFlow<Long> = _navigateToNote.asSharedFlow()

    private val _navigateUp = MutableSharedFlow<Unit>()
    val navigateUp: SharedFlow<Unit> = _navigateUp.asSharedFlow()

    val notes: StateFlow<List<Note>> = repository.observeNotesInFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Разделение закреплённых/обычных — в коде, а не в SQL: порядок внутри каждой
    // группы остаётся updatedAt DESC (см. NoteDao.observeNotesInFolder).
    val pinnedNotes: StateFlow<List<Note>> = notes
        .map { list -> list.filter { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unpinnedNotes: StateFlow<List<Note>> = notes
        .map { list -> list.filterNot { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subfolders: StateFlow<List<FolderWithCount>> =
        repository.observeSubfoldersWithCount(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allFolders: StateFlow<List<Folder>> = repository.observeAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createNote() {
        viewModelScope.launch {
            val id = repository.saveNote(Note(folderId = folderId))
            _navigateToNote.emit(id)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setNotePinned(note.id, !note.isPinned) }
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            repository.deleteNotesByIds(_selectedIds.value)
            exitSelectMode()
        }
    }

    fun moveSelectedNotes(targetFolderId: Long?) {
        viewModelScope.launch {
            repository.moveNotesToFolder(_selectedIds.value, targetFolderId)
            exitSelectMode()
        }
    }

    fun createFolderAndMoveSelected(name: String) {
        viewModelScope.launch {
            val newFolderId = repository.saveFolder(Folder(name = name))
            repository.moveNotesToFolder(_selectedIds.value, newFolderId)
            exitSelectMode()
        }
    }

    fun enterSelectMode() {
        _isSelectMode.value = true
        _selectedIds.value = emptySet()
    }

    fun exitSelectMode() {
        _isSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun createSubfolder(name: String) {
        viewModelScope.launch { repository.saveFolder(Folder(name = name, parentId = folderId)) }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch { repository.renameFolder(id, name) }
    }

    fun deleteSubfolder(folder: Folder) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }

    fun deleteThisFolder() {
        viewModelScope.launch {
            val f = folder.value ?: return@launch
            repository.deleteFolder(f)
            _navigateUp.emit(Unit)
        }
    }
}
