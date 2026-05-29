# Changelog

## [Unreleased]

---

## 2026-05-29 — Реализация фазы 11: Share Intent

### Добавлено
- `ShareReceiverActivity` — отдельная Activity с прозрачной темой, регистрируется как обработчик `android.intent.action.SEND / text/plain`
- `ShareNoteViewModel` — управляет состоянием, сохраняет новую заметку или добавляет текст в конец существующей
- `ShareNoteSheetContent` — Compose UI: превью текста, RadioGroup, поиск по заметкам, кнопки Save/Cancel
- `NoteDao.observeAllNotes()` + `NoteRepository.observeAllNotes()` — новый метод для отображения всех заметок в пикере
- `Theme.BibleNote.Transparent` — стиль для прозрачного окна Activity (через `windowIsTranslucent`)
- Строки `share_*` в EN и RU локализациях

### Техническое
- ShareReceiverActivity использует `ModalBottomSheet` из Material 3 — стандартная анимация снизу, закрытие свайпом
- Заголовок новой заметки = первая непустая строка ≤ 60 символов (`extractTitle`)
- Добавление в существующую — текст вставляется в конец с `\n\n`
- Koin: `ShareNoteViewModel` зарегистрирован в `AppModule`
- После ревью субагента исправлены: `LaunchedEffect` разделён, `isSaving` сбрасывается в `finally`, `WhileSubscribed(0)`, `selectedNoteId` в ViewModel, RadioButton `onClick = null`
- Сборка: BUILD SUCCESSFUL

---

## 2026-05-29 — Планирование фазы 11: Share Intent

### Добавлено в plan.md
- Фаза 11 «Share Intent» — приём шаренного текста из других приложений
- Описан UX-флоу: выбор «новая заметка / добавить в существующую»
- Зафиксированы технические решения: `ShareReceiverActivity`, `ShareNoteViewModel`, `ShareNoteScreen`
- Зафиксированы решения по заголовку (первая строка ≤ 60 символов) и позиции вставки (конец заметки)

---

## 2026-05-29 — Исправление смены темы (feature/fix-theme-switch)

### Исправлено
- Смена цветовой темы (светлая/тёмная) через настройки — тема не применялась при переключении
- Передача `isDarkTheme` через NavGraph и все экраны
- Явная передача цветов темы во все компоненты: NoteRow, FolderRow, BibleVerse, SelectionActionBar, BibleVerseSheet, MoveFolderSheet, BibleEditText
- Убраны хардкод-цвета (MaterialTheme.colorScheme) заменены на явные параметры темы

### Рефакторинг
- `BibleEditText.kt`: переменная `currentAmberArgb` упрощена с `intArrayOf(amberArgb)` до простого `Int`

---

## Ранняя история (из git-лога)

| Коммит | Описание |
|--------|----------|
| `622762a` | Обновлена иконка приложения |
| `d4cb0db` | Добавлен `BibleReferenceParserTest` |
| `fd63816` | Добавлены папки (FolderScreen, MoveFolderSheet) |
| `f46dfe2` | Добавлена навигация (NavGraph, BottomNavigation) |
| `e74a2ad` | Добавлены Settings и Onboarding экраны |
| `90cf6d0` | Создан BibleReaderScreen + BibleBookPickerScreen |
| `d73cf4f` | Добавлена шторка стиха (BibleVerseSheet) |
| `85ae90d` | Создан NoteEditorScreen |
| `39d591f` | Создан NotesListScreen |
| `b473bdb` | Обновлены зависимости |
| `01d1ba5` | Перенесён дата-слой (Room, BibleDB, DataStore) |
| `75c76f7` | Создан проект |