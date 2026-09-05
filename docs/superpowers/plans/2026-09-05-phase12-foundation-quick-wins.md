# Фаза 12 — Фундамент и быстрые победы

> **For agentic workers:** REQUIRED SUB-SKILL: используйте `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, чтобы выполнять этот план задача-за-задачей. Шаги размечены чекбоксами (`- [ ]`).

**Goal:** Закрыть четыре самостоятельные iOS-фичи (multiline-заголовок, порядок книг Синодального, закрепление заметок, запоминание последней папки/заметки) и заложить два фундамента, без которых фазы 13 и 14 не стартуют: хелперы `BibleReference` и рабочие миграции Room.

**Architecture:** Задачи 12.1 и 12.2 — чистый фундамент и идут первыми. 12.1 добавляет в существующий `BibleReference` три чистые функции (покрытые ссылкой стихи, форматирование списка стихов с настраиваемым разделителем, пересборка текста ссылки) — они нужны и счётчику упоминаний, и правке ссылки из шторки, и сохранению стихов в заметку. 12.2 переводит `AppDatabase` на автомиграции, потому что сейчас любой bump версии роняет установленные приложения. Остальные задачи независимы друг от друга и могут выполняться в любом порядке.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.05.01), Room 2.8.4 + KSP, Koin 4.2.1, Navigation Compose 2.9.8, DataStore Preferences 1.2.1, JUnit 4.13.2.

**Порядок:** 12.1 → 12.2 → (12.3 → 12.4) → 12.5 → 12.6 → (12.8 → 12.7)

---

## Структура файлов

**Создаются:**
- `app/src/test/java/ru/edgarakert/biblenote/data/bible/BibleReferenceFormattingTest.kt` — тесты хелперов ссылок
- `app/src/test/java/ru/edgarakert/biblenote/data/bible/SynodalBookOrderTest.kt` — тест порядка книг НЗ
- `app/src/test/java/ru/edgarakert/biblenote/data/settings/NotesPathCodecTest.kt` — тесты кодека пути навигации
- `app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt` — тест миграции Room 1→2
- `app/src/main/java/ru/edgarakert/biblenote/data/settings/NotesPathCodec.kt` — кодирование пути навигации в строку
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteListSection.kt` — общая секция списка заметок для двух экранов

**Изменяются:**
- `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleReference.kt` — `coveredVerses`, `formatVerseSpec`, `replacementText`
- `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleDatabaseService.kt` — порядок книг для `synodal`
- `app/src/main/java/ru/edgarakert/biblenote/data/db/AppDatabase.kt` — версия 2 + автомиграция
- `app/src/main/java/ru/edgarakert/biblenote/data/db/Note.kt` — колонка `isPinned`
- `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt` — `setPinned`
- `app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt` — `setNotePinned`
- `app/src/main/java/ru/edgarakert/biblenote/data/settings/SettingsRepository.kt` — ключ `notes.lastPath`
- `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NotesViewModel.kt` — разделение на закреплённые/обычные, `togglePin`
- `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/FolderViewModel.kt` — то же
- `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NoteEditorViewModel.kt` — фикс воскрешения заметки
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteRow.kt` — иконка закрепления
- `app/src/main/java/ru/edgarakert/biblenote/ui/screens/notes/NotesListScreen.kt` — секции
- `app/src/main/java/ru/edgarakert/biblenote/ui/screens/notes/FolderScreen.kt` — секции
- `app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt` — multiline-заголовок
- `app/src/main/java/ru/edgarakert/biblenote/MainActivity.kt` — синхронное чтение пути
- `app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt` — восстановление back stack
- `app/build.gradle.kts` — `room-testing`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml`

---

## Задача 12.1: Хелперы `BibleReference`

Три чистые функции. `coveredVerses` нужен счётчику упоминаний (13.2) и предвыбору стихов в шторке (13.4). `formatVerseSpec` с параметром разделителя закрывает риск **R2**: в текст заметки пишется ASCII-дефис (иначе парсер не прочитает написанное обратно), в заголовки — en dash. `replacementText` пересобирает текст ссылки, сохраняя сокращение ровно так, как его набрал пользователь.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleReference.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/bible/BibleReferenceFormattingTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

Создать `app/src/test/java/ru/edgarakert/biblenote/data/bible/BibleReferenceFormattingTest.kt`:

```kotlin
package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleReferenceFormattingTest {

    private fun ref(
        bookId: Int = 1,
        chapter: Int = 2,
        verseStart: Int? = null,
        verseEnd: Int? = null,
        verseList: List<Int> = emptyList(),
        displayText: String = "Быт 2"
    ) = BibleReference(bookId, chapter, verseStart, verseEnd, verseList, displayText, 0, displayText.length)

    // ── formatVerseSpec ────────────────────────────────────────

    @Test
    fun `formatVerseSpec of empty list is empty string`() {
        assertEquals("", BibleReference.formatVerseSpec(emptyList()))
    }

    @Test
    fun `formatVerseSpec of single verse is bare number`() {
        assertEquals("14", BibleReference.formatVerseSpec(listOf(14)))
    }

    @Test
    fun `formatVerseSpec collapses a consecutive run`() {
        assertEquals("14-18", BibleReference.formatVerseSpec(listOf(14, 15, 16, 17, 18)))
    }

    @Test
    fun `formatVerseSpec mixes a run and a single`() {
        assertEquals("14-16,20", BibleReference.formatVerseSpec(listOf(14, 15, 16, 20)))
    }

    @Test
    fun `formatVerseSpec sorts unsorted input`() {
        assertEquals("14-15,18", BibleReference.formatVerseSpec(listOf(18, 14, 15)))
    }

    @Test
    fun `formatVerseSpec deduplicates`() {
        assertEquals("14-15", BibleReference.formatVerseSpec(listOf(14, 14, 15)))
    }

    @Test
    fun `formatVerseSpec honours a custom separator`() {
        assertEquals("14–15", BibleReference.formatVerseSpec(listOf(14, 15), rangeSeparator = "–"))
    }

    @Test
    fun `formatVerseSpec defaults to an ASCII hyphen so the parser can read it back`() {
        val spec = BibleReference.formatVerseSpec(listOf(14, 15, 16))
        assertTrue(spec.contains('-'))
        assertFalse(spec.contains('–'))
    }

    // ── coveredVerses ──────────────────────────────────────────

    @Test
    fun `coveredVerses of a whole chapter is empty`() {
        assertEquals(emptyList<Int>(), ref().coveredVerses)
    }

    @Test
    fun `coveredVerses of a single verse is that verse`() {
        assertEquals(listOf(14), ref(verseStart = 14).coveredVerses)
    }

    @Test
    fun `coveredVerses of a range expands it`() {
        assertEquals(listOf(14, 15, 16), ref(verseStart = 14, verseEnd = 16).coveredVerses)
    }

    @Test
    fun `coveredVerses prefers verseList over the stale range fields`() {
        // Парсер для "Быт 2:14-15,20" заполняет и verseStart/verseEnd (только первый сегмент),
        // и verseList (полный набор). Авторитетен verseList.
        val r = ref(verseStart = 14, verseEnd = 15, verseList = listOf(14, 15, 20))
        assertEquals(listOf(14, 15, 20), r.coveredVerses)
    }

    // ── replacementText ────────────────────────────────────────

    @Test
    fun `replacementText adds a spec to a whole-chapter reference`() {
        assertEquals("Быт 2:14-16", BibleReference.replacementText("Быт 2", listOf(14, 15, 16)))
    }

    @Test
    fun `replacementText replaces an existing spec`() {
        assertEquals("Быт 2:14", BibleReference.replacementText("Быт 2:5", listOf(14)))
    }

    @Test
    fun `replacementText with an empty selection degrades to the whole chapter`() {
        assertEquals("Быт 2", BibleReference.replacementText("Быт 2:14-18", emptyList()))
    }

    @Test
    fun `replacementText preserves the abbreviation exactly as typed`() {
        assertEquals("быт.3:7", BibleReference.replacementText("быт.3:2", listOf(7)))
    }

    @Test
    fun `replacementText survives a numeric book prefix`() {
        assertEquals("1 Кор 13:4-8", BibleReference.replacementText("1 Кор 13:4-7", listOf(4, 5, 6, 7, 8)))
    }

    // ── round trip through the real parser ─────────────────────

    @Test
    fun `replacement text re-parses to the same verses`() {
        val written = BibleReference.replacementText("Быт 2", listOf(14, 15, 16, 20))
        val parsed = BibleReferenceParser().parse(written)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed[0].bookId)
        assertEquals(2, parsed[0].chapter)
        assertEquals(listOf(14, 15, 16, 20), parsed[0].coveredVerses)
    }

    @Test
    fun `an emptied selection re-parses as a whole chapter`() {
        val written = BibleReference.replacementText("Быт 2:14-18", emptyList())
        val parsed = BibleReferenceParser().parse(written)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].isWholeChapter)
        assertEquals(emptyList<Int>(), parsed[0].coveredVerses)
    }
}
```

Добавить импорт `import org.junit.Assert.assertFalse` в начало файла вместе с остальными.

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*BibleReferenceFormattingTest*"`
Expected: FAIL — компиляция не проходит, `Unresolved reference: formatVerseSpec`, `Unresolved reference: coveredVerses`, `Unresolved reference: replacementText`.

- [ ] **Шаг 3: Написать минимальную реализацию**

В `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleReference.kt` добавить вычисляемое свойство в тело класса:

```kotlin
    /**
     * Номера стихов, которые покрывает ссылка.
     * Ссылка на всю главу возвращает пустой список (номера стихов главы ей неизвестны) —
     * потребитель, которому нужны все стихи главы, подставляет их сам.
     * verseList авторитетен: для "Быт 2:14-15,20" парсер заполняет verseStart/verseEnd
     * только первым сегментом, а полный набор кладёт в verseList.
     */
    val coveredVerses: List<Int>
        get() = when {
            verseList.isNotEmpty() -> verseList.sorted()
            verseStart == null -> emptyList()
            else -> (verseStart..(verseEnd ?: verseStart)).toList()
        }
```

И в `companion object` — заменить существующий `compactVerseString` на пару функций:

```kotlin
        /**
         * Собирает список номеров стихов в компактную запись: "14-16,20".
         *
         * rangeSeparator по умолчанию — ASCII-дефис (U+002D): это единственный разделитель,
         * который принимает BibleReferenceParser, поэтому всё, что пишется в текст заметки,
         * должно использовать его. En dash (U+2013) допустим только в заголовках на экране.
         */
        fun formatVerseSpec(verses: List<Int>, rangeSeparator: String = "-"): String {
            val sorted = verses.toSortedSet().toList()
            if (sorted.isEmpty()) return ""

            val parts = mutableListOf<String>()
            var runStart = sorted.first()
            var runEnd = runStart

            for (verse in sorted.drop(1)) {
                if (verse == runEnd + 1) {
                    runEnd = verse
                } else {
                    parts += formatRun(runStart, runEnd, rangeSeparator)
                    runStart = verse
                    runEnd = verse
                }
            }
            parts += formatRun(runStart, runEnd, rangeSeparator)
            return parts.joinToString(",")
        }

        private fun formatRun(start: Int, end: Int, separator: String): String =
            if (start == end) "$start" else "$start$separator$end"

        /** Запись для показа на экране — с en dash. */
        fun compactVerseString(verses: List<Int>): String =
            formatVerseSpec(verses, rangeSeparator = "–")

        /**
         * Пересобирает текст ссылки под новый набор стихов, сохраняя сокращение книги
         * ровно так, как его набрал пользователь ("быт.3:2" → "быт.3:7").
         * Пустой набор схлопывает ссылку до главы целиком.
         */
        fun replacementText(displayText: String, verses: List<Int>): String {
            val head = displayText.substringBefore(':')
            val spec = formatVerseSpec(verses)
            return if (spec.isEmpty()) head else "$head:$spec"
        }
```

- [ ] **Шаг 4: Запустить тесты и убедиться, что они проходят**

Run: `./gradlew testDebugUnitTest --tests "*BibleReferenceFormattingTest*" --tests "*BibleReferenceParserTest*"`
Expected: PASS — оба класса зелёные. `BibleReferenceParserTest` запускается вместе, потому что он проверяет `compactVerseString` и не должен сломаться от рефакторинга.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleReference.kt app/src/test/java/ru/edgarakert/biblenote/data/bible/BibleReferenceFormattingTest.kt
git commit -m "feat: add coveredVerses, formatVerseSpec and replacementText to BibleReference"
```

---

## Задача 12.2: Миграции Room

Закрывает риск **R1**. `AppDatabase.create()` сейчас — `Room.databaseBuilder(...).build()` без `addMigrations` и без `fallbackToDestructiveMigration`. Версия 1 в проде; следующие две фазы обе меняют схему. Автомиграции возможны сразу: `exportSchema = true` уже включён, а `app/schemas/ru.edgarakert.biblenote.data.db.AppDatabase/1.json` закоммичен.

Эта задача поднимает версию до 2 **вместе с колонкой `isPinned`** из задачи 12.3 — отдельный bump версии ради пустой миграции не нужен.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/Note.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/AppDatabase.kt`
- Modify: `app/build.gradle.kts`
- Test: `app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt`

- [ ] **Шаг 1: Добавить зависимость для тестов миграций**

В `app/build.gradle.kts`, в блок `dependencies`:

```kotlin
    androidTestImplementation(libs.androidx.room.testing)
```

В `gradle/libs.versions.toml`, в секцию `[libraries]`:

```toml
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

- [ ] **Шаг 2: Написать падающий тест миграции**

Создать `app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt`:

```kotlin
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
}
```

- [ ] **Шаг 3: Запустить тест и убедиться, что он падает**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: FAIL — `Cannot find the schema file in the assets folder` либо `Migration didn't properly handle: notes`, потому что версия базы всё ещё 1 и колонки `isPinned` нет.

Если запуск падает с `no schema files`, добавить в `app/build.gradle.kts` в блок `android`:

```kotlin
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
```

- [ ] **Шаг 4: Добавить колонку и автомиграцию**

В `app/src/main/java/ru/edgarakert/biblenote/data/db/Note.kt` — добавить поле в `data class Note` последним:

```kotlin
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false
```

и импорт `import androidx.room.ColumnInfo`.

`defaultValue = "0"` обязателен: без него Room не сможет сгенерировать автомиграцию для существующих строк.

В `app/src/main/java/ru/edgarakert/biblenote/data/db/AppDatabase.kt` заменить аннотацию:

```kotlin
@Database(
    entities = [Note::class, Folder::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDatabase : RoomDatabase() {
```

и импорт `import androidx.room.AutoMigration`.

- [ ] **Шаг 5: Запустить тест и убедиться, что он проходит**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: PASS. В `app/schemas/ru.edgarakert.biblenote.data.db.AppDatabase/` появится `2.json` — его нужно закоммитить.

- [ ] **Шаг 6: Коммит**

```bash
git add app/build.gradle.kts gradle/libs.versions.toml app/src/main/java/ru/edgarakert/biblenote/data/db/ app/schemas/ app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt
git commit -m "feat: add Room auto-migration 1 to 2 with notes.isPinned column"
```

---

## Задача 12.3: Слой данных для закрепления заметок

Закрепление **не должно менять `updatedAt`** — иначе заметка прыгает наверх по свежести. Поэтому нужен отдельный точечный `UPDATE`, а не `upsert` целой сущности.

Порядок сортировки в DAO не меняем: iOS разделяет заметки на закреплённые и обычные **в коде**, а не в `ORDER BY`, и рендерит их двумя секциями. Внутри каждой секции порядок прежний — `updatedAt DESC`.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt`

- [ ] **Шаг 1: Добавить запрос в DAO**

В `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt`:

```kotlin
    /** Точечное обновление: закрепление не трогает updatedAt, чтобы не менять порядок по свежести. */
    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)
```

- [ ] **Шаг 2: Прокинуть через репозиторий**

В `app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt`:

```kotlin
    suspend fun setNotePinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)
```

- [ ] **Шаг 3: Собрать**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt
git commit -m "feat: add setNotePinned to the notes data layer"
```

---

## Задача 12.4: Секция закреплённых заметок в UI

Закрывает риски **R6** (дублирование двух экранов) и **R7** (у заметок нет жеста для закрепления).

iOS рендерит закреплённые **отдельной секцией сверху**, а не пересортированным единым списком — пересортировка внутри одной секции давала артефакт анимации «схлопнулось и появилось заново». На Compose это значит два блока `items()` со стабильными ключами.

Поиск не разделяется на секции: пока строка поиска не пуста, показывается один плоский список.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteListSection.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteRow.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NotesViewModel.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/FolderViewModel.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/notes/NotesListScreen.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/notes/FolderScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml`

- [ ] **Шаг 1: Добавить строки в обе локали**

`app/src/main/res/values/strings.xml`:

```xml
    <string name="notes_pin">Pin</string>
    <string name="notes_unpin">Unpin</string>
```

`app/src/main/res/values-ru/strings.xml`:

```xml
    <string name="notes_pin">Закрепить</string>
    <string name="notes_unpin">Открепить</string>
```

- [ ] **Шаг 2: Показать метку закрепления в `NoteRow`**

В `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteRow.kt` — обернуть заголовок в `Row` и добавить иконку перед ним (в iOS это `pin.fill`, 11 pt, amber):

```kotlin
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    if (note.isPinned) {
        Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(11.dp)
        )
    }
    Text(
        text = note.title.ifEmpty { stringResource(R.string.notes_untitled) },
        // остальные параметры не меняются
    )
}
```

Импорты: `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.PushPin`, `androidx.compose.material3.Icon`, `androidx.compose.foundation.layout.Arrangement`, `androidx.compose.foundation.layout.size`.

- [ ] **Шаг 3: Разделить заметки во ViewModel**

В `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NotesViewModel.kt` добавить производные потоки и действие. Разделение — в коде, порядок внутри каждой группы приходит из SQL (`updatedAt DESC`):

```kotlin
    val pinnedNotes: StateFlow<List<Note>> = rootNotes
        .map { notes -> notes.filter { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unpinnedNotes: StateFlow<List<Note>> = rootNotes
        .map { notes -> notes.filterNot { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setNotePinned(note.id, !note.isPinned) }
    }
```

Импорты: `kotlinx.coroutines.flow.map`, `kotlinx.coroutines.flow.stateIn`, `kotlinx.coroutines.flow.SharingStarted`.

То же самое добавить в `FolderViewModel.kt`, но производя потоки от `notes` вместо `rootNotes`.

- [ ] **Шаг 4: Вынести общую секцию списка**

Создать `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteListSection.kt` — один `LazyListScope`-расширитель, который используют оба экрана. Это устраняет дублирование (риск R6) и даёт закреплению один жест на оба экрана:

```kotlin
package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.Note

/**
 * Одна секция списка заметок. Закреплённые и обычные заметки рендерятся двумя вызовами
 * этой функции, а не одним пересортированным списком: пересортировка внутри одной секции
 * даёт артефакт анимации при закреплении (в iOS это же решение принято по той же причине).
 *
 * keyPrefix обязателен и должен различаться у секций, иначе ключи столкнутся.
 */
fun LazyListScope.noteListSection(
    notes: List<Note>,
    keyPrefix: String,
    isSelectMode: Boolean,
    selectedIds: Set<Long>,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onMove: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    items(notes, key = { "${keyPrefix}_${it.id}" }) { note ->
        var menuOpen by remember { mutableStateOf(false) }

        Box {
            NoteRow(
                note = note,
                isSelectMode = isSelectMode,
                isSelected = note.id in selectedIds,
                modifier = if (isSelectMode) {
                    Modifier.pointerInput(note.id) {
                        detectTapGestures(onTap = { onNoteClick(note) })
                    }
                } else {
                    Modifier.pointerInput(note.id) {
                        detectTapGestures(
                            onTap = { onNoteClick(note) },
                            onLongPress = { menuOpen = true }
                        )
                    }
                }
            )

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (note.isPinned) R.string.notes_unpin else R.string.notes_pin
                            )
                        )
                    },
                    onClick = { menuOpen = false; onTogglePin(note) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_move)) },
                    onClick = { menuOpen = false; onMove(note) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.note_delete)) },
                    onClick = { menuOpen = false; onDelete(note) }
                )
            }
        }
    }
}
```

Импорт `androidx.compose.foundation.lazy.items` добавить туда же.

- [ ] **Шаг 5: Использовать секции на обоих экранах**

В `NotesListScreen.kt` заменить единый блок `items(displayNotes, ...)` на:

```kotlin
if (isSearchActive) {
    noteListSection(
        notes = displayNotes, keyPrefix = "search",
        isSelectMode = isSelectMode, selectedIds = selectedIds,
        onNoteClick = ..., onTogglePin = viewModel::togglePin,
        onMove = ..., onDelete = ...
    )
} else {
    noteListSection(
        notes = pinnedNotes, keyPrefix = "pinned",
        isSelectMode = isSelectMode, selectedIds = selectedIds,
        onNoteClick = ..., onTogglePin = viewModel::togglePin,
        onMove = ..., onDelete = ...
    )
    noteListSection(
        notes = unpinnedNotes, keyPrefix = "note",
        isSelectMode = isSelectMode, selectedIds = selectedIds,
        onNoteClick = ..., onTogglePin = viewModel::togglePin,
        onMove = ..., onDelete = ...
    )
}
```

Свайп влево (удаление) сохраняется как есть внутри `NoteRow`-обёртки. В iOS для закрепления используется свайп вправо; на Android слот `enableDismissFromStartToEnd` сейчас `false` — при желании его можно занять закреплением, но long-press-меню обязательно, потому что оно единственный дискаверабельный путь.

Ту же замену сделать в `FolderScreen.kt`, используя `pinnedNotes`/`unpinnedNotes` из `FolderViewModel`.

- [ ] **Шаг 6: Собрать и проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL. Вручную: долгий тап по заметке → «Закрепить» → заметка уходит в верхнюю секцию с иконкой булавки, **её дата не меняется**; долгий тап → «Открепить» → возвращается на своё место по свежести.

- [ ] **Шаг 7: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/ app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
git commit -m "feat: pin notes into their own section above the rest"
```

---

## Задача 12.5: Multiline-заголовок заметки

iOS: заголовок был однострочным полем со шрифтом `display` (system `.title`, bold); стал вертикально растущим полем `lineLimit(1...4)` со шрифтом `titleLarge` (system `.title2`, semibold) — то есть на шаг мельче.

Сейчас в Android заголовок — `BasicTextField` с serif, bold, 22sp. Нужно снять однострочность, поставить лимит 4 строки и уменьшить начертание до semibold.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt:145`

- [ ] **Шаг 1: Изменить поле заголовка**

В `NoteEditorScreen.kt` в `BasicTextField` заголовка:

```kotlin
BasicTextField(
    value = title,
    onValueChange = viewModel::onTitleChanged,
    textStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = MaterialTheme.colorScheme.onBackground
    ),
    singleLine = false,
    maxLines = 4,
    keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next
    ),
    // decorationBox не меняется
)
```

Ключевые изменения: `fontWeight` `Bold` → `SemiBold`, добавлены `singleLine = false` и `maxLines = 4`.

- [ ] **Шаг 2: Собрать и проверить**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL. Вручную: длинный заголовок переносится, растёт до четырёх строк, дальше поле скроллится внутри себя, а не растягивает экран.

- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt
git commit -m "fix: let the note title wrap up to four lines"
```

---

## Задача 12.6: Православный порядок книг НЗ для Синодального перевода

Пикер книг сортирует строго по `id`, а это западный протестантский канон (Деян → Рим → … → Евр → Иак → …). В русской синодальной традиции соборные послания (Иак, 1–2 Пет, 1–3 Ин, Иуд) идут сразу после Деяний, до посланий Павла.

Меняется **только порядок показа**. `book_id`, запросы и ссылки не трогаются.

Решение Q4: порядок применяется **только к `synodal`**. Русский перевод в приложении один, поэтому буквальная проверка `translation == "synodal"` — точная, а не приближение.

`fetchAllBooks(translation)` сейчас принимает `translation` и **полностью его игнорирует** — эта задача впервые даёт параметру смысл.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleDatabaseService.kt:123`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/bible/SynodalBookOrderTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

Тест проверяет чистую функцию упорядочивания, а не обращение к SQLite — так он остаётся pure-JVM.

Создать `app/src/test/java/ru/edgarakert/biblenote/data/bible/SynodalBookOrderTest.kt`:

```kotlin
package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Test

class SynodalBookOrderTest {

    private val allBooks: List<Book> = (1..66).map { id ->
        Book(id = id, nameRu = "Книга $id", nameEn = "Book $id", abbreviation = "B$id")
    }

    @Test
    fun `synodal puts the general epistles right after Acts`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        val newTestament = ordered.filter { it.id >= 40 }.map { it.id }

        assertEquals(
            listOf(
                40, 41, 42, 43, 44,
                59, 60, 61, 62, 63, 64, 65,
                45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                58,
                66
            ),
            newTestament
        )
    }

    @Test
    fun `synodal leaves the old testament in id order`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        assertEquals((1..39).toList(), ordered.filter { it.id <= 39 }.map { it.id })
    }

    @Test
    fun `kjv keeps the western canonical order`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "kjv")
        assertEquals((1..66).toList(), ordered.map { it.id })
    }

    @Test
    fun `ordering never drops or duplicates a book`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        assertEquals(66, ordered.size)
        assertEquals(66, ordered.map { it.id }.toSet().size)
    }
}
```

Если конструктор `Book` в проекте отличается — привести вызов к фактической сигнатуре из `data/bible/Book.kt`, остальное не меняется.

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*SynodalBookOrderTest*"`
Expected: FAIL — `Unresolved reference: applyBookOrder`.

- [ ] **Шаг 3: Реализовать порядок**

В `app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleDatabaseService.kt`, в `companion object`:

```kotlin
        /**
         * Порядок чтения Нового Завета в русской синодальной (православной/славянской) традиции:
         * соборные послания (Иак…Иуд) идут после Деяний, до посланий Павла.
         * Английские переводы сохраняют западный канонический порядок (по book_id).
         */
        private val SYNODAL_NT_ORDER = listOf(
            40, 41, 42, 43, 44,                                     // Матфея–Деяния
            59, 60, 61, 62, 63, 64, 65,                             // Иакова, 1–2 Петра, 1–3 Иоанна, Иуды
            45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,     // Римлянам–Филимону
            58,                                                      // Евреям
            66                                                       // Откровение
        )

        /** Меняет только порядок показа: book_id и ссылки не затрагиваются. */
        fun applyBookOrder(books: List<Book>, translation: String): List<Book> {
            if (translation != "synodal") return books
            val byId = books.associateBy { it.id }
            val oldTestament = books.filter { it.id <= 39 }
            val newTestament = SYNODAL_NT_ORDER.mapNotNull { byId[it] }
            return oldTestament + newTestament
        }
```

И применить в `fetchAllBooks`, обернув текущий результат запроса:

```kotlin
    suspend fun fetchAllBooks(translation: String): List<Book> = withContext(ioDispatcher) {
        val books = /* существующее чтение курсора, без изменений */
        applyBookOrder(books, translation)
    }
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*SynodalBookOrderTest*"`
Expected: PASS, 4 теста.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/bible/BibleDatabaseService.kt app/src/test/java/ru/edgarakert/biblenote/data/bible/SynodalBookOrderTest.kt
git commit -m "fix: use Orthodox New Testament book order for the Synodal translation"
```

---

## Задача 12.7: Запоминание последней открытой папки/заметки

iOS хранит **типизированный путь целиком** (`Root → Папка → Подпапка → Заметка`), а не один id, и восстанавливает его **синхронно, до первого кадра** — восстановление из `onAppear` даёт видимую вспышку корневого экрана.

На Android эквивалент: прочитать путь из DataStore блокирующе **один раз** в `MainActivity` до `setContent`, и посеять back stack в `LaunchedEffect(Unit)` — композиция происходит до отрисовки, поэтому промежуточный корневой экран не успевает появиться.

Восстановление обязано быть устойчивым к битым id (риск **R3**): заметка могла быть удалена свайпом или автоудалена как пустая. Как в iOS, на первом неразрешимом элементе восстановление останавливается, **сохраняя уже восстановленный префикс** — удалённая заметка не стирает весь путь, пользователь попадает в её папку.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/settings/NotesPathCodec.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/settings/NotesPathCodecTest.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/settings/SettingsRepository.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/MainActivity.kt`

- [ ] **Шаг 1: Написать падающий тест кодека**

`kotlinx.serialization` в проекте нет, и ради одного списка её тянуть не стоит — берём собственную строковую кодировку и покрываем её тестами.

Создать `app/src/test/java/ru/edgarakert/biblenote/data/settings/NotesPathCodecTest.kt`:

```kotlin
package ru.edgarakert.biblenote.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesPathCodecTest {

    @Test
    fun `empty path encodes to an empty string`() {
        assertEquals("", NotesPathCodec.encode(emptyList()))
    }

    @Test
    fun `empty string decodes to an empty path`() {
        assertEquals(emptyList<NotesPathEntry>(), NotesPathCodec.decode(""))
    }

    @Test
    fun `a folder then a note round trips`() {
        val path = listOf(NotesPathEntry.Folder(12), NotesPathEntry.Note(34))
        assertEquals(path, NotesPathCodec.decode(NotesPathCodec.encode(path)))
    }

    @Test
    fun `a nested folder path round trips in order`() {
        val path = listOf(
            NotesPathEntry.Folder(1),
            NotesPathEntry.Folder(2),
            NotesPathEntry.Note(3)
        )
        assertEquals("f:1,f:2,n:3", NotesPathCodec.encode(path))
        assertEquals(path, NotesPathCodec.decode("f:1,f:2,n:3"))
    }

    @Test
    fun `malformed segments are skipped rather than throwing`() {
        assertEquals(
            listOf(NotesPathEntry.Folder(1), NotesPathEntry.Note(3)),
            NotesPathCodec.decode("f:1,garbage,x:2,n:3,f:")
        )
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*NotesPathCodecTest*"`
Expected: FAIL — `Unresolved reference: NotesPathCodec`.

- [ ] **Шаг 3: Реализовать кодек**

Создать `app/src/main/java/ru/edgarakert/biblenote/data/settings/NotesPathCodec.kt`:

```kotlin
package ru.edgarakert.biblenote.data.settings

/** Один шаг пути навигации во вкладке «Заметки». */
sealed interface NotesPathEntry {
    val id: Long

    data class Folder(override val id: Long) : NotesPathEntry
    data class Note(override val id: Long) : NotesPathEntry
}

/**
 * Кодирует путь навигации в одну строку для DataStore: "f:12,n:34".
 * Собственный формат вместо JSON — в проекте нет kotlinx.serialization,
 * а формат тривиален и полностью покрыт тестами.
 */
object NotesPathCodec {

    fun encode(path: List<NotesPathEntry>): String = path.joinToString(",") { entry ->
        when (entry) {
            is NotesPathEntry.Folder -> "f:${entry.id}"
            is NotesPathEntry.Note -> "n:${entry.id}"
        }
    }

    /** Битые сегменты пропускаются: испорченная настройка не должна ронять запуск. */
    fun decode(encoded: String): List<NotesPathEntry> =
        encoded.split(',')
            .mapNotNull { segment ->
                val parts = segment.split(':')
                if (parts.size != 2) return@mapNotNull null
                val id = parts[1].toLongOrNull() ?: return@mapNotNull null
                when (parts[0]) {
                    "f" -> NotesPathEntry.Folder(id)
                    "n" -> NotesPathEntry.Note(id)
                    else -> null
                }
            }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*NotesPathCodecTest*"`
Expected: PASS, 5 тестов.

- [ ] **Шаг 5: Добавить ключ в `SettingsRepository`**

В `app/src/main/java/ru/edgarakert/biblenote/data/settings/SettingsRepository.kt` — по образцу существующих ключей `bible.*`:

```kotlin
        private val KEY_NOTES_LAST_PATH = stringPreferencesKey("notes.lastPath")
```

```kotlin
    val notesLastPath: Flow<List<NotesPathEntry>> = dataStore.data
        .map { NotesPathCodec.decode(it[KEY_NOTES_LAST_PATH] ?: "") }

    suspend fun setNotesLastPath(path: List<NotesPathEntry>) {
        dataStore.edit { it[KEY_NOTES_LAST_PATH] = NotesPathCodec.encode(path) }
    }
```

- [ ] **Шаг 6: Писать путь при навигации**

В `app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt` завести состояние пути рядом с `navController` и обновлять его в тех же местах, где происходит переход:

```kotlin
val settingsRepository: SettingsRepository = koinInject()
val scope = rememberCoroutineScope()
var notesPath by remember { mutableStateOf(initialNotesPath) }

fun pushNotesPath(entry: NotesPathEntry) {
    notesPath = notesPath + entry
    scope.launch { settingsRepository.setNotesLastPath(notesPath) }
}

fun popNotesPathTo(depth: Int) {
    if (notesPath.size <= depth) return
    notesPath = notesPath.take(depth)
    scope.launch { settingsRepository.setNotesLastPath(notesPath) }
}
```

Вызывать `pushNotesPath(NotesPathEntry.Folder(id))` в `onNavigateToFolder` и `pushNotesPath(NotesPathEntry.Note(id))` в `onNavigateToNote`. На возврат подписаться через текущий маршрут: когда `currentBackStackEntryAsState()` показывает `notes`, вызывать `popNotesPathTo(0)`; для `folder/{folderId}` — обрезать путь до последней папки.

- [ ] **Шаг 7: Восстанавливать путь до первого кадра**

В `app/src/main/java/ru/edgarakert/biblenote/MainActivity.kt` прочитать путь блокирующе **один раз**, до `setContent`. Это единственное блокирующее чтение за запуск, и оно избавляет от вспышки корневого экрана:

```kotlin
private val settingsRepository: SettingsRepository by inject()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Синхронно, до setContent: иначе back stack достраивается уже после первого кадра
    // и пользователь видит вспышку корневого списка заметок.
    val restoredPath = runBlocking { settingsRepository.notesLastPath.first() }

    setContent {
        // ... существующая обвязка темы и онбординга
        AppNavHost(initialNotesPath = restoredPath)
    }
}
```

Импорты: `kotlinx.coroutines.runBlocking`, `kotlinx.coroutines.flow.first`, `org.koin.android.ext.android.inject`.

В `AppNavHost` добавить параметр и посев back stack. Каждый id проверяется в базе; на первом неразрешимом — стоп с сохранением префикса:

```kotlin
@Composable
fun AppNavHost(initialNotesPath: List<NotesPathEntry> = emptyList()) {
    // ...
    val repository: NoteRepository = koinInject()

    LaunchedEffect(Unit) {
        if (initialNotesPath.isEmpty()) return@LaunchedEffect
        for (entry in initialNotesPath) {
            val exists = when (entry) {
                is NotesPathEntry.Folder -> repository.observeFolderById(entry.id).first() != null
                is NotesPathEntry.Note -> repository.getNoteById(entry.id) != null
            }
            // Удалённая заметка не должна стирать весь путь: останавливаемся,
            // сохранив уже восстановленный префикс — пользователь попадёт в её папку.
            if (!exists) break

            when (entry) {
                is NotesPathEntry.Folder -> navController.navigate("folder/${entry.id}")
                is NotesPathEntry.Note -> navController.navigate("editor/${entry.id}")
            }
        }
    }
```

- [ ] **Шаг 8: Проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL.

Проверить вручную три сценария:
1. Открыть заметку в корне → закрыть приложение из списка задач → запустить → сразу открыта та же заметка, кнопка «назад» ведёт в список.
2. Открыть папку → подпапку → заметку → перезапустить → открыта заметка, «назад» проводит через подпапку и папку.
3. Открыть заметку → перезапустить → удалить заметку → перезапустить снова → приложение открывает список, а не падает.

- [ ] **Шаг 9: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/settings/ app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt app/src/main/java/ru/edgarakert/biblenote/MainActivity.kt app/src/test/java/ru/edgarakert/biblenote/data/settings/NotesPathCodecTest.kt
git commit -m "feat: restore the last opened note or folder on launch"
```

---

## Задача 12.8: Фикс воскрешения удалённой заметки

Риск **R3**, найден при аудите. `NoteEditorViewModel.deleteNote()` удаляет строку и шлёт `navigateBack`, но не сбрасывает `currentNote` и `isDirty`. Если пользователь правил заметку и удалил её внутри окна debounce 500 мс, `onCleared()` уходит в ветку `isDirty` и вызывает `saveNote` — а это `@Insert(onConflict = REPLACE)` с прежним id, то есть заметка возвращается.

Чинится до задачи 12.7, потому что воскрешение ломает восстановление пути: сохранённый `lastNoteId` начинает указывать на строку-зомби.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NoteEditorViewModel.kt`

- [ ] **Шаг 1: Сбросить состояние при удалении**

В `deleteNote()`, сразу после успешного `repository.deleteNote(note)`:

```kotlin
    fun deleteNote() {
        val note = currentNote ?: return
        viewModelScope.launch {
            repository.deleteNote(note)
            // Без сброса onCleared() увидит isDirty и пересохранит удалённую строку
            // через upsert с тем же id — заметка «воскреснет».
            currentNote = null
            isDirty = false
            _navigateBack.emit(Unit)
        }
    }
```

Имена `_navigateBack`/`isDirty` привести к фактическим в файле.

- [ ] **Шаг 2: Проверить вручную**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL. Сценарий: открыть заметку → напечатать символ → **сразу**, не дожидаясь полсекунды, удалить заметку → вернуться в список → заметки нет; перезапустить приложение → заметки по-прежнему нет.

- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/NoteEditorViewModel.kt
git commit -m "fix: stop a deleted note from being resurrected by the debounced autosave"
```

---

## Завершение фазы

- [ ] **Прогнать всё**

Run: `./gradlew test && ./gradlew assembleDebug`
Expected: все юнит-тесты зелёные, сборка успешна.

- [ ] **Обновить документацию** (рабочее соглашение проекта)

- `docs/plan.md` — добавить «Фаза 12» с отмеченными пунктами
- `docs/progress.md` — что сделано, с датой
- `CHANGELOG.md` — раздел «Добавлено»: закрепление заметок, multiline-заголовок, порядок книг Синодального, запоминание последней папки/заметки; «Исправлено»: воскрешение удалённой заметки

- [ ] **Ревью субагентом** (рабочее соглашение проекта): создать субагента для ревью диффа фазы.
