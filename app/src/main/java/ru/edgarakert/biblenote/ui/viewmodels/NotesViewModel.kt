package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.FolderWithCount
import ru.edgarakert.biblenote.data.db.Note

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSelectMode = MutableStateFlow(false)
    val isSelectMode: StateFlow<Boolean> = _isSelectMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _navigateToNote = MutableSharedFlow<Long>()
    val navigateToNote: SharedFlow<Long> = _navigateToNote.asSharedFlow()

    val rootFolders: StateFlow<List<FolderWithCount>> = repository.observeRootFoldersWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allFolders: StateFlow<List<Folder>> = repository.observeAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rootNotes: StateFlow<List<Note>> = repository.observeRootNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchResults: StateFlow<List<Note>> = _searchQuery
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else repository.searchNotes(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
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

    fun createNote() {
        viewModelScope.launch {
            val id = repository.saveNote(Note())
            _navigateToNote.emit(id)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            repository.deleteNotesByIds(_selectedIds.value)
            exitSelectMode()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch { repository.saveFolder(Folder(name = name)) }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch { repository.renameFolder(id, name) }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }

    fun moveSelectedNotes(targetFolderId: Long?) {
        viewModelScope.launch {
            repository.moveNotesToFolder(_selectedIds.value, targetFolderId)
            exitSelectMode()
        }
    }

    fun createFolderAndMoveSelected(name: String) {
        viewModelScope.launch {
            val folderId = repository.saveFolder(Folder(name = name))
            repository.moveNotesToFolder(_selectedIds.value, folderId)
            exitSelectMode()
        }
    }
}
