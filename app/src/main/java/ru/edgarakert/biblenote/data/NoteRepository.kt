package ru.edgarakert.biblenote.data

import kotlinx.coroutines.flow.Flow
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.FolderDao
import ru.edgarakert.biblenote.data.db.FolderWithCount
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteDao

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    fun observeRootNotes(): Flow<List<Note>> = noteDao.observeRootNotes()

    fun observeNotesInFolder(folderId: Long): Flow<List<Note>> = noteDao.observeNotesInFolder(folderId)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.search(query)

    fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAllNotes()

    fun observeRootFoldersWithCount(): Flow<List<FolderWithCount>> = folderDao.observeRootFoldersWithCount()

    suspend fun getNoteById(id: Long): Note? = noteDao.getById(id)

    suspend fun saveNote(note: Note): Long = noteDao.upsert(note)

    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    suspend fun deleteNotesByIds(ids: Set<Long>) = noteDao.deleteByIds(ids.toList())

    suspend fun setNotePinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    suspend fun saveFolder(folder: Folder): Long = folderDao.upsert(folder)

    suspend fun deleteFolder(folder: Folder) = folderDao.delete(folder)

    suspend fun renameFolder(id: Long, name: String) = folderDao.rename(id, name)

    fun observeFolderById(id: Long): Flow<Folder?> = folderDao.observeById(id)

    fun observeSubfoldersWithCount(parentId: Long): Flow<List<FolderWithCount>> =
        folderDao.observeSubfoldersWithCount(parentId)

    fun observeAllFolders(): Flow<List<Folder>> = folderDao.observeAllFolders()

    suspend fun moveNotesToFolder(noteIds: Set<Long>, folderId: Long?) =
        noteDao.updateFolderIds(noteIds.toList(), folderId)
}