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

## Фаза 8 — Навигация ✅

### 2026-05-29
- [x] `ui/navigation/NavGraph.kt` — `AppNavHost` composable: Scaffold + NavigationBar (3 вкладки) + NavHost с nested graphs (notes_graph / bible_graph / settings_graph)
- [x] `BottomNavigation` — Material 3 `NavigationBar`; цвета: selected=Amber, unselected=WarmGray, indicator=Hairline, background=Parchment; selected state через `NavDestination.hierarchy`
- [x] `notes_graph` — notes + editor/{noteId} (back stack isolation, saveState/restoreState)
- [x] `bible_graph` — bible (нормальная загрузка из DataStore) + bible_at/{bookId}/{chapter} (cross-tab deep navigation)
- [x] `settings_graph` — settings + settings/theme + settings/translations + settings/about; settingsVm hoisted до NavGraph scope (shared instance)
- [x] Связь шторки → Библия: route args `"bible_at/{bookId}/{chapter}"` вместо event bus; `BibleReaderScreen` получает `pendingBookId/pendingChapter`, проверяет отличие от текущего состояния ViewModel перед вызовом `navigateTo`
- [x] `NoteEditorScreen` — добавлен `onOpenChapter: (BibleReference) -> Unit = {}` параметр
- [x] `MainActivity.kt` — упрощён: делегирует в `AppNavHost()` из NavGraph.kt
- [x] `strings.xml` / `values-ru/strings.xml` — `tab_notes`, `tab_bible`, `tab_settings`
- [x] Ревью субагент: исправлены все HIGH (StateFlow-event-bus → route args, timing gap → route args, ViewModel scope)
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL (без предупреждений)

**Следующий шаг:** Фаза 9 — Папки (FolderScreen, MoveFolderScreen).

## Фаза 9 — Папки ✅

### 2026-05-29
- [x] `NoteDao.kt` — добавлен `updateFolderIds(ids, folderId?)` для перемещения заметок
- [x] `FolderDao.kt` — добавлены `observeSubfoldersWithCount(parentId)`, `observeAllFolders()`, `observeById(id)` (reactive Flow)
- [x] `NoteRepository.kt` — добавлены `getFolderById`, `observeFolderById`, `observeSubfoldersWithCount`, `observeAllFolders`, `moveNotesToFolder`
- [x] `NotesViewModel.kt` — добавлены `allFolders`, `moveSelectedNotes`, `createFolderAndMoveSelected`; `onMove` в SelectionActionBar теперь работает
- [x] `FolderViewModel.kt` — новый ViewModel: `folder` как реактивный StateFlow (observeById), `notes`, `subfolders`, `allFolders`; CRUD: createNote, deleteNote/selected, moveSelected, createSubfolder, renameFolder, deleteSubfolder, deleteThisFolder
- [x] `MoveFolderSheet.kt` — ModalBottomSheet: «Без папки» + корневые папки + подпапки с отступом + «Новая папка»
- [x] `FolderScreen.kt` — TopAppBar (back, +, меню: rename/new subfolder/select/delete), LazyColumn (subfolders + notes), SelectionActionBar, MoveFolderSheet, предупреждения о заметках при удалении папки
- [x] `NavGraph.kt` — добавлен маршрут `folder/{folderId}`; `onNavigateToFolder` подключён
- [x] `AppModule.kt` — зарегистрирован `FolderViewModel(folderId, repository)`
- [x] `strings.xml` / `values-ru/strings.xml` — `folder_move_to`, `folder_no_folder`, `folder_subfolder_new`, `folder_delete_notes_warning`
- [x] Ревью субагент: 3 HIGH исправлены (удалён default viewModel param → всегда из NavGraph; `deleteThisFolder` null guard; предупреждения о заметках в диалогах удаления); `_folder` → реактивный Flow
- [x] Сборка: `compileDebugKotlin` — BUILD SUCCESSFUL (без ошибок)

**Следующий шаг:** Фаза 10 — Полировка (иконка, локализация, edge-to-edge, анимации, тесты).

## Фаза 10 — Полировка ✅

### 2026-05-29
- [x] `ic_launcher_background.xml` — заменён на Parchment (#F6EFE4) вместо Android-зелёного
- [x] `ic_launcher_foreground.xml` — нарисован открытый Bible: обложка Amber (#C29A3D), страницы Parchment (#FEFCF7), строки текста, корешок (#A88028); вписан в safe zone 72×72 внутри 108dp
- [x] Локализация — все строки присутствуют в EN и RU файлах, новые ключи фазы 9 добавлены в обе локали
- [x] Edge-to-edge — `enableEdgeToEdge()` уже был. Добавлен `SideEffect` в `MainActivity`: синхронизирует `isAppearanceLightStatusBars`/`isAppearanceLightNavigationBars` с текущей темой (dark/light)
- [x] Анимации навигации — в `NavGraph.kt` добавлены глобальные transitions для `NavHost`: вперёд `slideIntoContainer(Start) + fadeIn(280ms)`, назад `slideOutOfContainer(End) + fadeOut(280ms)`, exit/popEnter — `fadeOut/fadeIn(200ms)`
- [x] `BibleReferenceParserTest.kt` — 24 unit-теста: пустой ввод, EN/RU ссылки, главы, диапазоны, регистронезависимость, границы слов, несколько ссылок, индексы, displayText, helpers isWholeChapter/isSingleVerse
- [x] Сборка + тесты: `compileDebugKotlin` BUILD SUCCESSFUL, `test` 24/24 PASSED (0 failures)

**Все 10 фаз завершены. Приложение готово к первому релизу.**
## Ресерч расхождений с iOS ✅

### 2026-09-05
- [x] Ресерч iOS-проекта: 15 коммитов после точки синхронизации `a4e6862`, включая молитвенный журнал (`4208f08`), полную главу в шторке с правкой ссылки (`1916b02`, `8ca475d`), счётчик упоминаний (`601aebf`), сохранение стихов в заметку (`141d93b`), закрепление заметок (`f83a996`), порядок книг Синодального (`3e0494c`), промпт оценки (`dfea6e7`)
- [x] Ресерч Android-проекта: полный аудит слоёв заметок/редактора и Библии четырьмя субагентами
- [x] Найдено сверх списка разработчика iOS: закрепление заметок, порядок книг НЗ, промпт оценки, фиксы редактора; отдельно — старый непортированный долг (rich text, share extension, Spotlight)
- [x] Найдены риски в самом Android-проекте: Room собирается без миграций и без destructive fallback; парсер принимает только ASCII-дефис, а форматирование выводит en dash; `deleteNote()` не сбрасывает состояние и воскрешает заметку через автосохранение; `HighlightColor.darkColor` — мёртвый код; названия переводов захардкожены по-русски
- [x] Ложная тревога снята: `CLAUDE.md` описывает 4 перевода, но код давно на двух (`synodal`, `kjv`) — расхождения с iOS нет, устарела документация
- [x] Составлены планы фаз 12–15 (34 задачи) в `docs/superpowers/plans/`
- [x] Получены ответы на открытые вопросы Q1–Q4: rich text портируем (выделен в фазу 16, 6 задач), `ACTION_SEND` и системный поиск пока не нужны, порядок книг — только для `synodal`

**Следующий шаг:** фаза 12 — фундамент и быстрые победы.

## Фаза 12 — Фундамент и быстрые победы ✅

### 2026-09-05
Ветка `feature/phase12-foundation`, 13 коммитов. Все 8 задач плана закрыты, `./gradlew test` и `./gradlew assembleDebug` зелёные.

- [x] **12.1** `BibleReference.kt` — `coveredVerses` (раскрывает ссылку в номера стихов; `verseList` авторитетнее устаревших `verseStart`/`verseEnd`), `formatVerseSpec(verses, rangeSeparator)` и `replacementText(displayText, verses)`. Закрыт риск **R2**: парсер принимает только ASCII-дефис, а `compactVerseString` выводил en dash — текст приложения не парсился обратно. Теперь ASCII по умолчанию, en dash только для заголовков на экране. 19 тестов, включая round-trip через настоящий парсер
- [x] **12.2** `AppDatabase` версии 2 + `AutoMigration(from = 1, to = 2)`, колонка `notes.isPinned` с `defaultValue = "0"`. Закрыт риск **R1**: база собиралась без `addMigrations` и без destructive fallback, версия 1 уже у пользователей. Подключена `room-testing`; `MigrationTest` на эмуляторе проверяет сохранность заметок, папок и связи `notes.folderId`. Осознанно **не** добавлен `fallbackToDestructiveMigrationOnDowngrade` — облачной синхронизации нет, молчаливое удаление заметок хуже падения при ручной установке старого APK
- [x] **12.3** `NoteDao.setPinned` + `NoteRepository.setNotePinned` — точечный `UPDATE`, а не upsert сущности: закрепление не меняет `updatedAt`, иначе заметка прыгала бы наверх по свежести
- [x] **12.4** `ui/components/NoteListSection.kt` — общий `LazyListScope`-расширитель вместо приватных дублей в `NotesListScreen` и `FolderScreen` (риск **R6**). Закреплённые рендерятся отдельной секцией сверху, а не пересортированным списком (пересортировка внутри одной секции даёт артефакт анимации). Long-press-меню из трёх пунктов (риск **R7**): закрепить/открепить, переместить, удалить; одиночное перемещение через новые `moveNote`/`createFolderAndMoveNote`, мультивыбор не затронут. При поиске секций нет — один плоский список
- [x] **12.5** Multiline-заголовок заметки: `maxLines = 4`, начертание `Bold` → `SemiBold` (порт iOS `86625e7`)
- [x] **12.6** `BibleDatabaseService.applyBookOrder` — православный порядок НЗ для `synodal`: соборные послания после Деяний, до посланий Павла. Меняется только порядок показа, `book_id` и ссылки не трогаются. Порядок сверен с фактическими id в `bible.sqlite` (44 = Деяния, 59–65 = соборные, 58 = Евреям). 4 pure-JVM теста
- [x] **12.7** Запоминание последней открытой папки/заметки. `NotesPathCodec` кодирует типизированный путь в строку `"f:12,n:34"` (без `kotlinx.serialization` ради одного списка), битые сегменты пропускаются. `MainActivity` читает путь блокирующе один раз до `setContent`, `AppNavHost` сеет back stack тем же `pushNotesPath`, что и обычная навигация, а обрезает путь реактивно по `currentBackStackEntryAsState` — так он не расходится ни с системным жестом «назад», ни с восстановлением вкладки. Устойчивость к риску **R3**: каждый id проверяется в базе, на первом неразрешимом восстановление останавливается, сохраняя префикс. Проверено на эмуляторе, включая удаление заметки прямо в файле БД в обход приложения — открывается последняя живая папка, не корень и не падение. 5 тестов кодека
- [x] **12.8** `NoteEditorViewModel.deleteNote()` сбрасывает `currentNote` и `isDirty` — риск **R3**. Раньше правка и удаление внутри окна debounce 500 мс возвращали заметку через upsert с прежним id, из `onCleared()` либо из ещё не отработавшего коллектора. Сделано **до** 12.7: иначе сохранённый путь указывал бы на строку-зомби

Сверх плана, найдено при ревью задач фазы:
- [x] Long-press-меню заметки было недостижимо для TalkBack: `pointerInput { detectTapGestures }` не даёт семантики `onClick`/`onLongClick`, а долгое нажатие — единственный путь к закреплению. Заменено на `combinedClickable` с `onLongClickLabel`
- [x] Запуск мигал чужим состоянием: `collectAsStateWithLifecycle(false)`/`(SYSTEM)` рисовали первый кадр по умолчаниям, пока DataStore не отдаст значения — вернувшийся пользователь видел кадр онбординга, а выставивший светлую тему на тёмной системе — тёмную вспышку. Начальные значения теперь берутся из того же блокирующего чтения, что и путь навигации
- [x] В `build.gradle.kts` объяснено, зачем androidTest форсит `kotlinx-serialization` 1.8.1 (Compose BOM жёстко фиксирует 1.7.3, `room-testing` требует 1.8.1) и по какому признаку форс можно будет снять

### Долг и известные края фазы 12

- **Логика восстановления навигации в `NavGraph.kt` не покрыта автотестами.** Это самый сложный
  новый код фазы: три взаимодействующих `LaunchedEffect` (посев при запуске, реакция на back stack,
  запись при переходе), корректность которых зависит от тонкого тайминга рекомпозиции и
  приостановки корутин. Проверено вручную на эмуляторе, в том числе на подложенном пути к
  несуществующей заметке, но от регрессии это не защищает. **Фаза 13 будет трогать навигацию
  заметок и стихов прямо поверх этого кода** — перед её началом стоит закрыть долг
  инструментальным тестом на Navigation Compose.
- **Заметка-зомби при убийстве процесса.** Если процесс убит посреди редактирования, отсоединённая
  корутина в `NoteEditorViewModel.onCleared()` не успевает отработать, и пустая заметка остаётся в
  базе. Поведение предсуществующее, задачей 12.8 не внесено; деградирует безопасно — строка живёт
  до следующего чистого выхода, а восстановление пути её корректно переживает.

**Следующий шаг:** фазы 13 и 14 — они не зависят друг от друга и могут идти параллельно.

## Фаза 13 — Связка Библия ↔ заметки ✅

### 2026-09-06
Ветка `feature/phase13-bible-notes` (от фазы 12), 11 коммитов. Все 8 задач плана закрыты, `./gradlew test` (107 тестов) и `./gradlew assembleDebug` зелёные.

Опорный принцип фазы, унаследованный от iOS: **текст заметки — единственный источник истины**. Ссылки нигде не хранятся отдельно, всегда заново выводятся парсером из текста. Отсюда три следствия: индекс упоминаний считается на лету, правка ссылки — обычная текстовая замена участка (а не операция над спанами), вставленные стихи пишутся обычным текстом.

- [x] **13.1** `NoteDao.observeAllNotes()` + проброс в репозиторий. Такого запроса намеренно не было (риск **R4**): заметки в Room, стихи в отдельной `bible.sqlite`, SQL-join невозможен, поэтому индекс считается в Kotlin по всему корпусу
- [x] **13.2** `NoteVerseIndexService.index(...)` — чистая функция «стихи главы → ссылающиеся заметки», 8 тестов. Три правила из iOS: ссылка на главу целиком индексируется на все её стихи; заметка попадает под стих один раз даже при нескольких ссылках; чужая глава или книга не даёт ничего
- [x] **13.3** Бейдж с числом заметок у стиха в читалке и `VerseNotesSheet` со списком; тап по строке открывает заметку в редакторе. Индекс считается на `Dispatchers.Default` (регулярка парсера — около 360 альтернатив по всему корпусу) и пересчитывается при смене главы и при любой эмиссии потока заметок, поэтому бейдж появляется сразу после сохранения, без ручного обновления
- [x] **13.4** Шторка стиха грузит главу целиком и скроллится к нужному стиху — ссылку `Быт 2` теперь можно читать с 14-го стиха, не уходя из заметки. Выделение показано полосой слева, а не заливкой фона, иначе терялся бы цвет подсветки стиха. Заголовок пересчитывается от живого выбора. Кнопка «Открыть главу» вынесена из списка в закреплённый низ — иначе на Пс 118 она уезжала бы вниз на 176 строк
- [x] **13.5** Тап по стиху в шторке немедленно переписывает ссылку в тексте заметки (`Быт 2` → `Быт 2:14` → `Быт 2:14-15`). Три несущие детали: диапазон берётся живым в момент тапа (спаны двигаются, пока пользователь печатает), правка применяется через `editable.replace` (попадает в стек отмены и не сбрасывает курсор), цель смещается после каждой правки на длину замены. Токен отсекает повторное применение при рекомпозиции
- [x] **13.6** `VerseSnippetBuilder` — выделение стихов превращается в текст заметки: ссылка строкой, пустая строка, пронумерованные стихи. 9 тестов, из них ключевой — round-trip через настоящий парсер: если он падает, вставленные стихи перестают быть кликабельной ссылкой
- [x] **13.7** `NoteContentWriter` — единственная точка записи текста в заметку извне редактора, 9 тестов. Чистые функции, ничего не сохраняют: сохранение делает вызывающий. **Будет доработан в фазе 16** — когда у заметки появится вторая копия текста с разметкой, `append` и `makeNote` должны будут писать обе (задача 16.5)
- [x] **13.8** Сохранение выделенных стихов в новую или существующую заметку: пикер назначения с обходом дерева папок, шторка с предзаполненным и редактируемым текстом, тост с кнопкой «Открыть». Заголовок новой заметки — сама ссылка

Сверх плана, найдено при ревью задач фазы:
- [x] `BibleEditText` молча отбрасывал правку, если её границы не подошли к живому тексту, а редактор к этому моменту уже сдвинул свой диапазон в расчёте на успех — и оставался рассинхронизирован навсегда, промахиваясь мимо ссылки на каждом следующем тапе. Теперь отказ сообщается наружу, и шторка закрывается, чтобы пользователь получил свежий живой диапазон
- [x] Выделение стиха в шторке показано только полосой слева, которой screen reader не видит. `clickable` заменён на `toggleable` — состояние «выбрано» теперь есть в семантике
- [x] `NoteContentWriter` проверял сниппет через `isBlank`, а существующий контент через `isEmpty`, поэтому заметка из одних пробелов получала разделитель и начиналась с пустой строки

Два бага пойманы исполнителем 13.8 именно на ручном прогоне, ревью по коду их бы не увидело: наложение текста в строке выбора назначения при длинной подписи и закешированный экземпляр `SaveVersesToNoteViewModel` — повторное открытие шторки для другого выделения возвращало экземпляр от первого, и новая заметка получала заголовок от предыдущего выделения.

### Долг и известные края фазы 13

- **Ключ ViewModel копит экземпляры.** `SaveVersesToNoteViewModel` и `BibleVerseSheetViewModel` создаются с динамическим ключом (по выделению и по ссылке соответственно), а старые экземпляры из `ViewModelStore` никто не удаляет — за длинную сессию чтения их число растёт. Экземпляры крошечные, так что практического эффекта нет, но при частых сохранениях стоит перейти на передачу выделения в `save(...)` вместо конструктора.
- **Долг фазы 12 по `NavGraph` уже выстрелил.** Ревью фазы 13 сообщило, что поворот экрана
  закрывает шторку стиха; проверка на устройстве показала более серьёзную причину — посев
  back stack выполнялся заново при каждом пересоздании Activity и дублировал записи стека.
  Починено (`324ca83`), но это ровно тот класс регрессии, ради которого долг и фиксировался:
  тест на восстановление навигации по-прежнему нужен.
- **Механика правки ссылки не покрыта автотестами.** `PendingEdit`, живой диапазон спана и смещение цели проверены только вручную на устройстве. Это самая тонкая механика фазы, и фаза 16 (rich text) будет переписывать `applyHighlighting` прямо под ней — тест стоит завести до её начала.

**Следующий шаг:** фаза 14 — молитвенный журнал (13 задач, от фазы 13 не зависит).

## Фаза 14 — Молитвенный журнал ✅

### 2026-09-13
Ветка `feature/phase14-prayer-journal` (стопкой от фазы 13 — обе меняют `NavGraph`), 16 коммитов. Все 13 задач плана закрыты. `./gradlew test` — 166 юнит-тестов, `connectedDebugAndroidTest` — 7 инструментальных (миграции 1→2→3 и DAO молитв), `lintDebug` чист. Каждый экран проверен на эмуляторе.

Продуктовые ограничения фичи из iOS соблюдены: никаких «просрочено», стриков и поздравлений; «молитесь об этом N дней» — число разных дней, а не серия; молитвы не попадают в общий поиск; уведомление всегда с общим текстом.

**Данные и логика**
- [x] **14.1** `PrayerRequest`/`PrayerEntry` с каскадным удалением дописок, автомиграция 2→3 — создаёт только две таблицы, `notes`/`folders` не трогает. Room сам включает `PRAGMA foreign_keys`, так что каскад реально работает
- [x] **14.2** `PrayerDao`/`PrayerRepository`. **План содержал потерю данных:** `@Insert(REPLACE)` SQLite выполняет как DELETE+INSERT, и каскад стирал все дописки при любом обновлении просьбы — «помолился», правка, ответ. Доказано падением теста на варианте из плана (`expected:<2> but was:<0>`), заменено на `@Upsert`; он при обновлении возвращает -1, репозиторий подставляет настоящий id
- [x] **14.3** `PrayerActions` — календарные дни, идемпотентность «помолился» в рамках дня, отказ на пустой ответ. 8 тестов
- [x] **14.4** `PrayerSelectionService` — весь активный список без ограничения количества, непомоленные сегодня выше; портирован отгруженный код iOS, а не его устаревший план (риск **R10**). 5 тестов
- [x] **14.5** Ежедневное напоминание на `AlarmManager.setAndAllowWhileIdle`. План пропускал **канал уведомлений** — без него на minSdk 26 уведомление не показывалось бы ни на одном устройстве; приёмники берут `SettingsRepository` из Koin (второй DataStore падает); перевзвод при перезагрузке, холодном старте и после обновления приложения. Проверено на устройстве, включая перезагрузку без запуска приложения
- [x] **14.6** 44 строки и 5 плюралов в обеих локалях — платформенные `<plurals>` вместо ручного `RussianPlural` из iOS, который давал «21 day»

**Экраны**
- [x] **14.7 + 14.8** Четвёртая вкладка и «Сегодня». Объединены: план регистрировал в графе экраны, которых ещё не было. Навигационные колбэки nullable — нет колбэка, нет кнопки. Пересчёт «помолились сегодня» при возврате на экран, с учётом того, что `StateFlow` не эмитит равные значения
- [x] **14.11** Редактор — сделан раньше 14.9/14.10, чтобы остальные экраны проверялись на данных из интерфейса. **План портил ссылки:** деление стихов по запятой ломало `Быт 1:2,5` — в Android запятая входит в синтаксис ссылки; разбор парсером, 11 тестов. Проверено по базе, снятой с устройства
- [x] **14.9** «Все просьбы» — фильтр, поиск (включая кириллицу в любом регистре), секции по категориям; «Доверить Богу» в меню долгого нажатия. 12 тестов
- [x] **14.10** Карточка просьбы. Дата из `DatePicker` — полночь по UTC, конвертация в `PrayerAnswerDate` (6 тестов); выбор ограничен от даты создания до сегодня. На устройстве найдено и исправлено: календарь обрезал воскресенье на 360dp и был лавандовым, кнопки разной высоты, меню озвучивалось как «Действия с заметкой»
- [x] **14.12** «Отвеченные молитвы» — группы по году, «Молились 3 месяца». Крайние случаи длительности, которые план не определял, зафиксированы 13 тестами. Проверено на устройстве с переводом часов эмулятора назад
- [x] **14.13** Настройки напоминания — разрешение запрашивается при включении; после отказа подсказка с переходом в настройки системы, иначе переключатель выглядел бы сломанным

### Решено владельцем продукта

- **Точность напоминания — окно 15 минут.** `setAndAllowWhileIdle` из плана система растягивала до окна в час (`dumpsys alarm`: `window=+1h`) — напоминание на 09:00 могло прийти в 09:50. Переведено на `setWindow` с окном 15 минут (`window=+15m` на устройстве). Цена решения: `setWindow` не срабатывает в режиме Doze и откладывается до ближайшего окна обслуживания, если телефон долго лежит неподвижно. Точный будильник отвергнут: `SCHEDULE_EXACT_ALARM` Google Play разрешает только будильникам и календарям.

### Долг и известные края фазы 14

- **Тап по уведомлению открывает приложение, а не вкладку «Молитвы».** Требует обработки intent в `MainActivity` и навигации в `NavGraph` рядом с логикой восстановления пути заметок — отложено, чтобы не трогать хрупкое место в конце фазы.
- **Иконка уведомления системная** (`ic_popup_reminder`) — нужна монохромная иконка приложения, кандидат в фазу 15.
- **После принудительной остановки напоминание молчит до следующего запуска приложения** — ограничение платформы: остановленным приложениям Android не доставляет широковещательные сообщения, включая `BOOT_COMPLETED` и `MY_PACKAGE_REPLACED`.
- **Спящая мина в папках, найдена попутно.** `FolderDao.upsert` тоже на `@Insert(REPLACE)`, а у папок каскад: `notes.folderId ON DELETE SET NULL`, подпапки `ON DELETE CASCADE`. Сейчас безопасно — через `saveFolder` только создаются новые папки, переименование идёт точечным UPDATE. Но любая будущая правка «пересохранить существующую папку» обнулит `folderId` у её заметок и удалит подпапки. Стоит перевести на `@Upsert` заранее.
- **Экраны молитв не покрыты UI-тестами.** Вся логика вынесена в чистые функции с юнит-тестами, но связка экранов с ними проверена только вручную.

Ревью фазы: **Approved with minor issues**. Единственное существенное замечание исправлено — `PrayerReminderReceiver` перевзводил будильник, не проверяя, включено ли напоминание, и при выключении во время доставки сработавшего будильника настройка и будильник расходились.

## Столкновение версий схемы с веткой форматирования

### 2026-09-14
Приложение упало при запуске с `IllegalStateException: Migration didn't properly handle: notes`. Разобрано по шагам и **воспроизведено на эмуляторе**, а не предположено.

**Причина.** Ветка `feature/add-formatting` (3 коммита, июнь 2026, не смержена) объявляет версию базы **2** с колонкой `notes.contentHtml` и своей ручной миграцией 1→2. Фаза 12 подняла версию до **2** со своей колонкой `notes.isPinned`. Room сверяет только **номер** версии: на устройстве, где раньше стояла сборка с форматированием, база уже считается версией 2, поэтому наша миграция 1→2 пропускается, выполняется только 2→3, а затем проверка схемы `notes` находит чужую форму — `contentHtml` есть, `isPinned` нет. Падение повторяется при каждом запуске.

**Кого задевает.** Только устройства, на которых стояла сборка ветки `feature/add-formatting`. У опубликованной версии (`main`, версия базы 1, `1.json` идентичен нашему) путь 1 → 2 → 3 отрабатывает штатно — проверено инструментальными тестами миграций. Релизных тегов у ветки нет, `versionCode` тот же, что в `main`, — наружу она не уходила.

**Что сделано.** Продакшен-код не менялся: закладывать в него терпимость к схеме заброшенной ветки — значит навсегда тащить её историю. Вместо этого проверена процедура восстановления устройства **с сохранением заметок**: в файле базы к `notes` добавляется `isPinned`, убирается `contentHtml`, версия остаётся 2 — дальше приложение само доезжает до версии 3. Прогнано на эмуляторе: заметка, индекс и внешний ключ целы, таблицы молитвенного журнала создаются. Разметка из `contentHtml` при этом теряется (показать её всё равно нечем до фазы 16) и выгружается в отдельный файл.

**Чтобы не повторилось.** Номер версии 2 занят схемой фазы 12. Ветку `feature/add-formatting` нельзя мержить как есть: её схему нужно переносить на версию 4 поверх нашей цепочки — либо делать rich text заново по плану фазы 16, а ветку удалить. Предупреждение добавлено в шапку плана фазы 16.

**Следующий шаг:** фаза 15 — полировка и оставшийся долг.
