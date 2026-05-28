package ru.edgarakert.biblenote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY createdAt ASC")
    fun observeRootFolders(): Flow<List<Folder>>

    @Query("""
        SELECT f.*, COUNT(n.id) AS noteCount
        FROM folders f
        LEFT JOIN notes n ON n.folderId = f.id
        WHERE f.parentId IS NULL
        GROUP BY f.id
        ORDER BY f.name COLLATE NOCASE ASC
    """)
    fun observeRootFoldersWithCount(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun observeSubfolders(parentId: Long): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: Folder): Long

    @Delete
    suspend fun delete(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}
