# BibleNoteAndroid — Прогресс

## Статус: Фаза 0 — Подготовка ✅

### 2026-05-28
- [x] Изучен iOS-проект: `/Users/edgarakert/Works/BibleNote/BibleNote/`
- [x] Зафиксированы рабочие соглашения (5 правил)
- [x] Создана документация: docs/plan.md, docs/progress.md, CLAUDE.md
- [x] Исправлен источник референса (только iOS, без KMP)

**Следующий шаг:** Фаза 1 — Настройка зависимостей, тема, очистка MainActivity.

---

## Фаза 1 — Основа проекта ✅

### 2026-05-28
- [x] Зависимости: Room 2.7.1 + KSP 2.2.10-2.0.2, Navigation Compose 2.9.0, DataStore 1.1.4, ViewModel Compose 2.10.0
- [x] minSdk исправлен: 24 → 26
- [x] bible.sqlite скопирован из iOS-проекта в assets/ (~25 МБ)
- [x] Тема Sacred Manuscript: Color.kt (Parchment/Ink/Amber/...), Theme.kt (без dynamic color), Type.kt (Serif для заголовков)
- [x] BibleNoteApplication.kt + AppContainer.kt (заглушки)
- [x] AndroidManifest.xml: зарегистрирован android:name=".BibleNoteApplication"
- [x] MainActivity: убран Hello World, добавлена AppNavHost-заглушка
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL

**Примечание:** `android.disallowKotlinSourceSets=false` добавлен в gradle.properties (KSP + AGP 9.x совместимость)

**Следующий шаг:** Фаза 2 — Room (Note, Folder), BibleReferenceParser, BibleDatabaseService, SettingsRepository.

## Фаза 2 — Данные и логика ✅

### 2026-05-28
- [x] Room: `Note.kt`, `Folder.kt` — сущности с FK и индексами
- [x] Room: `NoteDao.kt`, `FolderDao.kt` — Flow-запросы, CRUD
- [x] Room: `AppDatabase.kt` — `@Database(version=1)`, KSP генерация
- [x] Bible models: `Book.kt`, `Verse.kt`, `VerseRow.kt`, `VerseHighlight.kt`, `BibleReference.kt`
- [x] `HighlightColor.kt` — enum (amber/rose/sage/sky/lavender) со светлыми и тёмными цветами
- [x] `BibleReferenceParser.kt` — порт iOS, regex с 300+ алиасами (RU+EN), lookbehind `(?<!\p{L})`
- [x] `BibleDatabaseService.kt` — копирует bible.sqlite из assets (версионирование v2), read books/verses, write verse_highlights
- [x] `SettingsRepository.kt` — DataStore (без Context в поле), 5 настроек
- [x] `NoteRepository.kt` — обёртка над NoteDao + FolderDao
- [x] `di/AppModule.kt` — Koin-модуль: Room, Bible, Settings, NoteRepository
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL (без предупреждений)

**Следующий шаг:** Фаза 3 — NotesListScreen, NoteRow, FolderRow, NotesViewModel.

## Фаза 3 — Список заметок ✅

### 2026-05-28
- [x] `FolderWithCount.kt` — data class с `@Embedded Folder` + `noteCount: Int` для Room JOIN-запроса
- [x] `FolderDao.kt` — добавлен `observeRootFoldersWithCount()` (LEFT JOIN notes, GROUP BY folder)
- [x] `NoteRepository.kt` — добавлен `observeRootFoldersWithCount()`
- [x] `NotesViewModel.kt` — StateFlow: rootNotes, rootFolders, searchResults (debounce 200ms); selection mode; CRUD actions; SharedFlow navigateToNote
- [x] `NoteRow.kt` — карточка заметки (CardSurface, serif 17sp, дата, selection с анимацией)
- [x] `FolderRow.kt` — карточка папки (Amber folder icon, plurals note count)
- [x] `SelectionActionBar.kt` — плавающая панель (Move + Delete) с navigationBarsPadding
- [x] `NotesListScreen.kt` — TopAppBar + поиск, LazyColumn (folders + notes), SwipeToDismissBox (delete), context menu (long press), animated bottom bar, 4 AlertDialog
- [x] `AppModule.kt` — `viewModel { NotesViewModel(get()) }`
- [x] `MainActivity.kt` — `AppNavHost()` → `NotesListScreen()`
- [x] `strings.xml` (EN) + `values-ru/strings.xml` (RU) — все строки + plurals
- [x] `libs.versions.toml` + `build.gradle.kts` — `material-icons-extended`

**Следующий шаг:** Фаза 4 — NoteEditorScreen, BibleEditText, NoteEditorViewModel.

## Фаза 4 — Редактор заметок ✅

### 2026-05-28
- [x] `BibleEditText.kt` — AndroidView + EditText: Spannable подсветка (ForegroundColorSpan amber), BibleClickSpan-маркер, setOnTouchListener (только ACTION_UP), debounce 400ms, isProgrammatic try/finally, DisposableEffect cleanup
- [x] `NoteEditorScreen.kt` — Scaffold + TopAppBar (back, 3-dot menu), BasicTextField-заголовок (serif 22sp), amber divider, BibleEditText content, AlertDialog удаления
- [x] `NoteEditorViewModel.kt` — onCleared: short-lived saveScope с try/finally cancel
- [x] `AppModule.kt` — `viewModel { params -> NoteEditorViewModel(params.get<Long>(), get()) }`
- [x] `MainActivity.kt` — NavHost: routes "notes" → "editor/{noteId}", NavController
- [x] `strings.xml` / `values-ru/strings.xml` — editor_title_placeholder, editor_content_placeholder, editor_back, editor_menu, note_delete
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL
- [x] Ревью субагент: 4 HIGH исправлены (ACTION_DOWN, LaunchedEffect(viewModel), try/finally, saveScope.cancel)

**Следующий шаг:** Фаза 5 — BibleVerseSheet (ModalBottomSheet) + подсветка стихов.

## Фаза 5 — Шторка стиха
_Не начата_

## Фаза 6 — Читалка Библии
_Не начата_

## Фаза 7 — Настройки и онбординг
_Не начата_

## Фаза 8 — Навигация
_Не начата_

## Фаза 9 — Папки
_Не начата_

## Фаза 10 — Полировка
_Не начата_