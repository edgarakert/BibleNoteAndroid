package ru.edgarakert.biblenote.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.edgarakert.biblenote.data.PrayerRepository

/**
 * TDD-доказательство ловушки REPLACE + FK CASCADE (см. поправку к задаче 14.2):
 * `@Insert(onConflict = REPLACE)` на `upsertRequest` выполняет DELETE+INSERT для уже
 * существующей строки, а `prayer_entries.requestId` объявлен с `ON DELETE CASCADE` —
 * значит любое обновление просьбы молча стирает все её дописки.
 */
@RunWith(AndroidJUnit4::class)
class PrayerDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PrayerDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.prayerDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun updatingARequestKeepsItsEntries() = runBlocking {
        val requestId = dao.upsertRequest(PrayerRequest(title = "Здоровье мамы"))

        dao.upsertEntry(PrayerEntry(requestId = requestId, text = "Молился утром"))
        dao.upsertEntry(PrayerEntry(requestId = requestId, text = "Молился вечером"))

        val beforeUpdate = dao.observeEntries(requestId).first()
        assertEquals(2, beforeUpdate.size)

        val existing = dao.getRequestById(requestId)!!
        dao.upsertRequest(existing.copy(title = "Здоровье мамы — обновлено"))

        val afterUpdate = dao.observeEntries(requestId).first()
        assertEquals(2, afterUpdate.size)
    }

    @Test
    fun deletingARequestCascadesToItsEntries() = runBlocking {
        val requestId = dao.upsertRequest(PrayerRequest(title = "Работа"))
        dao.upsertEntry(PrayerEntry(requestId = requestId, text = "Дописка 1"))
        dao.upsertEntry(PrayerEntry(requestId = requestId, text = "Дописка 2"))

        assertEquals(2, dao.observeEntries(requestId).first().size)

        val request = dao.getRequestById(requestId)!!
        dao.deleteRequest(request)

        assertEquals(0, dao.observeEntries(requestId).first().size)
    }

    /**
     * `@Upsert` возвращает -1L, а не id, когда конфликт по PK привёл к UPDATE (проверено по
     * исходникам `androidx.room.EntityUpsertAdapter.upsertAndReturnId` в Room 2.8.4: "returns the
     * row id. If the insertion failed, update the existing entity and return -1L"). Тест бьёт по
     * `PrayerRepository`, а не по DAO напрямую — именно репозиторий обязан подставить настоящий id.
     */
    @Test
    fun upsertReturnsTheRealIdOnUpdate() = runBlocking {
        val repository = PrayerRepository(dao)

        val insertedId = repository.saveRequest(PrayerRequest(title = "Учёба"))
        assertNotEquals(-1L, insertedId)

        val existing = repository.getRequestById(insertedId)!!
        val updatedId = repository.saveRequest(existing.copy(title = "Учёба — обновлено"))

        assertEquals(insertedId, updatedId)
    }
}
