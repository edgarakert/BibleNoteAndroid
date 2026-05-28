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
- [ ] NotesListScreen (список заметок + папок, поиск)
- [ ] NoteRow, FolderRow
- [ ] SelectionActionBar (режим выделения)
- [ ] Диалоги: новая папка, переименовать, удалить папку
- [ ] NotesViewModel

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
- [ ] BottomNavigation (3 вкладки: Заметки, Библия, Настройки)
- [ ] NavGraph: вложенные графы для каждой вкладки
- [ ] Связь шторки стиха с вкладкой Библии (через shared state / NavigationViewModel)

### Фаза 9 — Папки
- [ ] FolderScreen (список заметок внутри папки)
- [ ] MoveFolderScreen (переместить заметки в папку)

### Фаза 10 — Полировка
- [ ] Иконка приложения
- [ ] Локализация (ru/en strings)
- [ ] Edge-to-edge UI
- [ ] Анимации переходов
- [ ] Unit-тесты BibleReferenceParser

---

## Соглашения

1. Всегда проверять официальную документацию Android, использовать best practices
2. Вести docs/plan.md и docs/progress.md
3. Декомпозировать задачи и делегировать субагентам
4. Использовать нужные скиллы
5. После каждой задачи — ревью субагент