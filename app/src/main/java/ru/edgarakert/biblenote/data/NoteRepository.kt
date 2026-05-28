package ru.edgarakert.biblenote.data

import kotlinx.coroutines.flow.Flow
import ru.edgarakert.biblenote.data.db.Folder
import ru.edgarakert.biblenote.data.db.FolderDao
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteDao

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    fun observeRootNotes(): Flow<List<Note>> = noteDao.observeRootNotes()
    
    fun observeNotesInFolder(folderId: Long): Flow<List<Note>> = noteDao.observeNotesInFolder(folderId)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.search(query)

    suspend fun getNoteById(id: Long): Note? = noteDao.getById(id)

    suspend fun saveNote(note: Note): Long = noteDao.upsert(note)

    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteById(id)

    fun observeRootFolders(): Flow<List<Folder>> = folderDao.observeRootFolders()

    fun observeSubfolders(parentId: Long): Flow<List<Folder>> = folderDao.observeSubfolders(parentId)

    suspend fun saveFolder(folder: Folder): Long = folderDao.upsert(folder)

    suspend fun deleteFolder(folder: Folder) = folderDao.delete(folder)

    suspend fun renameFolder(id: Long, name: String) = folderDao.rename(id, name)
}
