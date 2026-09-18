# Фаза 14 — Молитвенный журнал

> **For agentic workers:** REQUIRED SUB-SKILL: используйте `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, чтобы выполнять этот план задача-за-задачей. Шаги размечены чекбоксами (`- [ ]`).

**Goal:** Портировать молитвенный журнал из iOS: четвёртая вкладка с экраном «Сегодня», списком всех просьб, карточкой просьбы с датированными дописками, экраном отвеченных молитв, редактором и ежедневным напоминанием.

**Architecture:** Вся бизнес-логика — чистые функции и функции над копиями сущностей, покрытые pure-JVM тестами (задачи 14.3–14.5), поэтому новых тестовых зависимостей не требуется. Данные — две Room-сущности с каскадным удалением, добавляемые автомиграцией 2→3. Напоминание — один `AlarmManager`-будильник, перевзводимый после каждого срабатывания и после перезагрузки. UI — семь экранов внутри собственного nav-графа.

**Tech Stack:** Kotlin 2.3.21, Compose (BOM 2026.05.01), Room 2.8.4 + KSP, Koin 4.2.1, Navigation Compose 2.9.8, DataStore, `AlarmManager` + `BroadcastReceiver`, JUnit 4.13.2.

**Предусловие:** закрыта задача 12.2 (рабочие автомиграции Room). Фаза независима от фазы 13 и может идти параллельно с ней.

**Порядок:** 14.1 → 14.2 → 14.3 → 14.4 → 14.5 → 14.6 → 14.7 → 14.8 → 14.9 → 14.10 → 14.11 → 14.12 → 14.13

---

## Продуктовые ограничения (жёсткие, из iOS)

Это не пожелания к тону, а требования к фиче. Нарушение любого из них — баг:

- **Никаких** «просрочено», «пропущено», «провалено», стриков, бейджей, уровней, поздравлений и мотивирующих подталкиваний.
- Считать факты («молитесь об этом 47 дней») можно; считать серии подряд — нельзя.
- Нет автоматического архивирования, удаления и истечения срока просьб.
- Нет ИИ-текстов молитв и автоподбора стихов.
- Молитвы **не индексируются** ни в каком общем поиске приложения (требование приватности).
- Статус `entrusted` («доверить Богу») — это **не** «отменено» и **не** «просрочено».

---

## Отличия от iOS, сделанные намеренно

| Что | В iOS | На Android | Почему |
|---|---|---|---|
| Множественное число | Ручной `RussianPlural`, применяется ко всем локалям — по-английски выходит «21 day» | `res/values/plurals.xml` + `values-ru/plurals.xml`, `getQuantityString` | Платформа делает это верно для каждой локали; порт бага не нужен |
| Подборка «на сегодня» | В плане репозитория описан отбор 3–7 просьб, но **отгружен другой код** | Портируем **код**: весь активный список, непомоленные сегодня выше | Зафиксировано тестом `showsAllActiveRequestsRegardlessOfCount`; см. риск **R10** |
| Перевзвод напоминания | Только при возврате приложения на передний план | + `BOOT_COMPLETED` receiver | На Android будильники не переживают перезагрузку |
| Нумерация дней недели | 1 = воскресенье (Foundation) | `java.time.DayOfWeek`, 1 = понедельник | Нужна конвертация при переносе `ReminderRule.weekdays` |

---

## Структура файлов

**Создаются (данные):**
- `data/db/PrayerRequest.kt`, `data/db/PrayerEntry.kt` — Room-сущности
- `data/db/PrayerConverters.kt` — конвертеры для `verseRefs`
- `data/db/PrayerDao.kt`
- `data/PrayerRepository.kt`
- `data/prayer/PrayerActions.kt` — `hasPrayedToday`, `markPrayedToday`, `markAnswered`, `markEntrusted`
- `data/prayer/PrayerSelectionService.kt`
- `data/prayer/PrayerReminderScheduler.kt`, `data/prayer/PrayerReminderReceiver.kt`, `data/prayer/BootCompletedReceiver.kt`

**Создаются (UI):**
- `ui/screens/prayer/PrayerTodayScreen.kt`, `PrayerListScreen.kt`, `PrayerDetailScreen.kt`, `PrayerEditorScreen.kt`, `AnsweredPrayersScreen.kt`, `PrayerReminderSettingsScreen.kt`
- `ui/components/prayer/PrayerCard.kt`, `AnsweredPrayerCard.kt`, `PrayerCategoryBadge.kt`
- `ui/viewmodels/PrayerTodayViewModel.kt`, `PrayerListViewModel.kt`, `PrayerDetailViewModel.kt`, `PrayerEditorViewModel.kt`, `AnsweredPrayersViewModel.kt`, `PrayerReminderViewModel.kt`

**Тесты:** `PrayerActionsTest.kt`, `PrayerSelectionServiceTest.kt`, `PrayerReminderSchedulerTest.kt`

**Изменяются:** `data/db/AppDatabase.kt`, `data/settings/SettingsRepository.kt`, `di/AppModule.kt`, `ui/navigation/NavGraph.kt`, `AndroidManifest.xml`, `res/values/strings.xml`, `res/values-ru/strings.xml`, `res/values/plurals.xml`, `res/values-ru/plurals.xml`

---

## Задача 14.1: Сущности и миграция 2→3

Каскад: удаление просьбы удаляет её дописки — это контракт, зафиксированный тестом iOS `deletingRequest_cascadesToEntries`.

Поля `sortOrder` и `reminderRule` в iOS объявлены, но **не читаются ничем** в MVP. На Android они не переносятся: пустые колонки без потребителя — это долг, а не паритет. Если позже понадобятся — добавятся отдельной миграцией.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/db/PrayerRequest.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/db/PrayerEntry.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/db/PrayerConverters.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/AppDatabase.kt`
- Test: `app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt` (дополняется)

- [ ] **Шаг 1: Написать падающий тест миграции**

Дописать в `MigrationTest.kt` из задачи 12.2:

```kotlin
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
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: FAIL — `no such table: prayer_requests`.

- [ ] **Шаг 3: Создать сущности**

`data/db/PrayerRequest.kt`:

```kotlin
package ru.edgarakert.biblenote.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrayerCategory { FAMILY, CHURCH, HEALTH, WORK, GRATITUDE, PERSONAL, WORLD, OTHER }

/**
 * ENTRUSTED — «доверить Богу». Это не «отменено» и не «просрочено».
 * ARCHIVED объявлен для совместимости со схемой iOS, но ничем не выставляется.
 */
enum class PrayerStatus { ACTIVE, ANSWERED, ENTRUSTED, ARCHIVED }

@Entity(tableName = "prayer_requests")
data class PrayerRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    /** Сохраняется без trim — в отличие от title, чтобы абзацы пользователя не съедались. */
    val body: String = "",
    val category: PrayerCategory = PrayerCategory.OTHER,
    val status: PrayerStatus = PrayerStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val answeredAt: Long? = null,
    val answerText: String? = null,
    /** Сырые строки ссылок ("Флп 4:6-7"); разбираются BibleReferenceParser при показе. */
    val verseRefs: List<String> = emptyList(),
    val lastPrayedAt: Long? = null,
    /** Количество РАЗНЫХ дней, когда нажимали «помолился». Это не серия подряд. */
    @ColumnInfo(defaultValue = "0") val prayedDaysCount: Int = 0,
)
```

`data/db/PrayerEntry.kt`:

```kotlin
package ru.edgarakert.biblenote.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_entries",
    foreignKeys = [ForeignKey(
        entity = PrayerRequest::class,
        parentColumns = ["id"],
        childColumns = ["requestId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("requestId")]
)
data class PrayerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: Long,
    val text: String = "",
    val date: Long = System.currentTimeMillis(),
)
```

`data/db/PrayerConverters.kt`:

```kotlin
package ru.edgarakert.biblenote.data.db

import androidx.room.TypeConverter

class PrayerConverters {
    /** Разделитель — перевод строки: в ссылке на стих он встретиться не может. */
    @TypeConverter
    fun verseRefsToString(refs: List<String>): String = refs.joinToString("\n")

    @TypeConverter
    fun stringToVerseRefs(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("\n")

    @TypeConverter fun categoryToString(c: PrayerCategory): String = c.name
    @TypeConverter fun stringToCategory(v: String): PrayerCategory = PrayerCategory.valueOf(v)
    @TypeConverter fun statusToString(s: PrayerStatus): String = s.name
    @TypeConverter fun stringToStatus(v: String): PrayerStatus = PrayerStatus.valueOf(v)
}
```

- [ ] **Шаг 4: Подключить к базе**

В `AppDatabase.kt`:

```kotlin
@Database(
    entities = [Note::class, Folder::class, PrayerRequest::class, PrayerEntry::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
@TypeConverters(PrayerConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun prayerDao(): PrayerDao
    // ...
}
```

- [ ] **Шаг 5: Запустить тест и убедиться, что он проходит**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: PASS, оба теста. Закоммитить появившийся `app/schemas/.../3.json`.

- [ ] **Шаг 6: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/db/ app/schemas/ app/src/androidTest/
git commit -m "feat: add prayer request and entry entities with auto-migration 2 to 3"
```

---

## Задача 14.2: DAO и репозиторий

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/db/PrayerDao.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/PrayerRepository.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt`

- [ ] **Шаг 1: Написать DAO**

```kotlin
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequest(request: PrayerRequest): Long

    @Delete
    suspend fun deleteRequest(request: PrayerRequest)

    /** Свежие дописки сверху — так их показывает карточка просьбы. */
    @Query("SELECT * FROM prayer_entries WHERE requestId = :requestId ORDER BY date DESC")
    fun observeEntries(requestId: Long): Flow<List<PrayerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: PrayerEntry): Long

    @Delete
    suspend fun deleteEntry(entry: PrayerEntry)
}
```

- [ ] **Шаг 2: Написать репозиторий**

`PrayerRepository` — тонкая обёртка над DAO, повторяющая стиль `NoteRepository` (без `withContext`, Room переключает поток сам).

- [ ] **Шаг 3: Зарегистрировать в Koin**

В `di/AppModule.kt`:

```kotlin
    single<PrayerDao> { get<AppDatabase>().prayerDao() }
    single<PrayerRepository> { PrayerRepository(get()) }
```

- [ ] **Шаг 4: Собрать**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/ app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt
git commit -m "feat: add prayer DAO and repository"
```

---

## Задача 14.3: Действия над просьбой

Четыре правила, каждое зафиксировано тестом iOS:

- **`hasPrayedToday`** — сравнение **календарных дней** в локальной зоне, а не окно в 24 часа.
- **`markPrayedToday`** — идемпотентно в рамках дня: второй раз за день **не увеличивает** счётчик и **не обновляет** `lastPrayedAt`.
- **`markAnswered`** — пустой или пробельный текст **отклоняется**, ничего не меняя; иначе сохраняется **обрезанный** текст.
- **`markEntrusted`** — меняет только статус, поля ответа не трогает.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerActions.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerActionsTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerActionsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun request() = PrayerRequest(id = 1, title = "О работе")

    @Test
    fun `hasPrayedToday is false when never prayed`() {
        assertFalse(PrayerActions.hasPrayedToday(request(), at(2026, 9, 5, 12, 0), zone))
    }

    @Test
    fun `markPrayedToday increments on the first call of the day`() {
        val now = at(2026, 9, 5, 8, 0)
        val result = PrayerActions.markPrayedToday(request(), now, zone)
        assertEquals(1, result.prayedDaysCount)
        assertEquals(now, result.lastPrayedAt)
    }

    @Test
    fun `markPrayedToday does not double count on the same day`() {
        val morning = at(2026, 9, 5, 8, 0)
        val evening = at(2026, 9, 5, 20, 0)
        val once = PrayerActions.markPrayedToday(request(), morning, zone)
        val twice = PrayerActions.markPrayedToday(once, evening, zone)

        assertEquals(1, twice.prayedDaysCount)
        // lastPrayedAt НЕ обновляется на более позднее время того же дня
        assertEquals(morning, twice.lastPrayedAt)
        assertSame(once, twice)
    }

    @Test
    fun `markPrayedToday counts again on a new day`() {
        val today = at(2026, 9, 5, 20, 0)
        val tomorrow = at(2026, 9, 6, 8, 0)
        val once = PrayerActions.markPrayedToday(request(), today, zone)
        val twice = PrayerActions.markPrayedToday(once, tomorrow, zone)
        assertEquals(2, twice.prayedDaysCount)
    }

    @Test
    fun `a gap of many days does not reset the count`() {
        val first = PrayerActions.markPrayedToday(request(), at(2026, 1, 1, 9, 0), zone)
        val later = PrayerActions.markPrayedToday(first, at(2026, 9, 5, 9, 0), zone)
        assertEquals(2, later.prayedDaysCount)
    }

    @Test
    fun `markAnswered rejects blank text and changes nothing`() {
        val original = request()
        val result = PrayerActions.markAnswered(original, "   ", at(2026, 9, 5, 9, 0))
        assertNull(result)
        assertEquals(PrayerStatus.ACTIVE, original.status)
        assertNull(original.answerText)
    }

    @Test
    fun `markAnswered stores the trimmed text and switches status`() {
        val at = at(2026, 9, 5, 9, 0)
        val result = PrayerActions.markAnswered(request(), "  Бог дал работу через две недели  ", at)!!
        assertEquals(PrayerStatus.ANSWERED, result.status)
        assertEquals("Бог дал работу через две недели", result.answerText)
        assertEquals(at, result.answeredAt)
    }

    @Test
    fun `markEntrusted sets the status without touching the answer fields`() {
        val result = PrayerActions.markEntrusted(request())
        assertEquals(PrayerStatus.ENTRUSTED, result.status)
        assertNull(result.answerText)
        assertNull(result.answeredAt)
    }
}
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*PrayerActionsTest*"`
Expected: FAIL — `Unresolved reference: PrayerActions`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Действия над просьбой. Чистые функции над копиями: сохраняет вызывающий.
 * Здесь нет «серий», «просрочек» и «пропусков» — только факты, это продуктовое требование.
 */
object PrayerActions {

    /** Сравнение календарных дней в локальной зоне, а не окна в 24 часа. */
    fun hasPrayedToday(
        request: PrayerRequest,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val last = request.lastPrayedAt ?: return false
        val lastDay = Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return lastDay == today
    }

    /**
     * Идемпотентно в рамках календарного дня: повторный вызов в тот же день
     * не увеличивает счётчик и не сдвигает lastPrayedAt на более позднее время.
     */
    fun markPrayedToday(
        request: PrayerRequest,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): PrayerRequest {
        if (hasPrayedToday(request, now, zone)) return request
        return request.copy(lastPrayedAt = now, prayedDaysCount = request.prayedDaysCount + 1)
    }

    /**
     * Возвращает null, если текст ответа пуст после trim — и тогда НИЧЕГО не меняется.
     * Это инвариант уровня модели: кнопка «Сохранить» блокируется поверх него, а не вместо.
     */
    fun markAnswered(
        request: PrayerRequest,
        answerText: String,
        answeredAt: Long = System.currentTimeMillis(),
    ): PrayerRequest? {
        val trimmed = answerText.trim()
        if (trimmed.isEmpty()) return null
        return request.copy(
            answerText = trimmed,
            answeredAt = answeredAt,
            status = PrayerStatus.ANSWERED
        )
    }

    /** «Доверить Богу»: меняется только статус, поля ответа остаются пустыми. */
    fun markEntrusted(request: PrayerRequest): PrayerRequest =
        request.copy(status = PrayerStatus.ENTRUSTED)
}
```

- [ ] **Шаг 4: Запустить и убедиться, что проходит**

Run: `./gradlew testDebugUnitTest --tests "*PrayerActionsTest*"`
Expected: PASS, 8 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerActions.kt app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerActionsTest.kt
git commit -m "feat: add prayer request actions (prayed today, answered, entrusted)"
```

---

## Задача 14.4: `PrayerSelectionService`

Алгоритм экрана «Сегодня». Портируется **отгруженный код iOS**, а не устаревший план из его репозитория (риск **R10**):

1. оставить только `ACTIVE`;
2. устойчиво разделить на две группы: сначала те, за кого сегодня **не** молились, потом те, за кого молились;
3. вернуть всё, **без ограничения количества**.

Порядок внутри каждой группы сохраняет порядок входа — это часть контракта.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerSelectionService.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerSelectionServiceTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerSelectionServiceTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private fun at(d: Int, h: Int) =
        LocalDateTime.of(2026, 9, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private val now = at(5, 12)

    private fun req(
        title: String,
        status: PrayerStatus = PrayerStatus.ACTIVE,
        lastPrayedAt: Long? = null,
    ) = PrayerRequest(title = title, status = status, lastPrayedAt = lastPrayedAt)

    @Test
    fun `shows all active requests regardless of count`() {
        val requests = (1..20).map { req("Просьба $it") }
        assertEquals(20, PrayerSelectionService.todaysSelection(requests, now, zone).size)
    }

    @Test
    fun `excludes every non-active status`() {
        val requests = listOf(
            req("активная"),
            req("отвеченная", PrayerStatus.ANSWERED),
            req("доверенная", PrayerStatus.ENTRUSTED),
            req("архивная", PrayerStatus.ARCHIVED),
        )
        assertEquals(
            listOf("активная"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `not prayed today comes before prayed today`() {
        val requests = listOf(req("помолились", lastPrayedAt = at(5, 9)), req("не молились"))
        assertEquals(
            listOf("не молились", "помолились"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `prayed on an earlier day counts as not prayed today`() {
        val requests = listOf(req("сегодня", lastPrayedAt = at(5, 9)), req("вчера", lastPrayedAt = at(4, 9)))
        assertEquals(
            listOf("вчера", "сегодня"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }

    @Test
    fun `preserves input order within each bucket`() {
        val requests = listOf(
            req("a"), req("b"),
            req("c", lastPrayedAt = at(5, 9)), req("d", lastPrayedAt = at(5, 10))
        )
        assertEquals(
            listOf("a", "b", "c", "d"),
            PrayerSelectionService.todaysSelection(requests, now, zone).map { it.title }
        )
    }
}
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*PrayerSelectionServiceTest*"`
Expected: FAIL — `Unresolved reference: PrayerSelectionService`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.ZoneId

/**
 * Что показывать на экране «Сегодня».
 *
 * Ограничения по количеству НЕТ: показывается весь активный список, где просьбы,
 * за которые сегодня ещё не молились, идут выше уже помоленных. Нажатие «Помолился»
 * не убирает карточку — она просто переезжает в нижнюю группу.
 */
object PrayerSelectionService {

    fun todaysSelection(
        requests: List<PrayerRequest>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<PrayerRequest> = requests
        .filter { it.status == PrayerStatus.ACTIVE }
        // sortedBy устойчива: порядок внутри каждой группы сохраняется — это часть контракта.
        .sortedBy { if (PrayerActions.hasPrayedToday(it, now, zone)) 1 else 0 }
}
```

- [ ] **Шаг 4: Запустить и убедиться, что проходит**

Run: `./gradlew testDebugUnitTest --tests "*PrayerSelectionServiceTest*"`
Expected: PASS, 5 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerSelectionService.kt app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerSelectionServiceTest.kt
git commit -m "feat: add today's prayer selection ordering"
```

---

## Задача 14.5: Ежедневное напоминание

Одно напоминание за раз, один фиксированный идентификатор. Текст уведомления — **всегда общий**, без количества просьб: в iOS счётчик принимается параметром и намеренно не попадает в текст, и это гарантия приватности (уведомление на локскрине не должно раскрывать содержимое журнала).

Выбор механизма: `AlarmManager.setAndAllowWhileIdle` на вычисленное время, перевзвод после каждого срабатывания. `setAndAllowWhileIdle` **не требует** разрешения `SCHEDULE_EXACT_ALARM`, а точность до минуты для напоминания о молитве не нужна. `WorkManager` здесь хуже: периодическая работа не привязывается к настенному времени.

Настройки в DataStore — как в iOS: включённость и время как **минуты от полуночи**.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerReminderScheduler.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/prayer/PrayerReminderReceiver.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/prayer/BootCompletedReceiver.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/settings/SettingsRepository.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerReminderSchedulerTest.kt`

- [ ] **Шаг 1: Написать падающий тест расчёта времени**

Тест покрывает только чистую математику — без `AlarmManager`, поэтому остаётся pure-JVM.

```kotlin
package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PrayerReminderSchedulerTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private fun at(d: Int, h: Int, m: Int) =
        LocalDateTime.of(2026, 9, d, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `next trigger is today when the time has not passed yet`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 7, 0), zone = zone
        )
        assertEquals(at(5, 9, 0), next)
    }

    @Test
    fun `next trigger is tomorrow when the time has already passed`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 10, 0), zone = zone
        )
        assertEquals(at(6, 9, 0), next)
    }

    @Test
    fun `a time equal to now counts as already passed`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 9 * 60, now = at(5, 9, 0), zone = zone
        )
        assertEquals(at(6, 9, 0), next)
    }

    @Test
    fun `minutes since midnight map to hour and minute`() {
        val next = PrayerReminderScheduler.nextTriggerAt(
            minutesSinceMidnight = 21 * 60 + 30, now = at(5, 7, 0), zone = zone
        )
        assertEquals(at(5, 21, 30), next)
    }
}
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*PrayerReminderSchedulerTest*"`
Expected: FAIL — `Unresolved reference: PrayerReminderScheduler`.

- [ ] **Шаг 3: Реализовать расчёт и планирование**

```kotlin
package ru.edgarakert.biblenote.data.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

object PrayerReminderScheduler {

    /** Один будильник за раз — фиксированный request code, повторный вызов перезаписывает его. */
    private const val REQUEST_CODE = 4208

    /**
     * Ближайшее срабатывание для времени, заданного минутами от полуночи.
     * Момент, равный «сейчас», считается уже прошедшим — переносим на завтра.
     */
    fun nextTriggerAt(
        minutesSinceMidnight: Int,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        val candidate = nowZoned
            .withHour(minutesSinceMidnight / 60)
            .withMinute(minutesSinceMidnight % 60)
            .withSecond(0)
            .withNano(0)

        val target = if (candidate.toInstant().toEpochMilli() > now) candidate else candidate.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    fun schedule(context: Context, minutesSinceMidnight: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // setAndAllowWhileIdle не требует SCHEDULE_EXACT_ALARM: минутная точность
        // напоминанию о молитве не нужна, а разрешение на точные будильники — нужна лишняя.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt(minutesSinceMidnight),
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PrayerReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
```

`PrayerReminderReceiver` в `onReceive`: показать уведомление с заголовком `R.string.prayer_reminder_title` и телом `R.string.prayer_reminder_body_generic`, затем **перевзвести** будильник на следующий день, прочитав настройку из DataStore через `goAsync()`.

`BootCompletedReceiver` на `ACTION_BOOT_COMPLETED`: если напоминание включено — перевзвести.

- [ ] **Шаг 4: Объявить в манифесте**

Манифест сейчас не содержит ни одного permission — добавляем два:

```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

и два receiver'а внутри `<application>`:

```xml
        <receiver android:name=".data.prayer.PrayerReminderReceiver" android:exported="false" />

        <receiver android:name=".data.prayer.BootCompletedReceiver" android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
```

- [ ] **Шаг 5: Добавить настройки**

В `SettingsRepository` — по образцу существующих ключей:

```kotlin
        private val KEY_PRAYER_REMINDER_ENABLED = booleanPreferencesKey("prayer.reminder.enabled")
        private val KEY_PRAYER_REMINDER_MINUTES = intPreferencesKey("prayer.reminder.minutesSinceMidnight")
```

`prayerReminderEnabled: Flow<Boolean>` (по умолчанию `false`) и `prayerReminderMinutes: Flow<Int>` (по умолчанию `540` — 09:00), плюс сеттеры.

- [ ] **Шаг 6: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*PrayerReminderSchedulerTest*"`
Expected: PASS, 4 теста.

- [ ] **Шаг 7: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/ app/src/main/AndroidManifest.xml app/src/test/java/ru/edgarakert/biblenote/data/prayer/PrayerReminderSchedulerTest.kt
git commit -m "feat: schedule a daily prayer reminder notification"
```

---

## Задача 14.6: Строки и множественные числа

**Files:**
- Modify: `res/values/strings.xml`, `res/values-ru/strings.xml`
- Create/Modify: `res/values/plurals.xml`, `res/values-ru/plurals.xml`

- [ ] **Шаг 1: Добавить все строки в обе локали**

Полный набор ключей (RU / EN) — из iOS, с переводом имён под конвенцию проекта (`snake_case` с префиксом, без точек):

`prayer_today_title` Сегодня / Today · `prayer_today_empty_state` Добавьте первую просьбу / Add your first prayer request · `prayer_today_new_button` Новая просьба / New Request · `prayer_reminder_title` Время молитвы / Time to Pray · `prayer_reminder_body_generic` Уделите время молитве / Take a moment to pray · `prayer_editor_new_title` Новая просьба / New Request · `prayer_editor_edit_title` Изменить просьбу / Edit Request · `prayer_editor_title_placeholder` О чём молитесь? / What are you praying for? · `prayer_editor_body_placeholder` Подробности / Details · `prayer_editor_category_label` Категория / Category · `prayer_editor_verses_label` Стихи / Verses · `prayer_editor_verses_placeholder` например Флп 4:6-7 / e.g. Phil 4:6-7 · `prayer_category_family` Семья / Family · `prayer_category_church` Церковь / Church · `prayer_category_health` Здоровье / Health · `prayer_category_work` Работа / Work · `prayer_category_gratitude` Благодарность / Gratitude · `prayer_category_personal` Личное / Personal · `prayer_category_world` Мир / World · `prayer_category_other` Другое / Other · `prayer_card_pray_button` Помолился / Prayed · `prayer_list_title` Все просьбы / All Requests · `prayer_list_filter_active` Активные / Active · `prayer_list_filter_answered` Отвеченные / Answered · `prayer_list_filter_all` Все / All · `prayer_list_entrust_button` Доверить Богу / Entrust to God · `prayer_list_empty_state` Здесь пока пусто / No requests here yet · `prayer_delete` Удалить просьбу / Delete Request · `prayer_detail_pray_today_button` Помолился сегодня / Prayed Today · `prayer_detail_answered_button` Бог ответил / God Answered · `prayer_detail_entrust_menu` Доверить Богу / Entrust to God · `prayer_detail_entries_header` Обновления / Updates · `prayer_detail_add_entry_placeholder` Добавить обновление / Add an update · `prayer_detail_add_entry_button` Добавить / Add · `prayer_answer_title` Бог ответил / God Answered · `prayer_answer_placeholder` Как Бог ответил? / How did God answer? · `prayer_answer_save` Сохранить / Save · `prayer_answer_date_label` Дата / Date · `prayer_answered_title` Отвеченные молитвы / Answered Prayers · `prayer_answered_count` Записано ответов: %1$d / Answers recorded: %1$d · `prayer_answered_empty_state` Здесь появятся ответы на молитвы / Answered prayers will appear here · `prayer_answered_duration_prefix` Молились / Prayed for · `prayer_reminder_settings_title` Напоминание / Reminder · `prayer_reminder_enable_toggle` Ежедневное напоминание / Daily Reminder · `prayer_reminder_time_label` Время / Time · `tab_prayers` Молитвы / Prayers

- [ ] **Шаг 2: Добавить плюрал «дней молитвы»**

`res/values/plurals.xml`:

```xml
    <plurals name="prayer_days">
        <item quantity="one">praying for %1$d day</item>
        <item quantity="other">praying for %1$d days</item>
    </plurals>
```

`res/values-ru/plurals.xml`:

```xml
    <plurals name="prayer_days">
        <item quantity="one">молитесь об этом %1$d день</item>
        <item quantity="few">молитесь об этом %1$d дня</item>
        <item quantity="many">молитесь об этом %1$d дней</item>
        <item quantity="other">молитесь об этом %1$d дня</item>
    </plurals>
```

Плюрал для длительности на экране отвеченных (`prayer_duration_years`, `_months`, `_weeks`, `_days`) добавить по тому же образцу — он понадобится в задаче 14.12.

- [ ] **Шаг 3: Проверить полноту локалей**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, без предупреждений о недостающих переводах.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/main/res/values/ app/src/main/res/values-ru/
git commit -m "feat: add prayer journal strings and plurals in both locales"
```

---

## Задача 14.7: Четвёртая вкладка и nav-граф

`TopLevelRoute` — приватный enum в `NavGraph.kt`, нижняя панель строится итерацией по `TopLevelRoute.entries`, так что добавление вкладки локально.

В iOS вкладка «Молитвы» стоит **между Библией и настройками** — сохраняем этот порядок.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt`

- [ ] **Шаг 1: Добавить константу вкладки**

```kotlin
    PRAYERS(
        "prayers_graph",
        R.string.tab_prayers,
        Icons.Outlined.VolunteerActivism,
        Icons.Filled.VolunteerActivism
    ),
```

Расположить **между** `BIBLE` и `SETTINGS` — порядок в enum задаёт порядок в панели.

- [ ] **Шаг 2: Добавить граф**

```kotlin
    navigation(route = "prayers_graph", startDestination = "prayers") {
        composable("prayers") { PrayerTodayScreen(...) }
        composable("prayers/list") { PrayerListScreen(...) }
        composable("prayers/answered") { AnsweredPrayersScreen(...) }
        composable("prayers/reminder") { PrayerReminderSettingsScreen(...) }
        composable(
            "prayers/detail/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { PrayerDetailScreen(...) }
        composable(
            "prayers/editor/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { PrayerEditorScreen(...) }
    }
```

Для создания новой просьбы передавать `requestId = -1L`.

- [ ] **Шаг 3: Собрать**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, в нижней панели четыре вкладки.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/navigation/NavGraph.kt
git commit -m "feat: add the Prayers tab and navigation graph"
```

---

## Задача 14.8: Экран «Сегодня» и `PrayerCard`

**Files:**
- Create: `ui/screens/prayer/PrayerTodayScreen.kt`, `ui/viewmodels/PrayerTodayViewModel.kt`
- Create: `ui/components/prayer/PrayerCard.kt`, `ui/components/prayer/PrayerCategoryBadge.kt`

- [ ] **Шаг 1: `PrayerCategoryBadge`**

Пилюля: текст категории цветом `primary`, размер 11sp, `padding(horizontal = 8.dp, vertical = 3.dp)`, фон `primary.copy(alpha = 0.12f)`, форма `CircleShape`.

- [ ] **Шаг 2: `PrayerCard`**

```kotlin
@Composable
fun PrayerCard(
    request: PrayerRequest,
    hasPrayedToday: Boolean,
    modifier: Modifier = Modifier,
    onPray: (() -> Unit)? = null,
)
```

Структура: `Column(spacing 8.dp)` внутри `Card(RoundedCornerShape(16.dp), colorScheme.surface)`, `padding(16.dp)`:
1. `Row`: заголовок (16sp Medium, `maxLines = 2`) — `Spacer(weight)` — `PrayerCategoryBadge`.
2. Если `prayedDaysCount > 0` — `pluralStringResource(R.plurals.prayer_days, count, count)` вторичным цветом.
3. Если `onPray != null` — кнопка `prayer_card_pray_button`, **`enabled = !hasPrayedToday`**, при `hasPrayedToday` — `alpha = 0.4f`.

Кнопка рисуется **только когда передан обработчик** — на экране «Все просьбы» он не передаётся, там кнопки нет.

- [ ] **Шаг 3: ViewModel**

`PrayerTodayViewModel` отдаёт `todaysSelection` из потока всех просьб:

```kotlin
    val requests: StateFlow<List<PrayerRequest>> = repository.observeAllRequests()
        .map { PrayerSelectionService.todaysSelection(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markPrayed(request: PrayerRequest) {
        viewModelScope.launch { repository.upsertRequest(PrayerActions.markPrayedToday(request)) }
    }
```

- [ ] **Шаг 4: Экран**

`TopAppBar` с заголовком `prayer_today_title`, слева две иконки (список всех просьб, отвеченные), справа `+` и колокольчик. Тело — `LazyColumn(spacing 12.dp, padding 20.dp)` из `PrayerCard`. Тап по карточке (кроме кнопки) ведёт на `prayers/detail/{id}`.

Пустое состояние: иконка `VolunteerActivism` 42dp цветом `primaryContainer`, текст `prayer_today_empty_state`, кнопка `prayer_today_new_button`. **Без укоряющих формулировок.**

После нажатия «Помолился» карточка **не исчезает** — она переезжает в нижнюю группу, а кнопка гаснет.

- [ ] **Шаг 5: Проверить и закоммитить**

Run: `./gradlew installDebug`
Expected: вкладка «Молитвы» открывает «Сегодня» с пустым состоянием.

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add the Today prayer screen with prayer cards"
```

---

## Задача 14.9: Экран «Все просьбы»

**Files:** `ui/screens/prayer/PrayerListScreen.kt`, `ui/viewmodels/PrayerListViewModel.kt`

- [ ] **Шаг 1: Реализовать экран**

- Данные: все просьбы, `createdAt DESC`.
- Фильтр — `SingleChoiceSegmentedButtonRow`: активные / отвеченные / все. По умолчанию — активные. Просьбы со статусом `ENTRUSTED` и `ARCHIVED` видны **только** под «Все».
- Поиск: регистронезависимое вхождение по заголовку **или** телу, применяется после фильтра статуса.
- Группировка по категориям, секции в порядке объявления enum (`FAMILY, CHURCH, HEALTH, WORK, GRATITUDE, PERSONAL, WORLD, OTHER`); пустые категории пропускаются. Заголовок секции — название категории.
- Строки: `PrayerCard` **без** кнопки «Помолился». Тап → карточка просьбы.
- Свайп влево: удалить (с подтверждением) и, если просьба активна, «Доверить Богу» — **без подтверждения**.
- Пустое состояние: `prayer_list_empty_state`, один и тот же текст для любого фильтра.

- [ ] **Шаг 2: Проверить и закоммитить**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add the all prayer requests screen with filters and search"
```

---

## Задача 14.10: Карточка просьбы

Самый насыщенный экран. Порядок блоков сверху вниз:

1. Заголовок + бейдж категории.
2. Тело — только если непусто.
3. Стихи — только если хоть одна ссылка разобралась. Строится как `request.verseRefs.flatMap { parser.parse(it) }`; тап открывает **существующий** `BibleVerseSheet` (второй парсер не пишем).
4. Блок ответа — только при `status == ANSWERED && answerText != null`: заголовок `prayer_answer_title` капсом вторичным цветом, текст, фон `surface`, радиус 16.
5. Кнопки действий — только при `status == ACTIVE`: над ними, если `prayedDaysCount > 0`, плюрал `prayer_days`; затем две равные кнопки — «Помолился сегодня» (мягкая, `primary.copy(alpha = 0.12f)`, **выключена если уже молились сегодня**) и «Бог ответил» (сплошная `primary`, **никогда не выключена**).
6. Дописки — **всегда**, при любом статусе: заголовок `prayer_detail_entries_header`, поле ввода с кнопкой «Добавить» (**выключена пока текст пуст после trim**), список дописок `date DESC`.

Меню в тулбаре: при `ACTIVE` — «Переименовать» (открывает редактор со всеми полями) и «Доверить Богу»; всегда — «Удалить» с подтверждением.

Шторка ответа: многострочное поле `prayer_answer_placeholder`, `DatePicker` **только с датой**, кнопка сохранения **выключена пока текст пуст**. Сохранение вызывает `PrayerActions.markAnswered(...)` и закрывает шторку **только если функция вернула не-null** — блокировка кнопки это подстраховка, а настоящая защита в модели.

**Files:** `ui/screens/prayer/PrayerDetailScreen.kt`, `ui/viewmodels/PrayerDetailViewModel.kt`

- [ ] **Шаг 1: Реализовать экран и ViewModel**
- [ ] **Шаг 2: Проверить сценарии вручную**

Run: `./gradlew installDebug`

Проверить: «Помолился сегодня» гаснет после нажатия и снова доступна на следующий день; «Бог ответил» с пустым текстом не сохраняется; после ответа просьба уходит из «Сегодня» и появляется в «Отвеченных»; удаление просьбы удаляет и её дописки.

- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add the prayer detail screen with entries and the answer flow"
```

---

## Задача 14.11: Редактор просьбы

Один экран на создание и редактирование, различаются по `requestId` (`-1L` — создание).

Поля: заголовок (однострочный), тело (3–8 строк), категория (выпадающий список по всем восьми), стихи (однострочное поле, значения через запятую).

Правила сохранения, важные для совпадения round-trip'ов с iOS:
- **`title` сохраняется обрезанным**, **`body` — как есть, без trim**;
- стихи: разбить по `,`, обрезать каждый, выбросить пустые;
- кнопка «Сохранить» **выключена пока заголовок пуст после trim** — это единственное обязательное поле;
- при редактировании `createdAt`, `status` и счётчики **не трогаются**.

**Files:** `ui/screens/prayer/PrayerEditorScreen.kt`, `ui/viewmodels/PrayerEditorViewModel.kt`

- [ ] **Шаг 1: Реализовать**
- [ ] **Шаг 2: Проверить** — создание просьбы занимает несколько секунд: ввести заголовок и нажать «Сохранить».
- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add the prayer editor for creating and editing requests"
```

---

## Задача 14.12: Экран «Отвеченные молитвы»

Ради этого экрана существует вся фича.

- Данные: `status == ANSWERED`, сортировка по `answeredAt DESC`.
- Группировка по **году** из `answeredAt ?: createdAt`, годы по убыванию; заголовок группы — год, капсом, цветом `primary`.
- Сверху счётчик: `prayer_answered_count`.
- Карточка (`AnsweredPrayerCard`): заголовок, строка длительности, текст ответа. Радиус 18, фон `surface`, рамка `primary.copy(alpha = 0.2f)` толщиной 1.
- Длительность: от `createdAt` до `answeredAt`, **одна самая крупная ненулевая единица** («Молились 3 месяца»). Считать через `java.time.Period`/`Duration`, выводить через плюралы из задачи 14.6. Строка не выводится, если `answeredAt == null`.
- Пустое состояние: иконка `CheckCircle` 42dp цветом `primaryContainer` + `prayer_answered_empty_state`.

**Files:** `ui/screens/prayer/AnsweredPrayersScreen.kt`, `ui/components/prayer/AnsweredPrayerCard.kt`, `ui/viewmodels/AnsweredPrayersViewModel.kt`

- [ ] **Шаг 1: Реализовать**
- [ ] **Шаг 2: Проверить** — просьба, созданная 3 месяца назад и отвеченная сегодня, показывает «Молились 3 месяца» и попадает в группу текущего года.
- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add the answered prayers screen grouped by year"
```

---

## Задача 14.13: Настройки напоминания

- Переключатель `prayer_reminder_enable_toggle`. При включении: **сначала** запросить `POST_NOTIFICATIONS` (API 33+); при отказе — вернуть переключатель в выключенное состояние; при согласии — запланировать. При выключении — отменить будильник.
- Выбор времени показывается **только когда включено**; изменение немедленно перепланирует.
- Разрешение запрашивается **в момент включения**, а не на первом запуске приложения.

Дополнительно к iOS: перевзводить будильник при возврате приложения на передний план (`Lifecycle.Event.ON_START`) — на случай, если система отменила будильник.

**Files:** `ui/screens/prayer/PrayerReminderSettingsScreen.kt`, `ui/viewmodels/PrayerReminderViewModel.kt`

- [ ] **Шаг 1: Реализовать**
- [ ] **Шаг 2: Проверить** — включить напоминание на ближайшую минуту, свернуть приложение, дождаться уведомления; текст уведомления **общий, без количества просьб**; после срабатывания следующий будильник взведён на завтра.
- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "feat: add prayer reminder settings with runtime notification permission"
```

---

## Завершение фазы

- [ ] **Прогнать всё**

Run: `./gradlew test && ./gradlew assembleDebug`
Expected: зелёные тесты (17 новых: 8 + 5 + 4), успешная сборка.

- [ ] **Проверить миграцию на реальном апгрейде**

Установить предыдущую версию приложения, создать заметки и папки, поставить сборку с фазой 14 поверх (`./gradlew installDebug` без удаления). Заметки, папки и подсветки стихов должны остаться на месте.

- [ ] **Проверить продуктовые ограничения** по списку в начале файла — особенно отсутствие серий, «просрочек» и попадания молитв в общий поиск.

- [ ] **Обновить документацию:** `docs/plan.md`, `docs/progress.md`, `CHANGELOG.md`, и раздел «Структура проекта» в `CLAUDE.md` (новый пакет `data/prayer/`, экраны `ui/screens/prayer/`, четвёртая вкладка).

- [ ] **Ревью субагентом.**
