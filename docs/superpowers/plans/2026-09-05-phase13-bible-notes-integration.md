# Фаза 13 — Связка Библия ↔ заметки

> **For agentic workers:** REQUIRED SUB-SKILL: используйте `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, чтобы выполнять этот план задача-за-задачей. Шаги размечены чекбоксами (`- [ ]`).

**Goal:** Связать читалку Библии и заметки в обе стороны: показать у каждого стиха, сколько заметок на него ссылается; превратить шторку стиха в полную главу, где тап по стиху переписывает ссылку прямо в тексте заметки; дать возможность сохранить выделенные стихи в новую или существующую заметку.

**Architecture:** Три чистые функции без зависимостей от Android (`NoteVerseIndexService`, `VerseSnippetBuilder`, `NoteContentWriter`) пишутся первыми по TDD и покрываются pure-JVM тестами. Поверх них строится UI. Центральный принцип, унаследованный от iOS: **текст заметки — единственный источник истины**; ссылки всегда заново выводятся парсером, поэтому правка ссылки из шторки — это обычная текстовая замена участка, а не операция над спанами.

**Tech Stack:** Kotlin 2.3.21, Compose (BOM 2026.05.01), Room 2.8.4, Koin 4.2.1, `AndroidView` + `EditText` + `Spannable`, JUnit 4.13.2.

**Предусловие:** закрыта задача 12.1 (`coveredVerses`, `formatVerseSpec`, `replacementText`).

**Порядок:** 13.1 → 13.2 → 13.3 → 13.4 → 13.5 → 13.6 → 13.7 → 13.8

---

## Структура файлов

**Создаются:**
- `app/src/main/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexService.kt` — индекс «стих → заметки»
- `app/src/main/java/ru/edgarakert/biblenote/data/bible/VerseSnippetBuilder.kt` — текст стихов для вставки в заметку
- `app/src/main/java/ru/edgarakert/biblenote/data/NoteContentWriter.kt` — единственное место записи текста в заметку извне редактора
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/VerseNotesSheet.kt` — список заметок, ссылающихся на стих
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteDestinationPicker.kt` — выбор «куда сохранить»
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/SaveVersesToNoteSheet.kt` — шторка сохранения стихов
- `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/SaveVersesToNoteViewModel.kt`
- тесты: `NoteVerseIndexServiceTest.kt`, `VerseSnippetBuilderTest.kt`, `NoteContentWriterTest.kt`

**Изменяются:**
- `data/db/NoteDao.kt`, `data/NoteRepository.kt` — запрос всех заметок
- `ui/components/BibleVerse.kt` — бейдж с количеством упоминаний
- `ui/components/BibleVerseSheet.kt` — полная глава + выбор стихов + панель выбора
- `ui/viewmodels/BibleVerseSheetViewModel.kt` — загрузка всей главы, состояние выбора
- `ui/components/BibleEditText.kt` — живой диапазон спана при тапе + применение внешней правки
- `ui/screens/editor/NoteEditorScreen.kt` — прокидывание правки в редактор
- `ui/screens/bible/BibleReaderScreen.kt` — бейджи, кнопка «в заметку», тост
- `ui/viewmodels/BibleReaderViewModel.kt` — индекс упоминаний
- `res/values/strings.xml`, `res/values-ru/strings.xml`

---

## Задача 13.1: Запрос всех заметок

Индекс упоминаний обязан пройти по всему корпусу заметок. `NoteDao` намеренно не имеет такого запроса (риск **R4**): есть только `observeRootNotes()` (лишь корневые), `observeNotesInFolder(folderId)` и `search(query)`.

Пустые заметки отфильтровываем так же, как это делают существующие запросы: ссылок в них всё равно нет, а в списках они скрыты.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt`

- [ ] **Шаг 1: Добавить запрос**

В `NoteDao.kt`:

```kotlin
    /**
     * Все заметки во всех папках — нужен индексу упоминаний стихов.
     * Пустые заметки отфильтрованы, как и в остальных запросах: ссылок в них нет.
     * Порядок детерминированный, чтобы список заметок у стиха не «прыгал» между пересчётами.
     */
    @Query("SELECT * FROM notes WHERE NOT (title = '' AND content = '') ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<Note>>
```

В `NoteRepository.kt`:

```kotlin
    fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAllNotes()
```

- [ ] **Шаг 2: Собрать**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Шаг 3: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/db/NoteDao.kt app/src/main/java/ru/edgarakert/biblenote/data/NoteRepository.kt
git commit -m "feat: add observeAllNotes for the verse reference index"
```

---

## Задача 13.2: `NoteVerseIndexService`

Чистая функция: список заметок + книга/глава/номера стихов главы → карта «номер стиха → заметки, которые на него ссылаются».

Три правила из iOS, каждое зафиксировано тестом:
- ссылка на **всю главу** (`Быт 1`) индексируется на **все** стихи главы;
- заметка попадает под стих **один раз**, даже если ссылается на него несколько раз (внутри заметки используется множество);
- ссылка на другую главу или книгу не даёт ничего.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexService.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexServiceTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

Создать `app/src/test/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexServiceTest.kt`:

```kotlin
package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note

class NoteVerseIndexServiceTest {

    private val parser = BibleReferenceParser()
    private val genesis1Verses = (1..31).toList()

    private fun note(id: Long, content: String) = Note(id = id, title = "", content = content)

    private fun index(notes: List<Note>) =
        NoteVerseIndexService.index(
            notes = notes,
            bookId = 1,
            chapter = 1,
            verseNumbers = genesis1Verses,
            parser = parser
        )

    @Test
    fun `a single-verse reference indexes only that verse`() {
        val result = index(listOf(note(1, "Мысль о Быт 1:1 и всём начале")))
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertNull(result[2])
    }

    @Test
    fun `a range reference indexes every verse in the range`() {
        val result = index(listOf(note(1, "Быт 1:1-3")))
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertEquals(listOf(1L), result[2]?.map { it.id })
        assertEquals(listOf(1L), result[3]?.map { it.id })
        assertNull(result[4])
    }

    @Test
    fun `a whole-chapter reference indexes every verse of the chapter`() {
        val result = index(listOf(note(1, "Бытие 1 целиком")))
        assertEquals(31, result.size)
        assertEquals(listOf(1L), result[1]?.map { it.id })
        assertEquals(listOf(1L), result[31]?.map { it.id })
    }

    @Test
    fun `a comma reference indexes exactly the listed verses`() {
        val result = index(listOf(note(1, "Быт 1:2,5")))
        assertEquals(listOf(1L), result[2]?.map { it.id })
        assertEquals(listOf(1L), result[5]?.map { it.id })
        assertNull(result[3])
    }

    @Test
    fun `two notes on the same verse both appear`() {
        val result = index(listOf(note(1, "Быт 1:1"), note(2, "Ещё раз Быт 1:1")))
        assertEquals(listOf(1L, 2L), result[1]?.map { it.id })
    }

    @Test
    fun `a note referencing the same verse twice is counted once`() {
        val result = index(listOf(note(1, "Быт 1:1 и снова Быт 1:1")))
        assertEquals(1, result[1]?.size)
    }

    @Test
    fun `a reference to another chapter yields an empty index`() {
        assertTrue(index(listOf(note(1, "Быт 2:1"))).isEmpty())
    }

    @Test
    fun `a note without references yields an empty index`() {
        assertTrue(index(listOf(note(1, "Просто текст без ссылок"))).isEmpty())
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*NoteVerseIndexServiceTest*"`
Expected: FAIL — `Unresolved reference: NoteVerseIndexService`.

- [ ] **Шаг 3: Реализовать сервис**

Создать `app/src/main/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexService.kt`:

```kotlin
package ru.edgarakert.biblenote.data.bible

import ru.edgarakert.biblenote.data.db.Note

/**
 * Сопоставляет номера стихов главы с заметками, которые на них ссылаются,
 * прогоняя по тексту заметок тот же парсер, что подсвечивает ссылки в редакторе.
 *
 * Чистая функция без обращений к БД — вызывающий сам грузит заметки и номера стихов главы.
 */
object NoteVerseIndexService {

    fun index(
        notes: List<Note>,
        bookId: Int,
        chapter: Int,
        verseNumbers: List<Int>,
        parser: BibleReferenceParser,
    ): Map<Int, List<Note>> {
        val result = mutableMapOf<Int, MutableList<Note>>()

        for (note in notes) {
            val refs = parser.parse(note.content)
                .filter { it.bookId == bookId && it.chapter == chapter }
            if (refs.isEmpty()) continue

            // Множество: заметка попадает под стих один раз, сколько бы ссылок на него ни было.
            val matched = mutableSetOf<Int>()
            for (ref in refs) {
                if (ref.isWholeChapter) {
                    // Ссылка на главу целиком относится ко всем её стихам.
                    matched += verseNumbers
                } else {
                    matched += ref.coveredVerses
                }
            }

            for (verse in matched) {
                result.getOrPut(verse) { mutableListOf() } += note
            }
        }

        return result
    }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*NoteVerseIndexServiceTest*"`
Expected: PASS, 8 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexService.kt app/src/test/java/ru/edgarakert/biblenote/data/bible/NoteVerseIndexServiceTest.kt
git commit -m "feat: add NoteVerseIndexService mapping chapter verses to referencing notes"
```

---

## Задача 13.3: Бейдж упоминаний у стиха и шторка со списком заметок

`BibleVerse` сейчас — `Row` без слота под бейдж и без `onClick` (клик прокидывается вызывающим через `modifier`). Добавляем ведущий слот справа. Тап **по бейджу** открывает список заметок; тап по остальной строке по-прежнему переключает выделение стиха — бейдж не должен перехватывать этот жест.

Индекс считается на `Dispatchers.Default` (по всему корпусу заметок гоняется регулярка из ~360 альтернатив) и пересчитывается при смене главы, перевода и при любой эмиссии потока заметок — последнее и делает так, что бейдж появляется сразу после сохранения стихов в заметку.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleVerse.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/BibleReaderViewModel.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/bible/BibleReaderScreen.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/VerseNotesSheet.kt`
- Modify: `res/values/strings.xml`, `res/values-ru/strings.xml`

- [ ] **Шаг 1: Добавить строки**

`values/strings.xml`:

```xml
    <string name="verse_notes_title">Notes on this verse</string>
```

`values-ru/strings.xml`:

```xml
    <string name="verse_notes_title">Заметки об этом стихе</string>
```

- [ ] **Шаг 2: Добавить бейдж в `BibleVerse`**

Новые параметры (оба со значениями по умолчанию, чтобы существующие вызовы не сломались):

```kotlin
@Composable
fun BibleVerse(
    verseNumber: Int,
    text: String,
    highlightColor: HighlightColor?,
    verseScale: Float,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    noteCount: Int = 0,
    onNoteBadgeClick: (() -> Unit)? = null,
)
```

В конец внутреннего `Row`, после текста стиха:

```kotlin
    if (noteCount > 0 && onNoteBadgeClick != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .padding(end = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onNoteBadgeClick)
                .padding(horizontal = 4.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((10f * verseScale).dp)
            )
            Text(
                text = "$noteCount",
                color = MaterialTheme.colorScheme.primary,
                fontSize = (11f * verseScale).sp
            )
        }
    }
```

Размеры внутри бейджа умножаются на `verseScale` — иначе при масштабе 1.4 бейдж выглядит непропорционально мелким.

Заодно уменьшить фиксированный `padding(end = 24.dp)` у текста стиха до `end = if (noteCount > 0) 0.dp else 24.dp`, чтобы бейдж не наезжал на текст.

- [ ] **Шаг 3: Считать индекс во ViewModel читалки**

В `BibleReaderViewModel` добавить в `UiState` поле `noteCounts: Map<Int, Int> = emptyMap()` и приватное `verseNotes: Map<Int, List<Note>> = emptyMap()`.

Собрать индекс из потока заметок и текущей главы:

```kotlin
    private fun observeNoteIndex() {
        viewModelScope.launch {
            combine(
                repository.observeAllNotes(),
                _uiState.map { it.bookId to it.chapter }.distinctUntilChanged()
            ) { notes, (bookId, chapter) -> Triple(notes, bookId, chapter) }
                .collectLatest { (notes, bookId, chapter) ->
                    val verseNumbers = _uiState.value.verses.map { it.first }
                    // Регулярка парсера прогоняется по всему корпусу заметок — не на главном потоке.
                    val index = withContext(Dispatchers.Default) {
                        NoteVerseIndexService.index(notes, bookId, chapter, verseNumbers, parser)
                    }
                    verseNotes = index
                    _uiState.update { it.copy(noteCounts = index.mapValues { (_, v) -> v.size }) }
                }
        }
    }

    fun notesForVerse(verseNumber: Int): List<Note> = verseNotes[verseNumber].orEmpty()
```

Вызвать `observeNoteIndex()` из `init`. В конструктор ViewModel добавить `parser: BibleReferenceParser` и `repository: NoteRepository`, и обновить регистрацию в `di/AppModule.kt`:

```kotlin
    viewModel { BibleReaderViewModel(get(), get(), get(), get()) }
```

- [ ] **Шаг 4: Создать шторку со списком заметок**

Создать `app/src/main/java/ru/edgarakert/biblenote/ui/components/VerseNotesSheet.kt` — `ModalBottomSheet` с заголовком `"$bookName $chapter:$verseNumber"` и списком строк. Каждая строка: заголовок заметки (или `R.string.notes_untitled`, если пустой) и, если контент непуст, первые две строки контента вторичным цветом. Тап по строке вызывает `onOpenNote(noteId)`.

В отличие от iOS, где редактор открывается **внутри** шторки, на Android проще и привычнее закрыть шторку и перейти на вкладку «Заметки» в редактор — навигация в проекте уже устроена через маршруты, а не вложенные стеки.

- [ ] **Шаг 5: Подключить в читалке**

В `BibleReaderScreen` в `VersesContent` передать в `BibleVerse`:

```kotlin
    noteCount = uiState.noteCounts[verseNum] ?: 0,
    onNoteBadgeClick = { verseNotesFor = verseNum }
```

где `var verseNotesFor by remember { mutableStateOf<Int?>(null) }`, и отрисовать `VerseNotesSheet`, когда оно не `null`.

Переход в заметку: `onOpenNote` → `navController.navigate("editor/$noteId")` через новый параметр экрана `onOpenNote: (Long) -> Unit`, прокинутый из `NavGraph.kt` с `popUpTo`/`launchSingleTop`, как это уже сделано для `bible_at`.

- [ ] **Шаг 6: Проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL. Вручную: создать заметку с текстом `Быт 1:1-3` → открыть Библию на Бытие 1 → у стихов 1, 2 и 3 появился бейдж «1» → тап по бейджу открывает шторку с этой заметкой → тап по строке открывает её в редакторе. Тап по самому стиху по-прежнему выделяет его.

- [ ] **Шаг 7: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/ app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt
git commit -m "feat: show how many notes reference each verse in the Bible reader"
```

---

## Задача 13.4: Полная глава в шторке стиха

Сейчас шторка грузит только те стихи, что названы в ссылке. Нужно грузить главу целиком и скроллиться к первому стиху ссылки — чтобы ссылку `Быт 2` можно было читать с 14-го стиха, не уходя из заметки.

Тонкости, найденные в iOS:
- предвыбранные стихи = `reference.coveredVerses`, поэтому **ссылка на всю главу открывается с пустым выбором**;
- при пустом выборе **ничего не приглушается**;
- заголовок пересчитывается от **живого выбора**, а не от исходной ссылки, и использует **en dash**;
- скролл выполняется **один раз за появление** и только после того, как список отрисован — иначе он молча не срабатывает;
- выделение рисуется **полосой слева шириной 5 dp**, а фон строки остаётся под подсветку стиха (в iOS специально убрали фоновую заливку выделения, чтобы не терять цвет подсветки).

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/BibleVerseSheetViewModel.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleVerseSheet.kt`
- Modify: `res/values/strings.xml`, `res/values-ru/strings.xml`

- [ ] **Шаг 1: Добавить строку-подсказку**

`values/strings.xml`:

```xml
    <string name="verse_select_hint">Tap a verse — the link in the note updates</string>
```

`values-ru/strings.xml`:

```xml
    <string name="verse_select_hint">Нажмите на стих — ссылка в заметке обновится</string>
```

- [ ] **Шаг 2: Грузить главу целиком и держать выбор**

В `BibleVerseSheetViewModel`:

```kotlin
    data class UiState(
        val title: String = "",
        val bookName: String = "",
        val verses: List<Pair<Int, String>> = emptyList(),
        val highlights: Map<Int, HighlightColor> = emptyMap(),
        val selectedVerses: Set<Int> = emptySet(),
        val enabledTranslations: List<String> = emptyList(),
        val selectedTranslation: String = "",
        val verseScale: Float = 1f,
        val isLoading: Boolean = true,
    )
```

Загрузка — через уже существующий `fetchVersesWithHighlights(bookId, chapter, translation)`, который отдаёт стихи и подсветки одним джойном:

```kotlin
    private fun loadChapter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val translation = _uiState.value.selectedTranslation
            val rows = bibleService.fetchVersesWithHighlights(
                reference.bookId, reference.chapter, translation
            )
            val bookName = bibleService.fetchBookName(reference.bookId, translation)
                ?: "${reference.bookId}"
            _uiState.update { state ->
                state.copy(
                    bookName = bookName,
                    verses = rows.map { it.first to it.second },
                    highlights = rows.mapNotNull { row -> row.third?.let { row.first to it } }.toMap(),
                    title = buildTitle(bookName, state.selectedVerses),
                    isLoading = false
                )
            }
        }
    }

    /** Заголовок отражает живой выбор, а не исходную ссылку. En dash — только для показа. */
    private fun buildTitle(bookName: String, selected: Set<Int>): String {
        val spec = BibleReference.formatVerseSpec(selected.toList(), rangeSeparator = "–")
        return if (spec.isEmpty()) "$bookName ${reference.chapter}"
        else "$bookName ${reference.chapter}:$spec"
    }
```

Начальный выбор — в `init`: `_uiState.update { it.copy(selectedVerses = reference.coveredVerses.toSet()) }`.

Переключение стиха отдаёт наружу отсортированный список — каждый тап немедленно переписывает ссылку, кнопки «Применить» нет:

```kotlin
    fun toggleVerse(verseNumber: Int): List<Int> {
        val next = _uiState.value.selectedVerses.toMutableSet().apply {
            if (!add(verseNumber)) remove(verseNumber)
        }
        _uiState.update { it.copy(selectedVerses = next, title = buildTitle(it.bookName, next)) }
        return next.sorted()
    }

    /** Стих, на котором открывается шторка: первый из указанных в ссылке. */
    val scrollTarget: Int? get() = _uiState.value.selectedVerses.minOrNull()
```

- [ ] **Шаг 3: Переписать шторку**

В `BibleVerseSheet` добавить параметр:

```kotlin
    onVersesChanged: ((List<Int>) -> Unit)? = null,
```

**Непустой `onVersesChanged` — это и есть признак редактируемости** (`val isEditable = onVersesChanged != null`), ровно как в iOS.

Изменения в теле:

1. Завести `val listState = rememberLazyListState()` и вынести кнопку «Открыть главу» **из** `LazyColumn` в закреплённый низ — иначе на длинной главе (Пс 118 — 176 стихов) она уезжает вниз на 176 строк.
2. Скролл к цели — один раз за появление, после того как список получил элементы:

```kotlin
var didScroll by rememberSaveable { mutableStateOf(false) }
LaunchedEffect(uiState.verses.size) {
    if (didScroll || uiState.verses.isEmpty()) return@LaunchedEffect
    val target = viewModel.scrollTarget ?: return@LaunchedEffect
    val position = uiState.verses.indexOfFirst { it.first == target }
    if (position < 0) return@LaunchedEffect
    listState.scrollToItem(position)
    didScroll = true
}
```

3. Каждый стих — кликабельный только когда `isEditable`, с полосой выделения слева:

```kotlin
val isSelected = verseNum in uiState.selectedVerses
val isDimmed = isEditable && uiState.selectedVerses.isNotEmpty() && !isSelected

Row(Modifier.height(IntrinsicSize.Min)) {
    Box(
        Modifier
            .width(5.dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
    )
    BibleVerse(
        verseNumber = verseNum,
        text = verseText,
        highlightColor = uiState.highlights[verseNum],
        verseScale = uiState.verseScale,
        modifier = Modifier
            .alpha(if (isDimmed) 0.55f else 1f)
            .then(
                if (isEditable) Modifier.clickable {
                    onVersesChanged?.invoke(viewModel.toggleVerse(verseNum))
                } else Modifier
            )
    )
}
```

Обратите внимание: `isSelected` в `BibleVerse` **не передаётся** — иначе фон выделения перекроет цвет подсветки стиха. Выделение показывает полоса слева.

4. Панель выбора внизу, когда `isEditable` и есть стихи: заголовок `uiState.title` цветом `primary`, справа — количество через существующий плюрал `pluralStringResource(R.plurals.bible_verse_count, n, n)`, второй строкой — `stringResource(R.string.verse_select_hint)` вторичным цветом. Контейнер: `colorScheme.surface`, `RoundedCornerShape(20.dp)`.

- [ ] **Шаг 4: Проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL. Вручную: заметка с текстом `Быт 2:14` → тап по ссылке → шторка открыта на **всей главе 2**, проскроллена к 14-му стиху, стих 14 отмечен полосой, остальные приглушены. Заметка с текстом `Быт 2` → шторка открыта сверху, ничего не выделено, ничего не приглушено.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleVerseSheet.kt app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/BibleVerseSheetViewModel.kt app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
git commit -m "feat: show the full chapter in the verse sheet and scroll to the referenced verse"
```

---

## Задача 13.5: Правка ссылки в заметке из шторки

Самая тонкая задача фазы. Тап по стиху в шторке должен немедленно переписать ссылку в тексте заметки.

Три правила, без которых это не работает:

1. **Диапазон берётся живым, в момент тапа.** Спаны двигаются, пока пользователь печатает, поэтому `startIndex`/`endIndex`, вычисленные при разборе, устаревают. В `ClickableSpan.onClick` надо читать `editable.getSpanStart(this)` / `getSpanEnd(this)`.
2. **Правка применяется как настоящее редактирование текста** — `editable.replace(start, end, text)`, а не пересборкой всего `Spannable`. Только так изменение попадает в стек отмены и не сбрасывает позицию курсора.
3. **После каждой правки цель смещается.** Длина замены меняется (`Быт 2` → `Быт 2:14` → `Быт 2:14-16`), поэтому следующий тап должен целиться в **новый** диапазон: `(start, start + text.length)`.

Правка должна применяться ровно один раз — для этого у неё есть токен, и повторная композиция не применяет её заново.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleEditText.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt`

- [ ] **Шаг 1: Отдавать живой диапазон при тапе**

В `BibleEditText.kt`, в обработчике касания, где сейчас вызывается `onReferenceTappedState.value(spans[0].reference)`:

```kotlin
val span = spans[0]
val editable = view.text ?: return@setOnTouchListener false
val spanStart = editable.getSpanStart(span)
val spanEnd = editable.getSpanEnd(span)
if (spanStart < 0 || spanEnd > editable.length) return@setOnTouchListener false

// Диапазон и текст берём из живого Editable, а не из момента разбора:
// пока пользователь печатал, спан мог сдвинуться.
onReferenceTappedState.value(
    span.reference.copy(
        startIndex = spanStart,
        endIndex = spanEnd,
        displayText = editable.subSequence(spanStart, spanEnd).toString()
    )
)
```

- [ ] **Шаг 2: Принимать внешнюю правку**

Добавить тип и параметры:

```kotlin
/** Одноразовая правка текста извне редактора. token отсекает повторное применение. */
data class PendingEdit(val token: Long, val start: Int, val end: Int, val text: String)
```

```kotlin
@Composable
fun BibleEditText(
    // ... существующие параметры
    pendingEdit: PendingEdit? = null,
    onPendingEditApplied: (Long) -> Unit = {},
)
```

В `update`-блоке `AndroidView` — **до** ветки, которая сравнивает `view.text` с `text` и делает `setText` (иначе правка потеряется на полной перезаписи текста):

```kotlin
    val edit = pendingEdit
    if (edit != null && edit.token != lastAppliedToken.intValue.toLong()) {
        val editable = view.text
        if (editable != null && edit.start >= 0 && edit.end <= editable.length && edit.start <= edit.end) {
            view.isProgrammatic = true
            // replace, а не пересборка Spannable: правка попадает в стек отмены
            // и не сбрасывает позицию курсора.
            editable.replace(edit.start, edit.end, edit.text)
            view.isProgrammatic = false

            applyHighlighting(view, parser, amberArgb, inkArgb)
            onTextChangedState.value(editable.toString())
        }
        lastAppliedToken.longValue = edit.token
        onPendingEditApplied(edit.token)
        return@AndroidView
    }
```

где `val lastAppliedToken = remember { mutableLongStateOf(-1L) }`.

- [ ] **Шаг 3: Связать шторку и редактор**

В `NoteEditorScreen.kt`:

```kotlin
var tappedReference by remember { mutableStateOf<BibleReference?>(null) }
var activeRange by remember { mutableStateOf<IntRange?>(null) }
var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
var editToken by remember { mutableLongStateOf(0L) }

tappedReference?.let { ref ->
    BibleVerseSheet(
        reference = ref,
        onDismiss = { tappedReference = null; activeRange = null },
        onOpenChapter = { r -> tappedReference = null; activeRange = null; onOpenChapter(r) },
        onVersesChanged = { verses ->
            val range = activeRange ?: (ref.startIndex until ref.endIndex)
            val newText = BibleReference.replacementText(ref.displayText, verses)
            editToken += 1
            pendingEdit = PendingEdit(editToken, range.first, range.last + 1, newText)
            // Длина замены меняется с каждым тапом — следующая правка целится в новый диапазон.
            activeRange = range.first until (range.first + newText.length)
        }
    )
}
```

и при установке `tappedReference` из `onReferenceTapped` сразу писать `activeRange = it.startIndex until it.endIndex`.

Передать в `BibleEditText`: `pendingEdit = pendingEdit`, `onPendingEditApplied = { pendingEdit = null }`.

Важно: `ref.displayText` остаётся **исходным** на всю сессию шторки — это нормально и так задумано, потому что `replacementText` использует только часть до первого двоеточия.

- [ ] **Шаг 4: Проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL.

Проверить вручную:
1. Заметка с `Быт 2` → тап по ссылке → тап по стиху 14 в шторке → текст в заметке стал `Быт 2:14`; тап по 15 → `Быт 2:14-15`; тап по 20 → `Быт 2:14-15,20`; снять все → снова `Быт 2`.
2. Закрыть шторку → ссылка в заметке по-прежнему подсвечена и кликабельна (то есть записанный текст успешно распарсился обратно — проверка ASCII-дефиса из задачи 12.1).
3. Написанное сокращение сохраняется: `быт.3:2` после выбора стиха 7 становится `быт.3:7`, а не `Быт 3:7`.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleEditText.kt app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt
git commit -m "feat: rewrite the reference in the note when verses are tapped in the sheet"
```

---

## Задача 13.6: `VerseSnippetBuilder`

Превращает выделение стихов в читалке в обычный текст для заметки. Ссылка пишется **обычным текстом**, а не спаном: редактор заново разбирает заметку при каждом рендере и подсвечивает ссылки сам. Все названия книг из `bible.sqlite` являются алиасами парсера, поэтому подстановка имени книги из читалки безопасна в обеих локалях.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/bible/VerseSnippetBuilder.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/bible/VerseSnippetBuilderTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseSnippetBuilderTest {

    private fun verse(n: Int, text: String) = VerseSnippetBuilder.Verse(n, text)

    @Test
    fun `reference of a single verse`() {
        assertEquals("Иоанна 3:16", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16)))
    }

    @Test
    fun `reference of a consecutive range`() {
        assertEquals("Иоанна 3:16-17", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16, 17)))
    }

    @Test
    fun `reference of a disjoint selection`() {
        assertEquals("Иоанна 3:16-17,20", VerseSnippetBuilder.reference("Иоанна", 3, listOf(16, 17, 20)))
    }

    @Test
    fun `reference of an empty selection degrades to the chapter`() {
        assertEquals("Иоанна 3", VerseSnippetBuilder.reference("Иоанна", 3, emptyList()))
    }

    @Test
    fun `build puts the reference, a blank line, then numbered verses`() {
        val result = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(16, "Ибо так возлюбил Бог мир"), verse(17, "Ибо не послал Бог Сына"))
        )
        assertEquals(
            "Иоанна 3:16-17\n\n16 Ибо так возлюбил Бог мир\n17 Ибо не послал Бог Сына",
            result
        )
    }

    @Test
    fun `build orders verses by number and the header follows`() {
        val result = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(20, "Всякий, делающий злое"), verse(16, "Ибо так возлюбил Бог мир"))
        )
        assertEquals(
            "Иоанна 3:16,20\n\n16 Ибо так возлюбил Бог мир\n20 Всякий, делающий злое",
            result
        )
    }

    @Test
    fun `build trims surrounding whitespace of verse text`() {
        val result = VerseSnippetBuilder.build("Иоанна", 3, listOf(verse(16, "  Ибо так возлюбил Бог мир\n")))
        assertEquals("Иоанна 3:16\n\n16 Ибо так возлюбил Бог мир", result)
    }

    @Test
    fun `build of an empty verse list is just the chapter reference`() {
        assertEquals("Иоанна 3", VerseSnippetBuilder.build("Иоанна", 3, emptyList()))
    }

    @Test
    fun `the built snippet re-parses into a tappable reference`() {
        val snippet = VerseSnippetBuilder.build(
            "Иоанна", 3,
            listOf(verse(16, "текст"), verse(17, "текст"), verse(20, "текст"))
        )
        val parsed = BibleReferenceParser().parse(snippet)
        assertEquals(43, parsed[0].bookId)
        assertEquals(3, parsed[0].chapter)
        assertEquals(listOf(16, 17, 20), parsed[0].coveredVerses)
    }
}
```

Последний тест — самый важный: если он падает, вставленные стихи перестают быть кликабельными ссылками.

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*VerseSnippetBuilderTest*"`
Expected: FAIL — `Unresolved reference: VerseSnippetBuilder`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data.bible

/**
 * Превращает выделение стихов в читалке в обычный текст для заметки.
 * Ничего не знает о БД — принимает уже загруженные стихи, поэтому тестируется без SQLite.
 *
 * Ссылка пишется обычным текстом, а не спаном: редактор заново разбирает заметку
 * при каждом рендере и подсвечивает ссылки сам.
 */
object VerseSnippetBuilder {

    data class Verse(val number: Int, val text: String)

    /** "Иоанна 3:16-17,20". Пустой выбор схлопывается в ссылку на главу. */
    fun reference(bookName: String, chapter: Int, verseNumbers: List<Int>): String {
        // ASCII-дефис: парсер не понимает en dash.
        val spec = BibleReference.formatVerseSpec(verseNumbers)
        return if (spec.isEmpty()) "$bookName $chapter" else "$bookName $chapter:$spec"
    }

    /** Ссылка отдельной строкой, пустая строка, затем пронумерованные стихи. */
    fun build(bookName: String, chapter: Int, verses: List<Verse>): String {
        val ordered = verses.sortedBy { it.number }.distinctBy { it.number }
        val header = reference(bookName, chapter, ordered.map { it.number })
        if (ordered.isEmpty()) return header

        val body = ordered.joinToString("\n") { "${it.number} ${it.text.trim()}" }
        return "$header\n\n$body"
    }
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*VerseSnippetBuilderTest*"`
Expected: PASS, 9 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/bible/VerseSnippetBuilder.kt app/src/test/java/ru/edgarakert/biblenote/data/bible/VerseSnippetBuilderTest.kt
git commit -m "feat: add VerseSnippetBuilder turning a verse selection into note text"
```

---

## Задача 13.7: `NoteContentWriter`

Единственное место, где текст попадает в заметку извне редактора.

В iOS этот класс синхронизирует две копии текста (`content` и `contentRTF`). На Android заметка пока хранит только `content`, поэтому вся ветка с разметкой схлопывается в простое дописывание.

> **Будет доработан в фазе 16.** Решение по вопросу Q1 — rich text портируем, но после паритета. Когда у заметки появится вторая копия текста с разметкой, `append` и `makeNote` должны будут писать обе (задача 16.5). Сейчас пишем простую версию: она полностью рабочая, и её тесты остаются в силе после доработки.
 Сохраняются три поведения, зафиксированные тестами iOS: разделитель `\n\n` только если заметка непуста, пустой сниппет — полный no-op, `updatedAt` обновляется.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/NoteContentWriter.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/NoteContentWriterTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note

class NoteContentWriterTest {

    @Test
    fun `appending to an empty note adds no leading blank line`() {
        val note = Note(id = 1, title = "", content = "")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Иоанна 3:16", result.content)
    }

    @Test
    fun `appending to a non-empty note separates with a blank line`() {
        val note = Note(id = 1, title = "", content = "Старая мысль")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals("Старая мысль\n\nИоанна 3:16", result.content)
    }

    @Test
    fun `appending bumps updatedAt`() {
        val note = Note(id = 1, title = "", content = "Текст", updatedAt = 1000L)
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        assertEquals(5000L, result.updatedAt)
    }

    @Test
    fun `appending an empty snippet changes nothing`() {
        val note = Note(id = 1, title = "Заголовок", content = "Текст", updatedAt = 1000L)
        assertEquals(note, NoteContentWriter.append("", note, now = 5000L))
    }

    @Test
    fun `appending a blank snippet changes nothing`() {
        val note = Note(id = 1, title = "", content = "Текст", updatedAt = 1000L)
        assertEquals(note, NoteContentWriter.append("   \n ", note, now = 5000L))
    }

    @Test
    fun `makeNote stores the title and the snippet`() {
        val note = NoteContentWriter.makeNote("Иоанна 3:16\n\n16 текст", "Иоанна 3:16", folderId = null, now = 7000L)
        assertEquals("Иоанна 3:16", note.title)
        assertEquals("Иоанна 3:16\n\n16 текст", note.content)
        assertEquals(null, note.folderId)
        assertTrue(note.id == 0L)
    }

    @Test
    fun `makeNote keeps the folder it was given`() {
        val note = NoteContentWriter.makeNote("текст", "Заголовок", folderId = 42L, now = 7000L)
        assertEquals(42L, note.folderId)
    }
}
```

- [ ] **Шаг 2: Запустить тест и убедиться, что он падает**

Run: `./gradlew testDebugUnitTest --tests "*NoteContentWriterTest*"`
Expected: FAIL — `Unresolved reference: NoteContentWriter`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data

import ru.edgarakert.biblenote.data.db.Note

/**
 * Единственное место, где текст попадает в заметку извне редактора.
 * Чистые функции: возвращают новую Note, ничего не сохраняют — сохранение делает вызывающий.
 */
object NoteContentWriter {

    /** Дописывает сниппет в конец. Пустой сниппет — no-op, заметка возвращается как есть. */
    fun append(snippet: String, note: Note, now: Long = System.currentTimeMillis()): Note {
        if (snippet.isBlank()) return note

        val separator = if (note.content.isEmpty()) "" else "\n\n"
        return note.copy(
            content = note.content + separator + snippet,
            updatedAt = now
        )
    }

    /** Новая заметка со сниппетом в теле. Заголовок вызывающий формирует сам (обычно это ссылка). */
    fun makeNote(
        snippet: String,
        title: String,
        folderId: Long? = null,
        now: Long = System.currentTimeMillis(),
    ): Note = Note(
        title = title,
        content = snippet,
        folderId = folderId,
        createdAt = now,
        updatedAt = now
    )
}
```

- [ ] **Шаг 4: Запустить тест и убедиться, что он проходит**

Run: `./gradlew testDebugUnitTest --tests "*NoteContentWriterTest*"`
Expected: PASS, 7 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/NoteContentWriter.kt app/src/test/java/ru/edgarakert/biblenote/data/NoteContentWriterTest.kt
git commit -m "feat: add NoteContentWriter for writing verses into notes"
```

---

## Задача 13.8: Сохранение стихов в заметку из читалки

Собирает всё вместе. Цепочка: выделить стихи в читалке → «в заметку» → шторка с готовым текстом и выбором назначения → сохранить → тост с кнопкой «Открыть».

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/NoteDestinationPicker.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/SaveVersesToNoteSheet.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/viewmodels/SaveVersesToNoteViewModel.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/bible/BibleReaderScreen.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt`
- Modify: `res/values/strings.xml`, `res/values-ru/strings.xml`

- [ ] **Шаг 1: Добавить строки**

`values/strings.xml`:

```xml
    <string name="verse_action_save_to_note">Add to Note</string>
    <string name="verse_save_title">Verses to Note</string>
    <string name="verse_save_text">Note text</string>
    <string name="verse_save_added_to">Added to \"%1$s\"</string>
    <string name="verse_save_open">Open</string>
    <string name="share_save_to">Save to</string>
    <string name="share_folders_section">Folders</string>
    <string name="share_new_note_in">New Note in \"%1$s\"</string>
```

`values-ru/strings.xml`:

```xml
    <string name="verse_action_save_to_note">В заметку</string>
    <string name="verse_save_title">Стихи в заметку</string>
    <string name="verse_save_text">Текст заметки</string>
    <string name="verse_save_added_to">Добавлено в «%1$s»</string>
    <string name="verse_save_open">Открыть</string>
    <string name="share_save_to">Сохранить в</string>
    <string name="share_folders_section">Папки</string>
    <string name="share_new_note_in">Новая заметка в «%1$s»</string>
```

- [ ] **Шаг 2: Пикер назначения**

Создать `NoteDestinationPicker.kt`. Модель назначения носит **только идентификаторы**, объекты разрешает вызывающий:

```kotlin
data class NoteDestination(val folderId: Long? = null, val noteId: Long? = null) {
    val isNewNote: Boolean get() = noteId == null
}
```

Экран показывает один уровень дерева и три секции:
1. **«Новая заметка здесь»** — `stringResource(R.string.share_new_note_in, locationName)`, галочка когда `destination.noteId == null && destination.folderId == locationFolderId`. Тап выбирает назначение и **возвращает к шторке сохранения**, а не углубляется.
2. **`share_folders_section`** — подпапки текущего уровня, отсортированные по имени; тап углубляется на уровень.
3. **`notes_title`** — заметки текущего уровня в порядке `updatedAt DESC`; строка показывает заголовок (или `notes_untitled`) и первые 50 символов контента; галочка у выбранной.

- [ ] **Шаг 3: Шторка сохранения**

Создать `SaveVersesToNoteSheet.kt` — `ModalBottomSheet` с:
- заголовком `verse_save_title`, кнопкой «Отмена» и кнопкой `common_save`, **заблокированной пока текст пуст после `trim()`**;
- многострочным `TextField` (`minLines = 5`) под заголовком `verse_save_text`, **предзаполненным** результатом `VerseSnippetBuilder.build(...)` — пользователь может урезать его до одной ссылки, просто удалив лишнее;
- строкой выбора назначения: иконка, подпись `share_save_to`, справа — текущее назначение; тап открывает пикер.

Подпись назначения:

```kotlin
val destinationLabel = when {
    destinationNote != null -> destinationNote.title.ifEmpty { stringResource(R.string.notes_untitled) }
    destinationFolder != null -> stringResource(R.string.share_new_note_in, destinationFolder.name)
    else -> stringResource(R.string.share_new_note_in, stringResource(R.string.notes_title))
}
```

Сохранение в `SaveVersesToNoteViewModel`:

```kotlin
    fun save(text: String, destination: NoteDestination, onSaved: (Long, String) -> Unit) {
        val body = text.trim()
        if (body.isEmpty()) return

        viewModelScope.launch {
            val noteId = destination.noteId
            val saved = if (noteId != null) {
                val existing = repository.getNoteById(noteId) ?: return@launch
                val updated = NoteContentWriter.append(body, existing)
                repository.saveNote(updated)
                updated.title.ifEmpty { "" } to noteId
            } else {
                // Заголовок новой заметки — сама ссылка ("Иоанна 3:16-17").
                val title = VerseSnippetBuilder.reference(bookName, chapter, verseNumbers)
                val note = NoteContentWriter.makeNote(body, title, destination.folderId)
                val id = repository.saveNote(note)
                title to id
            }
            onSaved(saved.second, saved.first)
        }
    }
```

- [ ] **Шаг 4: Кнопка в панели действий читалки**

В `VerseActionBar` добавить рядом с кнопкой копирования иконку «в заметку» с `contentDescription = stringResource(R.string.verse_action_save_to_note)`. По нажатию собрать выделенные стихи, **отсортированные по номеру**, в `List<VerseSnippetBuilder.Verse>` и открыть шторку. Если выделение пусто — ничего не делать.

- [ ] **Шаг 5: Тост после сохранения**

После сохранения: снять выделение стихов, показать внизу карточку с текстом `stringResource(R.string.verse_save_added_to, noteTitle)` и кнопкой `verse_save_open`, которая переходит в редактор этой заметки. Тост скрывается сам через **3 секунды**:

```kotlin
LaunchedEffect(toastToken) {
    if (toastToken == 0L) return@LaunchedEffect
    delay(3_000)
    savedNote = null
}
```

Индекс упоминаний пересчитывать не нужно вручную: он подписан на `observeAllNotes()` (задача 13.3), поэтому бейдж у стиха появится сразу после сохранения сам.

- [ ] **Шаг 6: Проверить на устройстве**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL.

Проверить вручную:
1. В читалке выделить стихи 16 и 17 Иоанна 3 → «в заметку» → в поле уже лежит `Иоанна 3:16-17`, пустая строка и два стиха с номерами → сохранить в корень → создана заметка с заголовком `Иоанна 3:16-17`.
2. У стихов 16 и 17 сразу появился бейдж «1».
3. Открыть созданную заметку → ссылка в первой строке подсвечена и кликабельна.
4. Ещё раз выделить стих 20 → «в заметку» → выбрать назначением **ту же заметку** → текст дописан снизу через пустую строку, новая заметка не создана.
5. Тост «Добавлено в …» исчезает сам через три секунды; кнопка «Открыть» ведёт в заметку.

- [ ] **Шаг 7: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/ app/src/main/java/ru/edgarakert/biblenote/di/AppModule.kt app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
git commit -m "feat: save selected verses from the Bible reader into a note"
```

---

## Завершение фазы

- [ ] **Прогнать всё**

Run: `./gradlew test && ./gradlew assembleDebug`
Expected: зелёные тесты (в том числе 24 старых теста парсера), успешная сборка.

- [ ] **Проверить производительность индекса**

На базе из 200+ заметок открыть читалку и пролистать 10 глав подряд. Скролл не должен дёргаться. Если дёргается — это тот случай, ради которого в роадмапе (риск **R4**) описан запасной вариант: таблица `note_verse_refs`, заполняемая при сохранении заметки, вместо пересчёта по всему корпусу.

- [ ] **Обновить документацию:** `docs/plan.md`, `docs/progress.md`, `CHANGELOG.md`.

- [ ] **Ревью субагентом** — особое внимание задаче 13.5: правка спанов и стек отмены.
