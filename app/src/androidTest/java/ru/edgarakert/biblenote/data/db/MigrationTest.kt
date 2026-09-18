package ru.edgarakert.biblenote.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsExistingNotesAndDefaultsIsPinnedToFalse() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO notes (id, title, content, folderId, createdAt, updatedAt) " +
                    "VALUES (1, 'Старая заметка', 'Быт 1:1', NULL, 1000, 2000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT title, isPinned FROM notes WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Старая заметка", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    /**
     * У notes есть внешний ключ на folders. ALTER TABLE ADD COLUMN его не трогает,
     * но именно на таблицах с FK автомиграции ломаются тише всего, а впереди ещё
     * три bump'а схемы (фазы 14 и 16) — проверяем связь явно.
     */
    @Test
    fun migrate1To2_preservesFoldersAndTheNoteToFolderLink() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO folders (id, name, parentId, createdAt) " +
                    "VALUES (7, 'Проповеди', NULL, 500)"
            )
            execSQL(
                "INSERT INTO notes (id, title, content, folderId, createdAt, updatedAt) " +
                    "VALUES (2, 'В папке', 'Ин 3:16', 7, 1000, 2000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT name FROM folders WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Проповеди", cursor.getString(0))
        }
        db.query("SELECT folderId, isPinned FROM notes WHERE id = 2").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    @Test
    fun migrate2To3_addsPrayerTablesAndKeepsNotes() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO notes (id, title, content, folderId, createdAt, updatedAt, isPinned) " +
                    "VALUES (1, 'Заметка', 'текст', NULL, 1000, 2000, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true)

        db.query("SELECT COUNT(*) FROM notes").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM prayer_requests").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM prayer_entries").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0))
        }
    }
}
