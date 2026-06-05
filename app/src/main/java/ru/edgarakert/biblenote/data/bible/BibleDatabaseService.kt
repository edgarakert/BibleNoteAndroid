package ru.edgarakert.biblenote.data.bible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BibleDatabaseService(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val DB_VERSION = 2
        private const val DB_NAME = "bible.sqlite"
        private const val VERSION_PREF_KEY = "bible.db.version"
        private const val PREFS_NAME = "biblenote_prefs"
    }

    private val db: SQLiteDatabase by lazy { openDatabase() }

    private fun openDatabase(): SQLiteDatabase {
        val dbFile = File(context.filesDir, DB_NAME)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt(VERSION_PREF_KEY, 0)

        if (storedVersion < DB_VERSION || !dbFile.exists()) {
            context.assets.open(DB_NAME).use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
            prefs.edit { putInt(VERSION_PREF_KEY, DB_VERSION) }
        }

        val database = SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE
        )
        runMigrations(database)
        return database
    }

    private fun runMigrations(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS verse_highlights (
                book_id      INTEGER NOT NULL,
                chapter      INTEGER NOT NULL,
                verse_number INTEGER NOT NULL,
                color_name   TEXT NOT NULL,
                created_at   INTEGER NOT NULL,
                PRIMARY KEY (book_id, chapter, verse_number)
            )
            """.trimIndent()
        )
    }

    // MARK: - Books

    suspend fun fetchAllBooks(translation: String): List<Book> = withContext(ioDispatcher) {
        val cursor = db.rawQuery(
            "SELECT id, name_ru, name_en, abbreviation FROM books ORDER BY id", null
        )
        cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(Book(it.getInt(0), it.getString(1), it.getString(2), it.getString(3)))
                }
            }
        }
    }

    suspend fun fetchBookName(bookId: Int, translation: String): String? = withContext(ioDispatcher) {
        val col = if (translation == "kjv" /* || translation == "niv" */) "name_en" else "name_ru"
        val cursor = db.rawQuery("SELECT $col FROM books WHERE id = ?", arrayOf(bookId.toString()))
        cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    // MARK: - Verses

    suspend fun fetchChapterCount(bookId: Int, translation: String): Int = withContext(ioDispatcher) {
        val cursor = db.rawQuery(
            "SELECT MAX(chapter) FROM verses WHERE book_id = ? AND translation = ?",
            arrayOf(bookId.toString(), translation)
        )
        cursor.use { if (it.moveToFirst()) it.getInt(0) else 1 }
    }

    suspend fun fetchVerses(reference: BibleReference, translation: String): List<Pair<Int, String>> = withContext(ioDispatcher) {
        val sb = StringBuilder(
            "SELECT verse, text FROM verses WHERE translation = ? AND book_id = ? AND chapter = ?"
        )
        val args = mutableListOf(translation, reference.bookId.toString(), reference.chapter.toString())
        when {
            reference.verseList.isNotEmpty() -> {
                val placeholders = reference.verseList.joinToString(",") { "?" }
                sb.append(" AND verse IN ($placeholders)")
                args.addAll(reference.verseList.map { it.toString() })
            }
            !reference.isWholeChapter -> {
                val start = reference.verseStart ?: 1
                val end = reference.verseEnd ?: start
                sb.append(" AND verse >= ? AND verse <= ?")
                args += listOf(start.toString(), end.toString())
            }
        }
        sb.append(" ORDER BY verse")
        val cursor = db.rawQuery(sb.toString(), args.toTypedArray())
        cursor.use {
            buildList { while (it.moveToNext()) add(Pair(it.getInt(0), it.getString(1))) }
        }
    }

    suspend fun fetchVersesWithHighlights(
        bookId: Int,
        chapter: Int,
        translation: String
    ): List<Triple<Int, String, HighlightColor?>> = withContext(ioDispatcher) {
        val cursor = db.rawQuery(
            """
            SELECT v.verse, v.text, h.color_name
            FROM verses v
            LEFT JOIN verse_highlights h
                ON h.book_id = v.book_id AND h.chapter = v.chapter AND h.verse_number = v.verse
            WHERE v.translation = ? AND v.book_id = ? AND v.chapter = ?
            ORDER BY v.verse
            """.trimIndent(),
            arrayOf(translation, bookId.toString(), chapter.toString())
        )
        cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val colorName = if (it.isNull(2)) null else it.getString(2)
                    add(Triple(it.getInt(0), it.getString(1), HighlightColor.fromName(colorName)))
                }
            }
        }
    }

    // MARK: - Highlights

    suspend fun fetchHighlights(bookId: Int, chapter: Int): Map<Int, HighlightColor> = withContext(ioDispatcher) {
        val cursor = db.rawQuery(
            "SELECT verse_number, color_name FROM verse_highlights WHERE book_id = ? AND chapter = ?",
            arrayOf(bookId.toString(), chapter.toString())
        )
        cursor.use {
            buildMap {
                while (it.moveToNext()) {
                    val color = HighlightColor.fromName(it.getString(1)) ?: continue
                    put(it.getInt(0), color)
                }
            }
        }
    }

    suspend fun saveHighlights(color: HighlightColor, bookId: Int, chapter: Int, verseNumbers: Set<Int>) = withContext(ioDispatcher) {
        if (verseNumbers.isEmpty()) return@withContext
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (verseNum in verseNumbers) {
                db.execSQL(
                    "INSERT OR REPLACE INTO verse_highlights (book_id, chapter, verse_number, color_name, created_at) VALUES (?,?,?,?,?)",
                    arrayOf<Any>(bookId, chapter, verseNum, color.colorName, now)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun deleteHighlights(bookId: Int, chapter: Int, verseNumbers: Set<Int>) = withContext(ioDispatcher) {
        if (verseNumbers.isEmpty()) return@withContext
        val placeholders = verseNumbers.joinToString(",") { "?" }
        val args = (listOf(bookId.toString(), chapter.toString()) +
                verseNumbers.map { it.toString() }).toTypedArray()
        db.execSQL(
            "DELETE FROM verse_highlights WHERE book_id = ? AND chapter = ? AND verse_number IN ($placeholders)",
            args
        )
    }
}
