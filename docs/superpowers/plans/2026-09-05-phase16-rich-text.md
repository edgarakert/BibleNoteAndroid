# Фаза 16 — Rich text в редакторе заметок

> **For agentic workers:** REQUIRED SUB-SKILL: используйте `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, чтобы выполнять этот план задача-за-задачей. Шаги размечены чекбоксами (`- [ ]`).

**Goal:** Добавить в редактор заметок жирный, курсив и увеличенный размер шрифта с панелью форматирования — как в iOS (`555b26e`, #27).

**Architecture:** Плоский текст остаётся в `Note.content` без изменений — на нём завязаны поиск, превью в списке и разбор ссылок на стихи, и трогать это незачем. Форматирование хранится **отдельной колонкой** как список диапазонов (`жирный 0–10`, `курсив 12–20`), сериализованный в компактную строку. Кодек — чистый Kotlin без зависимостей от Android, покрывается pure-JVM тестами. `EditText` уже работает со `Spannable`, поэтому применение и снятие стилей — это работа со спанами, а сдвиг диапазонов при редактировании система делает сама.

**Tech Stack:** Kotlin 2.3.21, Compose, `AndroidView` + `EditText` + `Spannable` (`StyleSpan`, `RelativeSizeSpan`), Room 2.8.4 + автомиграция, JUnit 4.13.2.

> ⚠️ **Учесть перед началом: в репозитории уже есть незавершённая ветка `feature/add-formatting`** (3 коммита, июнь 2026) с собственной реализацией rich text: колонка `notes.contentHtml`, `RichTextSerializer`, `FormattingToolbar`. Она объявляет **версию базы 2** — тот же номер, что заняла фаза 12 под `isPinned`. Из-за этого сборка с той ветки и наши сборки несовместимы по базе: приложение падает с `Migration didn't properly handle: notes` (случай разобран 2026-09-14, см. `docs/progress.md`). Прежде чем начинать фазу 16, нужно решить: доработать ту ветку, перенеся её схему на версию 4 поверх нашей цепочки, или сделать заново по этому плану, а ветку удалить. Мержить её как есть нельзя.

**Предусловия:** закрыты фазы 12 (автомиграции Room) и 13 (`NoteContentWriter`, правка ссылок из шторки). Фаза 16 идёт **после** паритета с iOS — это решение Q1 из роадмапа.

**Порядок:** 16.1 → 16.2 → 16.3 → 16.4 → 16.5 → 16.6

---

## Почему не HTML и не RTF

iOS хранит вторую копию текста в RTF (`Note.contentRTF`). Прямой Android-аналог — `Html.toHtml` / `HtmlCompat.fromHtml` — **не подходит**, и это надо зафиксировать, чтобы не переоткрывать вопрос в процессе:

`Html.toHtml` записывает `RelativeSizeSpan` как `<span style="font-size:…">`, но штатный обратный конвертер `HtmlToSpannedConverter` разбирает из атрибута `style` только `text-align`, `color`, `background-color` и `text-decoration` — **размер шрифта он не читает**. То есть размер не пережил бы круг «сохранили → открыли». Обходить это пришлось бы собственным `TagHandler`, а это больше кода, чем собственный формат, и с худшей тестируемостью.

Собственный формат диапазонов даёт три выигрыша: кодек тестируется как чистая функция без эмулятора; формат ровно под три поддерживаемых стиля, без разбора произвольного HTML; плоский текст не дублируется, поэтому расхождение между «текстом для поиска» и «текстом для показа» физически невозможно.

---

## Что НЕ меняется

Это важно для оценки риска — фаза не трогает ничего из перечисленного:

- `Note.content` — тот же плоский текст, та же колонка. Поиск (`NoteDao.search`), превью в списке (`content.take(100)`), фильтр пустых заметок и `BibleReferenceParser` продолжают работать без правок.
- `applyHighlighting` в `BibleEditText` снимает только **свои** спаны (`ForegroundColorSpan` и `BibleClickSpan`), поэтому `StyleSpan` и `RelativeSizeSpan` она и сегодня не задевает. Причина, по которой форматирование сейчас теряется, — не она, а то, что текст возвращается во ViewModel как обычная `String`. Это и чинится в задаче 16.3.
- Механика правки ссылки из шторки (задача 13.5) построена на `editable.replace(...)`, а он сам сдвигает спаны. Переделки не требует.

---

## Структура файлов

**Создаются:**
- `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteFormatting.kt` — модель диапазонов и кодек
- `app/src/main/java/ru/edgarakert/biblenote/ui/components/FormattingToolbar.kt`
- `app/src/test/java/ru/edgarakert/biblenote/data/db/NoteFormattingCodecTest.kt`
- `app/src/test/java/ru/edgarakert/biblenote/data/NoteContentWriterFormattingTest.kt`

**Изменяются:**
- `data/db/Note.kt` — колонка `formatting`
- `data/db/AppDatabase.kt` — версия 4 + автомиграция 3→4
- `ui/components/BibleEditText.kt` — применение и извлечение диапазонов
- `ui/screens/editor/NoteEditorScreen.kt` — панель форматирования
- `ui/viewmodels/NoteEditorViewModel.kt` — сохранение форматирования
- `data/NoteContentWriter.kt` — сохранение диапазонов при дописывании
- `res/values/strings.xml`, `res/values-ru/strings.xml`

---

## Задача 16.1: Колонка форматирования и миграция 3→4

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/Note.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/db/AppDatabase.kt`
- Test: `app/src/androidTest/java/ru/edgarakert/biblenote/data/db/MigrationTest.kt` (дополняется)

- [ ] **Шаг 1: Написать падающий тест миграции**

Дописать в `MigrationTest.kt`:

```kotlin
    @Test
    fun migrate3To4_addsFormattingColumnAsNullForLegacyNotes() {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO notes (id, title, content, folderId, createdAt, updatedAt, isPinned) " +
                    "VALUES (1, 'Старая заметка', 'Быт 1:1', NULL, 1000, 2000, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true)

        db.query("SELECT content, formatting FROM notes WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Быт 1:1", cursor.getString(0))
            // null = заметка из времён до rich text, форматирования у неё нет
            assertTrue(cursor.isNull(1))
        }
    }
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: FAIL — `no such column: formatting`.

- [ ] **Шаг 3: Добавить колонку**

В `Note.kt` — последним полем:

```kotlin
    /**
     * Диапазоны форматирования, сериализованные NoteFormattingCodec.
     * null — заметка создана до появления rich text: форматирования нет, текст плоский.
     */
    val formatting: String? = null
```

Колонка nullable, поэтому `defaultValue` не нужен — Room сгенерирует автомиграцию сам.

В `AppDatabase.kt`:

```kotlin
@Database(
    entities = [Note::class, Folder::class, PrayerRequest::class, PrayerEntry::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ]
)
```

- [ ] **Шаг 4: Запустить и убедиться, что проходит**

Run: `./gradlew connectedDebugAndroidTest --tests "*MigrationTest*"`
Expected: PASS, три теста миграций. Закоммитить `app/schemas/.../4.json`.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/db/ app/schemas/ app/src/androidTest/
git commit -m "feat: add the note formatting column with auto-migration 3 to 4"
```

---

## Задача 16.2: Кодек диапазонов форматирования

Чистая функция, никаких зависимостей от Android — поэтому тестируется как обычный JVM-код.

Формат: диапазоны через `;`, поля внутри диапазона через `:`. Жирный и курсив — `b:0-10`, `i:12-20`; размер несёт коэффициент — `s1.25:22-30`. Порядок диапазонов не значим.

Кодек обязан быть **устойчив к мусору**: испорченная строка не должна ронять открытие заметки — непонятные диапазоны просто пропускаются, текст остаётся читаемым.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/db/NoteFormatting.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/db/NoteFormattingCodecTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFormattingCodecTest {

    @Test
    fun `an empty run list encodes to an empty string`() {
        assertEquals("", NoteFormattingCodec.encode(emptyList()))
    }

    @Test
    fun `an empty string decodes to an empty run list`() {
        assertEquals(emptyList<FormatRun>(), NoteFormattingCodec.decode(""))
    }

    @Test
    fun `null decodes to an empty run list`() {
        assertEquals(emptyList<FormatRun>(), NoteFormattingCodec.decode(null))
    }

    @Test
    fun `bold and italic round trip`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 0, 10),
            FormatRun(FormatType.ITALIC, 12, 20)
        )
        assertEquals("b:0-10;i:12-20", NoteFormattingCodec.encode(runs))
        assertEquals(runs, NoteFormattingCodec.decode("b:0-10;i:12-20"))
    }

    @Test
    fun `a size run carries its scale`() {
        val runs = listOf(FormatRun(FormatType.SIZE, 22, 30, scale = 1.25f))
        assertEquals("s1.25:22-30", NoteFormattingCodec.encode(runs))
        assertEquals(runs, NoteFormattingCodec.decode("s1.25:22-30"))
    }

    @Test
    fun `all three types round trip together`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 0, 4),
            FormatRun(FormatType.ITALIC, 5, 9),
            FormatRun(FormatType.SIZE, 10, 14, scale = 1.5f)
        )
        assertEquals(runs, NoteFormattingCodec.decode(NoteFormattingCodec.encode(runs)))
    }

    @Test
    fun `malformed segments are skipped rather than throwing`() {
        assertEquals(
            listOf(FormatRun(FormatType.BOLD, 0, 4), FormatRun(FormatType.ITALIC, 10, 12)),
            NoteFormattingCodec.decode("b:0-4;мусор;x:1-2;b:abc-4;s:5-6;i:10-12")
        )
    }

    @Test
    fun `runs with a non-positive length are dropped on encode`() {
        val runs = listOf(
            FormatRun(FormatType.BOLD, 5, 5),
            FormatRun(FormatType.ITALIC, 9, 4),
            FormatRun(FormatType.BOLD, 0, 3)
        )
        assertEquals("b:0-3", NoteFormattingCodec.encode(runs))
    }

    @Test
    fun `decoded runs out of the text bounds are clamped away by the caller helper`() {
        val runs = NoteFormattingCodec.decode("b:0-4;i:100-120")
        assertEquals(listOf(FormatRun(FormatType.BOLD, 0, 4)), NoteFormattingCodec.clampTo(runs, textLength = 10))
    }
}
```

Последний тест закрывает реальный сценарий: текст заметки мог быть изменён снаружи (например, дописыванием стихов), и диапазон может выйти за пределы.

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*NoteFormattingCodecTest*"`
Expected: FAIL — `Unresolved reference: NoteFormattingCodec`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data.db

enum class FormatType { BOLD, ITALIC, SIZE }

/** Диапазон форматирования в тексте заметки. end не входит в диапазон. */
data class FormatRun(
    val type: FormatType,
    val start: Int,
    val end: Int,
    val scale: Float = 1f,
)

/**
 * Сериализация форматирования в одну строку: "b:0-10;i:12-20;s1.25:22-30".
 *
 * Свой формат вместо HTML: штатный HtmlCompat.fromHtml не читает font-size обратно,
 * поэтому размер шрифта не пережил бы круг «сохранили → открыли».
 */
object NoteFormattingCodec {

    fun encode(runs: List<FormatRun>): String = runs
        .filter { it.end > it.start }
        .joinToString(";") { run ->
            val head = when (run.type) {
                FormatType.BOLD -> "b"
                FormatType.ITALIC -> "i"
                FormatType.SIZE -> "s${run.scale}"
            }
            "$head:${run.start}-${run.end}"
        }

    /** Битые диапазоны пропускаются: испорченная строка не должна ронять открытие заметки. */
    fun decode(encoded: String?): List<FormatRun> {
        if (encoded.isNullOrEmpty()) return emptyList()

        return encoded.split(';').mapNotNull { segment ->
            val parts = segment.split(':')
            if (parts.size != 2) return@mapNotNull null

            val bounds = parts[1].split('-')
            if (bounds.size != 2) return@mapNotNull null
            val start = bounds[0].toIntOrNull() ?: return@mapNotNull null
            val end = bounds[1].toIntOrNull() ?: return@mapNotNull null
            if (end <= start || start < 0) return@mapNotNull null

            val head = parts[0]
            when {
                head == "b" -> FormatRun(FormatType.BOLD, start, end)
                head == "i" -> FormatRun(FormatType.ITALIC, start, end)
                head.startsWith("s") -> {
                    val scale = head.drop(1).toFloatOrNull() ?: return@mapNotNull null
                    FormatRun(FormatType.SIZE, start, end, scale)
                }
                else -> null
            }
        }
    }

    /** Отбрасывает диапазоны, вышедшие за пределы текста, и подрезает частично вышедшие. */
    fun clampTo(runs: List<FormatRun>, textLength: Int): List<FormatRun> = runs
        .mapNotNull { run ->
            if (run.start >= textLength) return@mapNotNull null
            val end = minOf(run.end, textLength)
            if (end <= run.start) null else run.copy(end = end)
        }
}
```

- [ ] **Шаг 4: Запустить и убедиться, что проходит**

Run: `./gradlew testDebugUnitTest --tests "*NoteFormattingCodecTest*"`
Expected: PASS, 9 тестов.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/db/NoteFormatting.kt app/src/test/java/ru/edgarakert/biblenote/data/db/NoteFormattingCodecTest.kt
git commit -m "feat: add a codec for note formatting runs"
```

---

## Задача 16.3: `BibleEditText` — применение и извлечение стилей

Причина, по которой форматирование теряется сегодня: `onTextChanged` отдаёт наверх обычную `String`, а `Note.content` — плоская колонка, поэтому при следующем `setText` все `StyleSpan` исчезают. Чиним контракт: наружу уходит и текст, и диапазоны.

Извлечение делается **из живого `Editable`**: система сама двигает спаны при вводе, вставках и `replace(...)`, поэтому диапазоны всегда актуальны и пересчитывать их вручную не нужно.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleEditText.kt`

- [ ] **Шаг 1: Расширить контракт**

```kotlin
@Composable
fun BibleEditText(
    text: String,
    formatting: List<FormatRun>,
    onContentChanged: (text: String, runs: List<FormatRun>) -> Unit,
    // ... остальные параметры без изменений
)
```

Старый `onTextChanged: (String) -> Unit` заменяется на `onContentChanged`.

- [ ] **Шаг 2: Применять диапазоны при загрузке текста**

В ветке `update`, где сейчас выполняется `setText(text)` — после установки текста и **до** `applyHighlighting`:

```kotlin
private fun applyFormatting(editText: EditText, runs: List<FormatRun>) {
    val editable = editText.text ?: return

    // Снимаем только свои стилевые спаны, чтобы не задеть подсветку ссылок.
    editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
    editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { editable.removeSpan(it) }

    for (run in NoteFormattingCodec.clampTo(runs, editable.length)) {
        val span: Any = when (run.type) {
            FormatType.BOLD -> StyleSpan(Typeface.BOLD)
            FormatType.ITALIC -> StyleSpan(Typeface.ITALIC)
            FormatType.SIZE -> RelativeSizeSpan(run.scale)
        }
        // SPAN_EXCLUSIVE_INCLUSIVE: текст, дописанный вплотную к концу жирного участка,
        // продолжает быть жирным — так ведут себя привычные редакторы.
        editable.setSpan(span, run.start, run.end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
    }
}
```

- [ ] **Шаг 3: Извлекать диапазоны при изменении**

```kotlin
private fun extractFormatting(editable: Editable): List<FormatRun> {
    val runs = mutableListOf<FormatRun>()

    editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { span ->
        val type = when (span.style) {
            Typeface.BOLD -> FormatType.BOLD
            Typeface.ITALIC -> FormatType.ITALIC
            else -> return@forEach
        }
        runs += FormatRun(type, editable.getSpanStart(span), editable.getSpanEnd(span))
    }

    editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { span ->
        runs += FormatRun(
            FormatType.SIZE,
            editable.getSpanStart(span),
            editable.getSpanEnd(span),
            span.sizeChange
        )
    }

    return runs.filter { it.end > it.start }
}
```

В `TextWatcher.afterTextChanged` вместо `onTextChangedState.value(newText)` вызывать:

```kotlin
onContentChangedState.value(editable.toString(), extractFormatting(editable))
```

То же самое — в ветке применения `PendingEdit` (задача 13.5): после `editable.replace(...)` отдавать наверх и текст, и диапазоны.

- [ ] **Шаг 4: Собрать**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL после обновления вызова в `NoteEditorScreen`.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleEditText.kt
git commit -m "feat: apply and extract character formatting in the note editor field"
```

---

## Задача 16.4: Панель форматирования

Три кнопки: жирный, курсив, размер. В iOS это `FormattingToolbar` с иконками `bold`, `italic`, `textformat.size.larger`; активная кнопка подсвечивается янтарным, неактивная — серым.

Поведение:
- кнопка применяет стиль **к текущему выделению**; если выделения нет — переключает стиль «с этого места» для последующего ввода;
- подсветка кнопки отражает стиль в позиции курсора и обновляется при смене выделения;
- размер шрифта **циклический**: обычный → 1.25 → 1.5 → обычный, как одна кнопка в iOS.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/FormattingToolbar.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/editor/NoteEditorScreen.kt`
- Modify: `res/values/strings.xml`, `res/values-ru/strings.xml`

- [ ] **Шаг 1: Добавить строки**

`values/strings.xml`:

```xml
    <string name="editor_format_bold">Bold</string>
    <string name="editor_format_italic">Italic</string>
    <string name="editor_format_size">Text size</string>
```

`values-ru/strings.xml`:

```xml
    <string name="editor_format_bold">Жирный</string>
    <string name="editor_format_italic">Курсив</string>
    <string name="editor_format_size">Размер текста</string>
```

- [ ] **Шаг 2: Создать панель**

```kotlin
@Composable
fun FormattingToolbar(
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isSizeActive: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSize: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`Row` высотой 44.dp на фоне `colorScheme.background`, сверху разделитель `primary.copy(alpha = 0.3f)` толщиной 0.5.dp. Три `IconButton` 44×44 с иконками `FormatBold`, `FormatItalic`, `FormatSize`; `tint` — `primary` для активной кнопки и `onSurfaceVariant` для неактивной. У каждой кнопки `contentDescription` из строк выше.

Панель показывается **только когда поле ввода в фокусе** и прижата к верхней границе клавиатуры (`imePadding()`).

- [ ] **Шаг 3: Реализовать переключение стиля**

Функция работает над `Editable` `EditText`. Снятие стиля с части диапазона требует разрезания существующего спана — иначе повторное нажатие «жирный» на середине жирного куска не снимет его:

```kotlin
fun toggleStyle(editText: EditText, type: FormatType, scale: Float = 1f) {
    val editable = editText.text ?: return
    val start = editText.selectionStart.coerceAtLeast(0)
    val end = editText.selectionEnd.coerceAtLeast(0)
    if (start == end) return  // без выделения переключать нечего

    val from = minOf(start, end)
    val to = maxOf(start, end)
    val isActive = hasStyle(editable, from, to, type)

    if (isActive) {
        removeStyle(editable, from, to, type)
    } else {
        val span: Any = when (type) {
            FormatType.BOLD -> StyleSpan(Typeface.BOLD)
            FormatType.ITALIC -> StyleSpan(Typeface.ITALIC)
            FormatType.SIZE -> RelativeSizeSpan(scale)
        }
        editable.setSpan(span, from, to, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
    }
}
```

`removeStyle` обязан обрабатывать частичное пересечение: спан, выходящий за снимаемый диапазон, удаляется и заменяется одним или двумя обрезками (слева и справа от снимаемого участка).

- [ ] **Шаг 4: Подключить к редактору**

`NoteEditorScreen` держит состояние активности кнопок, обновляя его из уже существующего колбэка `onCursorPositionChanged` (он вызывается при каждой смене выделения — этого достаточно и нового механизма не требуется).

- [ ] **Шаг 5: Проверить на устройстве**

Run: `./gradlew installDebug`

Проверить:
1. Выделить слово → «жирный» → слово стало жирным, кнопка подсвечена; повторное нажатие снимает.
2. Снять жирный с середины жирного предложения → остаются два жирных куска по краям.
3. Выйти из заметки и открыть заново → форматирование на месте.
4. Ссылка на стих внутри жирного текста — по-прежнему янтарная и кликабельная.

- [ ] **Шаг 6: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/ app/src/main/res/
git commit -m "feat: add a formatting toolbar with bold, italic and text size"
```

---

## Задача 16.5: Доработка `NoteContentWriter`

Обещанная в фазе 13 переделка. `append` дописывает текст **в конец**, поэтому существующие диапазоны не сдвигаются и остаются валидными — но их нужно перенести в возвращаемую копию, а не потерять.

Дописываемый фрагмент форматирования не несёт: стихи вставляются обычным текстом, ссылка подсвечивается парсером сама.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/NoteContentWriter.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/NoteContentWriterFormattingTest.kt`

- [ ] **Шаг 1: Написать падающий тест**

```kotlin
package ru.edgarakert.biblenote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec

class NoteContentWriterFormattingTest {

    @Test
    fun `appending keeps the existing formatting runs`() {
        val note = Note(id = 1, content = "Жирная мысль", formatting = "b:0-6")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)

        assertEquals("Жирная мысль\n\nИоанна 3:16", result.content)
        // Дописывание в конец не сдвигает уже размеченные диапазоны.
        assertEquals("b:0-6", result.formatting)
    }

    @Test
    fun `appending to a legacy plain note leaves formatting null`() {
        val note = Note(id = 1, content = "Старая мысль", formatting = null)
        assertNull(NoteContentWriter.append("Иоанна 3:16", note, now = 5000L).formatting)
    }

    @Test
    fun `a new note created from verses has no formatting`() {
        val note = NoteContentWriter.makeNote("Иоанна 3:16", "Иоанна 3:16", folderId = null, now = 7000L)
        assertNull(note.formatting)
    }

    @Test
    fun `existing runs stay within the grown text`() {
        val note = Note(id = 1, content = "Жирная мысль", formatting = "b:0-6")
        val result = NoteContentWriter.append("Иоанна 3:16", note, now = 5000L)
        val runs = NoteFormattingCodec.decode(result.formatting)
        assertEquals(runs, NoteFormattingCodec.clampTo(runs, result.content.length))
    }
}
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*NoteContentWriterFormattingTest*"`
Expected: FAIL — `formatting` не переносится (`note.copy` в текущей реализации его сохраняет, но тест зафиксирует это явно; если реализация собирает `Note(...)` заново — тест упадёт).

- [ ] **Шаг 3: Обновить реализацию**

```kotlin
    fun append(snippet: String, note: Note, now: Long = System.currentTimeMillis()): Note {
        if (snippet.isBlank()) return note

        val separator = if (note.content.isEmpty()) "" else "\n\n"
        // Дописывание идёт в конец, поэтому существующие диапазоны форматирования
        // остаются валидными и переносятся как есть. Сам фрагмент разметки не несёт.
        return note.copy(
            content = note.content + separator + snippet,
            updatedAt = now
        )
    }
```

`makeNote` оставить без изменений — `formatting` остаётся `null` по умолчанию.

- [ ] **Шаг 4: Запустить оба набора тестов писателя**

Run: `./gradlew testDebugUnitTest --tests "*NoteContentWriter*"`
Expected: PASS — 7 старых тестов из задачи 13.7 и 4 новых.

- [ ] **Шаг 5: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/data/NoteContentWriter.kt app/src/test/java/ru/edgarakert/biblenote/data/NoteContentWriterFormattingTest.kt
git commit -m "feat: carry note formatting through appends from the Bible reader"
```

---

## Задача 16.6: Сквозная проверка

Форматирование затрагивает больше мест, чем кажется, поэтому проверяем связки, а не только сам редактор.

- [ ] **Шаг 1: Старые заметки**

Открыть заметку, созданную до фазы 16 (`formatting IS NULL`): текст показывается плоским, ничего не падает; применить жирный → сохранилось.

- [ ] **Шаг 2: Ссылки на стихи внутри форматирования**

Заметка с текстом `Быт 1:1`, выделить всю строку, сделать жирной. Ссылка должна остаться янтарной и кликабельной: `applyHighlighting` накладывает свой `ForegroundColorSpan` поверх `StyleSpan`, они не конфликтуют.

- [ ] **Шаг 3: Правка ссылки из шторки поверх форматирования**

Сделать ссылку жирной → тапнуть по ней → выбрать в шторке другие стихи. Текст ссылки заменяется, жирность участка сохраняется (за счёт `SPAN_EXCLUSIVE_INCLUSIVE` и того, что `editable.replace` сам двигает спаны).

- [ ] **Шаг 4: Список и поиск**

Превью заметки в списке — плоский текст без разметки; поиск по форматированному тексту находит заметку. Обе функции читают `content`, который не менялся.

- [ ] **Шаг 5: Поворот экрана**

Повернуть экран с открытой форматированной заметкой: форматирование, позиция курсора и скролл на месте.

- [ ] **Шаг 6: Прогнать всё**

Run: `./gradlew test && ./gradlew assembleDebug`
Expected: все тесты зелёные, сборка успешна.

- [ ] **Шаг 7: Проверить апгрейд с реальной базой**

Установить предыдущую версию, создать заметки, поставить сборку с фазой 16 поверх без удаления. Заметки, папки, подсветки и молитвы на месте, миграция 3→4 прошла.

- [ ] **Шаг 8: Обновить документацию**

- `CLAUDE.md` — раздел «Ключевые архитектурные решения»: описать хранение форматирования отдельной колонкой диапазонов и причину отказа от HTML; в моделях Room дописать поле `formatting`
- `docs/plan.md`, `docs/progress.md`, `CHANGELOG.md`

- [ ] **Шаг 9: Ревью субагентом.**
