# BibleNoteAndroid — План реализации

## Цель
Переписать iOS-приложение BibleNote на Android (Jetpack Compose).

**Единственный источник:** `/Users/edgarakert/Works/BibleNote/BibleNote/`

---

## Стек

| Слой | Технология |
|------|-----------|
| UI | Jetpack Compose + Material Design 3 |
| Навигация | Navigation Compose + BottomNavigation |
| Состояние | ViewModel + StateFlow |
| Notes DB | Room (Note, Folder) |
| Bible DB | SQLite в assets (read-only): books, verses, verse_highlights |
| Настройки | DataStore Preferences |
| DI | Koin 4.x (BOM) |
| Min SDK | 26 (Android 8.0) |

---

## Фазы

### Фаза 1 — Основа проекта (Foundation)
- [ ] Настроить зависимости (Room + KSP, Navigation, DataStore, Compose, Lifecycle)
- [ ] Исправить minSdk на 26
- [ ] Добавить bible.sqlite в assets
- [ ] Создать тему (BibleNoteTheme: все цвета, типографика)
- [ ] Создать BibleNoteApplication + AppContainer (заглушки)
- [ ] Очистить MainActivity (убрать Hello World, подключить NavHost)

### Фаза 2 — Данные и логика (Data Layer)
- [ ] Room: сущности Note, Folder + DAO + AppDatabase
- [ ] BibleReferenceParser (порт из iOS BibleNote/Services/)
- [ ] BibleDatabaseService (SQLite из assets: books, verses, verse_highlights)
- [ ] Модели bible: Book, Verse, VerseHighlight, HighlightColor
- [ ] SettingsRepository (DataStore: translation, enabledTranslations, appearanceMode, verseScale, onboarding)
- [ ] NoteRepository (обёртка над Room DAO)
- [ ] di/AppModule.kt (Koin-модуль: все зависимости)

### Фаза 3 — UI: Список заметок
- [x] NotesListScreen (список заметок + папок, поиск)
- [x] NoteRow, FolderRow
- [x] SelectionActionBar (режим выделения)
- [x] Диалоги: новая папка, переименовать, удалить папку
- [x] NotesViewModel

### Фаза 4 — UI: Редактор заметок
- [ ] NoteEditorScreen
- [ ] BibleEditText (AndroidView + EditText + Spannable highlighting)
- [ ] NoteEditorViewModel (автосохранение через debounce, удаление пустой заметки)

### Фаза 5 — UI: Шторка стиха
- [ ] BibleVerseSheet (ModalBottomSheet)
- [ ] Переключатель перевода внутри шторки
- [ ] Кнопка «Открыть главу» → переключение на вкладку Библии
- [ ] Подсветка стихов (HighlightColor) в шторке

### Фаза 6 — UI: Читалка Библии
- [ ] BibleBookPickerScreen (список книг с поиском)
- [ ] BibleReaderScreen (чтение главы, подсветка стихов)
- [ ] BibleReaderViewModel (fetchBooks, fetchVerses, fetchHighlights, saveHighlight)
- [ ] Навигация по главам (prev/next)

### Фаза 7 — UI: Настройки и онбординг
- [ ] SettingsScreen
- [ ] TranslationSettingsScreen (выбор перевода по умолчанию + включённые переводы)
- [ ] BibleThemeSettingsScreen (verseScale, appearanceMode)
- [ ] AboutScreen
- [ ] OnboardingScreen (3 слайда + выбор переводов)
- [ ] TranslationSelectionScreen
- [ ] SettingsViewModel

### Фаза 8 — Навигация
- [x] BottomNavigation (3 вкладки: Заметки, Библия, Настройки)
- [x] NavGraph: вложенные графы для каждой вкладки
- [x] Связь шторки стиха с вкладкой Библии (через route arguments "bible_at/{bookId}/{chapter}")

### Фаза 9 — Папки
- [x] FolderScreen (список заметок внутри папки)
- [x] MoveFolderScreen (переместить заметки в папку)

### Фаза 10 — Полировка
- [x] Иконка приложения
- [x] Локализация (ru/en strings)
- [x] Edge-to-edge UI
- [x] Анимации переходов
- [x] Unit-тесты BibleReferenceParser

---

## Фаза 11 — Баг-фиксы (подтверждены при тестировании)

| # | Экран | Описание | Файл |
|---|-------|----------|------|
| B1 | Редактор | Многострочная ссылка не тапается: `line == spanLine` отсекает все строки кроме первой | `BibleEditText.kt` |
| B2 | Список заметок | Новая пустая заметка на секунду появляется в списке перед удалением | `NotesViewModel.kt`, `NoteEditorViewModel.kt` |
| B3 | Редактор | При смене ориентации экрана пропадает фокус и курсор в `BibleEditText` | `BibleEditText.kt` |
| B4 | Список заметок | При одной заметке пункт «Выбрать» недоступен: `canEnterSelectMode = size > 1` | `NotesListScreen.kt` |
| B5 | Читалка Библии | Системная кнопка «Назад» в пикере книг выходит из всего экрана вместо закрытия пикера | `BibleReaderScreen.kt` |
| B6 | Онбординг | Системная кнопка «Назад» на `TranslationSelectionScreen` возвращает на последний слайд онбординга | `NavGraph.kt` / `OnboardingScreen.kt` |

- [x] B1: Исправить определение тапа на многострочных ссылках в `BibleEditText`
- [x] B2: Устранить мигание пустой заметки при немедленном выходе из редактора
- [x] B3: Восстанавливать фокус в `BibleEditText` после смены ориентации
- [x] B4: Изменить условие `canEnterSelectMode` на `rootNotes.size >= 1`
- [x] B5: Добавить `BackHandler` в `BibleReaderScreen` для закрытия пикера
- [x] B6: Запретить навигацию назад с `TranslationSelectionScreen` на слайды онбординга

---

---

## Фаза 12 — Недостающий функционал (сравнение с iOS)

> Источник: iOS-версия `/Users/edgarakert/Works/BibleNote/BibleNote/`
> Файлы iOS для справки: `UIKit/FormattingToolbar.swift`, `UIKit/BibleTextView.swift`, `Services/SpotlightService.swift`, `Views/Note/NoteEditorView.swift`

### ✅ Задача 12.1 — Rich Text форматирование в редакторе (Bold / Italic / Крупный шрифт)

iOS имеет `FormattingToolbar` над клавиатурой и хранит заметку как RTF (`note.contentRTF: Data?`). Android хранит только plain text.

Что нужно сделать:
- Добавить поле `contentHtml: String?` в `data/db/Note.kt` + миграция Room (версия 2)
- Создать `data/db/RichTextSerializer.kt` — сериализует `Spannable` ↔ JSON через `org.json.JSONObject` (встроен в Android, без новых зависимостей). Сохраняет только `StyleSpan(BOLD/ITALIC)` и `RelativeSizeSpan(1.5f)`, игнорирует `ForegroundColorSpan`/`BibleClickSpan`
- Добавить в `CursorTrackingEditText` (`ui/components/BibleEditText.kt`) методы `applyBold()`, `applyItalic()`, `applyLarge()` + in-memory undo-стек (`ArrayDeque<SpannableStringBuilder>`)
- Новые параметры `BibleEditText`: триггерные счётчики `boldTrigger/italicTrigger/largeTrigger/undoTrigger/redoTrigger: Int` (инкремент = сигнал применить действие, как iOS делает с `undoTrigger`) + коллбэки `onContentChanged(plain, html?)`, `onFormattingChanged(bold, italic, large)`, `onUndoStateChanged(canUndo, canRedo)`
- Создать `ui/components/FormattingToolbar.kt` — Compose Row с кнопками B / I / Aa, активная подсвечена `primary`-цветом
- Показывать `FormattingToolbar` только когда видна клавиатура (`WindowInsets.isImeVisible`), прижать через `windowInsetsPadding(WindowInsets.ime)`
- Обновить `NoteEditorViewModel` — добавить `StateFlow` для `isBold/isItalic/isLarge/canUndo/canRedo` и триггерных счётчиков; загружать/сохранять `contentHtml`
- Обновить `NoteEditorScreen` — подключить `FormattingToolbar`, добавить иконки Undo/Redo в `TopAppBar` (показывать только когда доступны)

### Задача 12.2 — «Переместить в папку» из редактора заметки

В `NoteEditorScreen.kt` кнопка меню «Переместить в папку» — заглушка `/* Phase 9 */` (строка ~109). В iOS работает полностью.

Что нужно сделать:
- Добавить в `NoteEditorViewModel` `StateFlow<List<Folder>>` (все папки) + методы `moveToFolder(folderId: Long?)` и `createFolderAndMove(name: String)`
- Если в `NoteDao` нет `updateFolderId` — добавить `@Query("UPDATE notes SET folderId = :folderId WHERE id = :noteId")`
- В `NoteEditorScreen` убрать заглушку: `onClick = { showMenu = false; showMoveFolderSheet = true }`, добавить `MoveFolderSheet` (компонент уже есть в `ui/components/MoveFolderSheet.kt`)

---

## Соглашения

1. Всегда проверять официальную документацию Android, использовать best practices
2. Вести docs/plan.md и docs/progress.md
3. Декомпозировать задачи и делегировать субагентам
4. Использовать нужные скиллы
5. После каждой задачи — ревью субагент