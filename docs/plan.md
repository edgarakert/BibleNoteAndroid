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

### Фаза 11 — Share Intent (приём шаренного текста)

**Цель:** пользователь выделяет текст в любом приложении, выбирает «Поделиться» → BibleNote, и сохраняет его в новую или существующую заметку.

#### Технический контракт

- Intent action: `android.intent.action.SEND`, MIME: `text/plain`
- Текст читается из `intent.getStringExtra(Intent.EXTRA_TEXT)`
- Отдельная `Activity` (`ShareReceiverActivity`) — не загрязняет `MainActivity`
- `ShareReceiverActivity` оформлена с прозрачным фоном + `Dialog`-тема, чтобы выглядела как шторка поверх чужого приложения

#### UX-флоу

1. Пользователь шарит текст → открывается `ShareReceiverActivity`
2. Показывается экран выбора:
   - Preview шаренного текста (первые ~3 строки, с «...»)
   - Radio: **Создать новую заметку** / **Добавить в существующую**
   - При «Существующую» — список заметок (LazyColumn, поиск по названию)
   - Кнопки «Отмена» и «Сохранить»
3. **Новая заметка:**
   - Заголовок = первая непустая строка текста, обрезанная до 60 символов
   - Содержимое = весь шаренный текст
   - После сохранения: Activity закрывается
4. **Добавить в существующую:**
   - Текст добавляется в конец заметки с отступом `\n\n`
   - Activity закрывается после сохранения

#### Задачи

- [x] `AndroidManifest.xml`: объявить `ShareReceiverActivity` с `intent-filter` (`ACTION_SEND`, `text/plain`)
- [x] `ShareReceiverActivity`: `ComponentActivity` с Dialog-темой, запускает Compose-контент
- [x] `ShareNoteViewModel`: `StateFlow<ShareUiState>` (режим, список заметок, текст), методы `saveAsNewNote()`, `appendToNote(noteId)`
- [x] `ShareNoteScreen`: Composable с preview-текстом, RadioGroup, условный список заметок с поиском
- [x] `extractTitle(text: String): String` — первая непустая строка ≤ 60 символов
- [x] Koin: добавить `ShareNoteViewModel` в `AppModule`
- [x] Ревью субагент после реализации

#### Принятые решения

| Тема | Решение |
|------|---------|
| Папка для новой заметки | Без папки (root) |
| Шаренный текст пустой | Показать ошибку, не сохранять |
| Открыть редактор после сохранения | Нет — Activity закрывается |
| Заголовок новой заметки | Первая непустая строка ≤ 60 символов |
| Позиция текста в существующей | Конец заметки с `\n\n` |

---

## Соглашения

1. Всегда проверять официальную документацию Android, использовать best practices
2. Вести docs/plan.md и docs/progress.md
3. Декомпозировать задачи и делегировать субагентам
4. Использовать нужные скиллы
5. После каждой задачи — ревью субагент