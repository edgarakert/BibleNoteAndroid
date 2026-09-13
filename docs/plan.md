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

## Соглашения

1. Всегда проверять официальную документацию Android, использовать best practices
2. Вести docs/plan.md и docs/progress.md
3. Декомпозировать задачи и делегировать субагентам
4. Использовать нужные скиллы
5. После каждой задачи — ревью субагент
---

## Фазы 12–15 — Догнать iOS по функционалу

Ресерч проведён 2026-09-05. Точка синхронизации Android с iOS — коммит `a4e6862`;
после неё в iOS ушло 15 коммитов. Полная карта расхождений, открытые вопросы и риски —
в [docs/superpowers/plans/2026-09-05-ios-parity-roadmap.md](superpowers/plans/2026-09-05-ios-parity-roadmap.md).

### Фаза 12 — Фундамент и быстрые победы ✅
[План](superpowers/plans/2026-09-05-phase12-foundation-quick-wins.md) · 8 задач
- [x] Хелперы `BibleReference`: `coveredVerses`, `formatVerseSpec`, `replacementText`
- [x] Рабочие автомиграции Room (сейчас любой bump версии роняет установленные приложения)
- [x] Закрепление заметок отдельной секцией (`isPinned`)
- [x] Multiline-заголовок заметки, лимит 4 строки
- [x] Православный порядок книг НЗ для Синодального перевода
- [x] Запоминание последней открытой папки/заметки
- [x] Фикс воскрешения удалённой заметки через debounce-автосохранение

Сверх плана, найдено при ревью задач фазы:
- [x] Long-press-меню заметки доступно через TalkBack (`combinedClickable` вместо `pointerInput`)
- [x] Запуск не мигает онбордингом и чужой темой — начальные значения читаются с диска

### Фаза 13 — Связка Библия ↔ заметки ✅
[План](superpowers/plans/2026-09-05-phase13-bible-notes-integration.md) · 8 задач
- [x] Счётчик упоминаний стиха в заметках + шторка со списком заметок
- [x] Полная глава в шторке стиха со скроллом к нужному стиху
- [x] Правка ссылки в заметке тапом по стиху в шторке
- [x] Сохранение выделенных стихов в новую или существующую заметку

Сверх плана, найдено при ревью задач фазы:
- [x] Восстановление после отброшенной правки: раньше редактор молча оставался
      рассинхронизирован с текстом и промахивался мимо ссылки на каждом следующем тапе
- [x] Выделение стиха в шторке озвучивается screen reader (`toggleable` вместо `clickable`)
- [x] `NoteContentWriter`: заметка из одних пробелов считается пустой, как и сниппет

### Фаза 14 — Молитвенный журнал ✅
[План](superpowers/plans/2026-09-05-phase14-prayer-journal.md) · 13 задач
- [x] Сущности, миграция 2→3, DAO, репозиторий
- [x] Бизнес-логика: действия над просьбой, подборка «на сегодня», напоминания
- [x] Четвёртая вкладка и семь экранов

Поправки к плану, без которых фича не работала бы:
- [x] `@Upsert` вместо `@Insert(REPLACE)` — REPLACE с каскадом стирал дописки при любом обновлении просьбы
- [x] Канал уведомлений — без него на minSdk 26 напоминание молча не показывалось ни на одном устройстве
- [x] Стихи в редакторе разбираются парсером, а не делением по запятой — запятая входит в синтаксис ссылки
- [x] Дата ответа из `DatePicker` — полночь по UTC, без конвертации дата уезжала на сутки

Сверх плана:
- [x] Перевзвод напоминания при холодном старте и после обновления приложения
- [x] Подсказка про настройки системы после отказа в разрешении на уведомления

**Ждёт решения:** точность напоминания — сейчас окно доставки до часа (см. `docs/progress.md`)

### Фаза 15 — Полировка и оставшийся долг
[План](superpowers/plans/2026-09-05-phase15-polish.md) · 5 задач
- [ ] Промпт оценки приложения после N запусков
- [ ] Цвета подсветки в тёмной теме (`darkColor` — мёртвый код)
- [ ] Локализованные названия переводов вместо захардкоженных
- [ ] Синхронизация `CLAUDE.md` с фактическим кодом
- [ ] Проверка поведения редактора у клавиатуры

**После фазы 15 паритет с iOS достигнут.**

### Фаза 16 — Rich text (сверх паритета)
[План](superpowers/plans/2026-09-05-phase16-rich-text.md) · 6 задач
- [ ] Колонка форматирования в `Note` + автомиграция 3→4
- [ ] Кодек диапазонов форматирования (свой формат вместо HTML — `fromHtml` не читает размер шрифта обратно)
- [ ] `BibleEditText`: применение и извлечение `StyleSpan` / `RelativeSizeSpan`
- [ ] Панель форматирования: жирный, курсив, размер
- [ ] Доработка `NoteContentWriter` под форматирование
- [ ] Сквозная проверка: старые заметки, ссылки внутри форматирования, поиск, поворот экрана

### Принятые решения (2026-09-05)
- **Rich text — портируем**, но после паритета (фаза 16)
- **Приём `ACTION_SEND` — не нужен** пока
- **Индексация в системном поиске — не нужна** пока
- **Порядок книг Синодального — только для `synodal`**: русский перевод в приложении один
