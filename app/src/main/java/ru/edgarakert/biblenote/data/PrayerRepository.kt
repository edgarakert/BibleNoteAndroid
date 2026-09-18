package ru.edgarakert.biblenote.data

import kotlinx.coroutines.flow.Flow
import ru.edgarakert.biblenote.data.db.PrayerDao
import ru.edgarakert.biblenote.data.db.PrayerEntry
import ru.edgarakert.biblenote.data.db.PrayerRequest

class PrayerRepository(
    private val prayerDao: PrayerDao
) {
    fun observeAllRequests(): Flow<List<PrayerRequest>> = prayerDao.observeAllRequests()

    fun observeRequestById(id: Long): Flow<PrayerRequest?> = prayerDao.observeRequestById(id)

    suspend fun getRequestById(id: Long): PrayerRequest? = prayerDao.getRequestById(id)

    /**
     * `PrayerDao.upsertRequest` использует `@Upsert`, который возвращает `-1L` при обновлении
     * уже существующей строки (id в БД совпал) — сам id при этом никуда не делся, просто Room
     * не пересообщает его тем же способом, что при вставке. Без этой подстановки код, который
     * после сохранения переходит на экран просьбы по id, получал бы `-1` вместо настоящего id.
     */
    suspend fun saveRequest(request: PrayerRequest): Long {
        val rowId = prayerDao.upsertRequest(request)
        return if (rowId == -1L) request.id else rowId
    }

    suspend fun deleteRequest(request: PrayerRequest) = prayerDao.deleteRequest(request)

    fun observeEntries(requestId: Long): Flow<List<PrayerEntry>> = prayerDao.observeEntries(requestId)

    /** См. `saveRequest` — та же ловушка `@Upsert`, возвращающего `-1L` при обновлении. */
    suspend fun saveEntry(entry: PrayerEntry): Long {
        val rowId = prayerDao.upsertEntry(entry)
        return if (rowId == -1L) entry.id else rowId
    }

    suspend fun deleteEntry(entry: PrayerEntry) = prayerDao.deleteEntry(entry)
}
