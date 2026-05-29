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

## Фаза 5 — Шторка стиха ✅

### 2026-05-28
- [x] `BibleVerseSheetViewModel.kt` — UiState (title, verses, highlights, enabledTranslations, selectedTranslation, verseScale, isLoading); `loadJob` для отмены race condition; `private val reference`
- [x] `BibleVerse.kt` — строка стиха: номер (Amber), текст (Ink), фон highlight, масштаб verseScale
- [x] `BibleVerseSheet.kt` — ModalBottomSheet (Parchment): serif-заголовок, translation picker (capsule, только если >1), hairline-разделитель, CircularProgressIndicator при загрузке, LazyColumn стихов + кнопка «Открыть главу», empty state
- [x] `NoteEditorScreen.kt` — `tappedReference` state, wire up `onReferenceTapped`, показ `BibleVerseSheet`
- [x] `AppModule.kt` — `viewModel { params -> BibleVerseSheetViewModel(...) }`
- [x] `strings.xml` / `values-ru/strings.xml` — `verse_not_found`, `verse_open_chapter`
- [x] Ревью субагент: 4 HIGH исправлены (race condition loadJob, reference private, loading indicator, `onOpenChapter` — Phase 8 design)
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL

**Следующий шаг:** Фаза 6 — BibleBookPickerScreen, BibleReaderScreen, BibleReaderViewModel.

## Фаза 6 — Читалка Библии ✅

### 2026-05-28
- [x] `SettingsRepository.kt` — добавлены `lastBookId`, `lastChapter`, `lastBibleTranslation` (DataStore, сохранение позиции между сессиями)
- [x] `BibleReaderViewModel.kt` — `UiState` (bookId, chapter, bookName, chapterCount, translation, enabledTranslations, books, verses, highlights, selectedVerseNumbers, verseScale); `initJob` отдельно от `loadJob`; методы: `navigatePrev/Next/To`, `setTranslation`, `toggleVerseSelection`, `clearSelection`, `applyHighlight`, `removeHighlights`, `copySelectedVerses`, `fetchChapterCount`
- [x] `BibleVerse.kt` — добавлен `isSelected: Boolean = false` (amber фон при выделении)
- [x] `BibleBookPickerScreen.kt` — список книг (ВЗ + НЗ), внутренняя навигация к ChapterGrid (5 колонок); safe capture `book` вместо `selectedBook!!`
- [x] `BibleReaderScreen.kt` — TopAppBar (title-кнопка → пикер, translation menu), скролл стихов с tappable-выделением, ChapterNavBar (prev/next кружки), VerseActionBar (close + copy + 5 цветов подсветки), анимированный slide/fade; `remember` для `sharedHighlight`
- [x] `AppModule.kt` — `viewModel { BibleReaderViewModel(get(), get()) }`
- [x] `strings.xml` / `values-ru/strings.xml` — `bible_picker_title`, `bible_old_testament`, `bible_new_testament`, plurals `bible_verse_count`
- [x] `MainActivity.kt` — startDestination временно переключён на `"bible"` для тестирования
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL (без предупреждений)
- [x] Ревью субагент: 2 HIGH исправлены (highlight path → `loadChapter` после save; `selectedBook!!` → safe capture); MEDIUM: `remember(sharedHighlight)`, отдельный `initJob`

**Следующий шаг:** Фаза 7 — SettingsScreen, TranslationSettingsScreen, BibleThemeSettingsScreen, AboutScreen, OnboardingScreen.

## Фаза 7 — Настройки и онбординг ✅

### 2026-05-29
- [x] `SettingsViewModel.kt` — UiState (defaultTranslation, enabledTranslations, appearanceMode, verseScale, translationGroups); toggleEnabledTranslation с Mutex (anti-race); saveOnboardingSelections suspend; TranslationInfo/TranslationGroup inner types; locale-ordered groups
- [x] `SettingsScreen.kt` — TopAppBar, секции (Язык→system settings, Библия→Theme+Translations, Приложение→About); внутренние компоненты: SectionHeader, SettingsCard, SettingsRow
- [x] `TranslationSettingsScreen.kt` — группы переводов с toggle (CheckCircle icon); секция «по умолчанию» (только enabled); ViewModel как параметр
- [x] `BibleThemeSettingsScreen.kt` — SingleChoiceSegmentedButtonRow (System/Light/Dark); Slider размера шрифта (0.8..1.4, 12 steps); ViewModel как параметр
- [x] `AboutScreen.kt` — лого, версия (PackageManager), описание, контакт + mailto Intent
- [x] `OnboardingScreen.kt` — HorizontalPager 3 слайда, PageDotsView (анимированные капсулы), слайд 2: анимированный демо-текст с remember; кнопка Далее/Выбрать переводы
- [x] `TranslationSelectionScreen.kt` — карточки переводов с border при выборе, collectAsStateWithLifecycle, scope.launch + suspend saveOnboardingSelections → onComplete()
- [x] `MainActivity.kt` — onboarding gating (collectAsStateWithLifecycle false→OnboardingFlow), darkTheme из AppearanceMode, AppNavHost с settingsVm hoisted до Activity scope
- [x] `AppModule.kt` — добавлен viewModel { SettingsViewModel(get()) }
- [x] `strings.xml` / `values-ru/strings.xml` — все новые строки (настройки, переводы, онбординг, about)
- [x] Ревью субагент: 5 HIGH исправлены (Mutex toggleEnabled, suspend saveOnboardingSelections, collectAsStateWithLifecycle, scope.launch перед onComplete, ViewModel hoisting)
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL (без ошибок)

**Следующий шаг:** Фаза 8 — Навигация (BottomNavigation, TabBar, интеграция всех экранов).

## Фаза 8 — Навигация
_Не начата_

## Фаза 9 — Папки
_Не начата_

## Фаза 10 — Полировка
_Не начата_