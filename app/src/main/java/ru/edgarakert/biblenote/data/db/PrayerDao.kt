package ru.edgarakert.biblenote.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_requests ORDER BY createdAt DESC")
    fun observeAllRequests(): Flow<List<PrayerRequest>>

    @Query("SELECT * FROM prayer_requests WHERE id = :id")
    fun observeRequestById(id: Long): Flow<PrayerRequest?>

    @Query("SELECT * FROM prayer_requests WHERE id = :id")
    suspend fun getRequestById(id: Long): PrayerRequest?

    /**
     * НЕ `@Insert(onConflict = REPLACE)`: REPLACE на существующей строке — это DELETE+INSERT
     * на уровне SQLite, а `prayer_entries.requestId` объявлен с `ON DELETE CASCADE` (задача 14.1),
     * и Room включает `PRAGMA foreign_keys = ON` при каждом открытии соединения. Значит REPLACE на
     * `upsertRequest` молча удалил бы ВСЕ дописки просьбы при любом её обновлении («помолился
     * сегодня», правка заголовка, «отвечено», «доверить Богу») — см. `PrayerDaoTest
     * .updatingARequestKeepsItsEntries`, которая ловит именно это. `@Upsert` (Room 2.5+) делает
     * `INSERT`, а при конфликте по PK — `UPDATE`, не трогая зависимые строки. Обратная сторона:
     * при обновлении существующей строки он возвращает `-1L`, а не её id (см. `PrayerRepository`).
     * В `NoteDao`/`FolderDao` REPLACE безопасен только потому, что на заметки никто не ссылается,
     * а папки через upsert сейчас только создаются — не копируй тот паттерн сюда бездумно.
     */
    @Upsert
    suspend fun upsertRequest(request: PrayerRequest): Long

    @Delete
    suspend fun deleteRequest(request: PrayerRequest)

    /** Свежие дописки сверху — так их показывает карточка просьбы. */
    @Query("SELECT * FROM prayer_entries WHERE requestId = :requestId ORDER BY date DESC")
    fun observeEntries(requestId: Long): Flow<List<PrayerEntry>>

    /** `@Upsert`, а не REPLACE — единообразие с `upsertRequest`; см. комментарий там. */
    @Upsert
    suspend fun upsertEntry(entry: PrayerEntry): Long

    @Delete
    suspend fun deleteEntry(entry: PrayerEntry)
}
