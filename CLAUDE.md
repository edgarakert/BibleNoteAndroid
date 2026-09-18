# BibleNoteAndroid — CLAUDE.md

Android-приложение для заметок с автоматическим распознаванием ссылок на библейские стихи.
Написано на Kotlin + Jetpack Compose. Порт iOS-приложения BibleNote.

---

## Источник

| Тип | Путь |
|-----|------|
| iOS SwiftUI (единственный референс) | `/Users/edgarakert/Works/BibleNote/BibleNote/` |
| Документация прогресса | `docs/progress.md` |
| План реализации | `docs/plan.md` |

> Папки `androidApp/` и `sharedLogic/` в BibleNote — старые артефакты KMP, не используются.

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

## Рабочие соглашения

1. **Официальная документация** — всегда проверять актуальную документацию Android и используемых библиотек, следовать свежим best practices
2. **Документация** — обновлять `docs/plan.md` и `docs/progress.md` после каждой задачи
3. **Декомпозиция** — разбивать задачи на мелкие и делегировать субагентам
4. **Скиллы** — использовать доступные скиллы (kotlin-build, kotlin-review, android-development-assistant и т.д.)
5. **Ревью** — после каждой задачи создавать субагента для ревью изменений

---

## Структура проекта

```
app/src/main/
├── java/ru/edgarakert/biblenote/
│   ├── MainActivity.kt
│   ├── BibleNoteApplication.kt      — Application, запускает Koin (startKoin)
│   ├── di/AppModule.kt              — Koin модуль (все зависимости)
│   ├── data/
│   │   ├── db/                      — Room: Note, Folder, DAO, AppDatabase
│   │   ├── bible/                   — BibleDatabaseService, BibleReferenceParser
│   │   │                              модели: Book, Verse, VerseHighlight, HighlightColor
│   │   └── settings/                — SettingsRepository (DataStore)
│   ├── ui/
│   │   ├── theme/                   — BibleNoteTheme, цвета, типографика
│   │   ├── navigation/              — NavGraph, BottomNavigation (3 вкладки)
│   │   ├── screens/
│   │   │   ├── notes/               — NotesListScreen, FolderScreen, MoveFolderScreen
│   │   │   ├── editor/              — NoteEditorScreen
│   │   │   ├── bible/               — BibleReaderScreen, BibleBookPickerScreen
│   │   │   ├── settings/            — SettingsScreen, TranslationSettingsScreen,
│   │   │   │                          BibleThemeSettingsScreen, AboutScreen
│   │   │   └── onboarding/          — OnboardingScreen, TranslationSelectionScreen
│   │   ├── components/              — BibleEditText (AndroidView + Spannable),
│   │   │                              BibleVerseSheet (ModalBottomSheet),
│   │   │                              NoteRow, FolderRow, BibleVerse, SelectionActionBar
│   │   └── viewmodels/              — NotesViewModel, NoteEditorViewModel,
│   │                                  BibleReaderViewModel, SettingsViewModel
├── assets/
│   └── bible.sqlite                 — books + verses (4 перевода) + verse_highlights, ~25 МБ
└── res/
    ├── values/strings.xml           — en строки
    └── values-ru/strings.xml        — ru строки
```

---

## Навигация

Три вкладки нижней навигации (аналог iOS TabView):
1. **Заметки** — список заметок/папок → редактор заметки
2. **Библия** — выбор книги → чтение главы
3. **Настройки** — экраны настроек

---

## Дизайн-система

Концепция «Sacred Manuscript» — тёплый пергамент, чернильный текст, янтарный акцент.

| Токен | HEX | Назначение |
|-------|-----|-----------|
| `Parchment` | `#F6EFE4` | фон всех экранов |
| `Ink` | `#1D1711` | основной текст |
| `Amber` | `#C29A3D` | ссылки, акценты, активные элементы |
| `AmberSoft` | `#E6CF99` | пустые состояния, декоративные иконки |
| `WarmGray` | `#7A6E60` | вторичный текст, иконки |
| `CardSurface` | `#FDFAF5` | карточки заметок и папок |
| `Hairline` | `#D6CCBE` | разделители |

- Светлая тема по умолчанию; dark mode — через настройку AppearanceMode
- Serif шрифты (`FontFamily.Serif`) для заголовков и контента заметок
- Карточки: `RoundedCornerShape(12.dp)`

---

## База данных библии (bible.sqlite)

Схема (SQLite, read-only кроме verse_highlights):

```sql
books            (id, name_ru, name_en, abbreviation)
verses           (id, translation, book_id, chapter, verse, text)
verse_highlights (book_id, chapter, verse_number, color_name, created_at)
                 -- PK: (book_id, chapter, verse_number)
```

Переводы: `"synodal"`, `"nrt"`, `"kjv"`, `"niv"`

При первом запуске bible.sqlite копируется из assets в `filesDir`. При обновлении версии БД — перекопировать.

---

## Настройки (DataStore)

| Ключ | Тип | Значения |
|------|-----|---------|
| `defaultTranslation` | String | `"synodal"` / `"nrt"` / `"kjv"` / `"niv"` |
| `enabledTranslations` | StringSet | подмножество переводов, по умолчанию — все |
| `appearanceMode` | String | `"system"` / `"light"` / `"dark"` |
| `verseScale` | Float | 0.8–1.4, default 1.0 |
| `onboardingCompleted` | Boolean | false → показать онбординг |

---

## Ключевые архитектурные решения

### Редактор: AndroidView + EditText, не Compose TextField
Compose `TextField` не поддерживает `Spannable` при вводе. `EditText` завёрнут в `AndroidView` — полный контроль над `Spannable`.

### Подсветка ссылок: ForegroundColorSpan + ClickableSpan
`BibleReferenceParser.parse(text)` возвращает ссылки с позициями. На каждую ссылку: `ForegroundColorSpan(amber)` + `ClickableSpan`. Тап через `setOnTouchListener`.

### Шторка стиха: ModalBottomSheet
При тапе на ссылку — `ModalBottomSheet` с текстом стиха, переключателем перевода и кнопкой «Открыть главу» (переключает на вкладку Библии).

### Форматирование текста: диапазоны отдельной колонкой, не HTML
Жирный/курсив/размер хранятся не в `content` (он остаётся плоским текстом — на нём завязаны поиск и превью), а в колонке `Note.formatting: String?` — список диапазонов (`NoteFormattingCodec`, `data/db/NoteFormatting.kt`), сериализованный в компактную строку своего формата: `"b:0-10;i:12-20;s1.3:22-30"`. `HtmlCompat.fromHtml`/`Html.toHtml` не подошёл бы — штатный конвертер не читает `font-size` обратно из атрибута `style`, поэтому размер шрифта не пережил бы цикл «сохранили → открыли». `BibleEditText` применяет/извлекает диапазоны как `StyleSpan`/`RelativeSizeSpan` на живом `Editable` (хелперы — `ui/components/NoteFormattingSpans.kt`, отдельно от файла компонента, чтобы не смешивать с логикой подсветки ссылок); `applyHighlighting` трогает только свои `ForegroundColorSpan`/`ClickableSpan`, поэтому ссылка на стих внутри жирного текста остаётся кликабельной. Кнопка форматирования работает и по выделению (снимает при повторном нажатии, с корректным разрезанием спана при частичном снятии), и в «режиме ввода» без выделения — как в iOS: переключил стиль, дальше печатается с ним, пока не нажать ещё раз.

### Модели Room
```
Note   (id, title, content, folderId?, formatting?, createdAt, updatedAt)
Folder (id, name, parentId?, createdAt)
```

### Подсветка стихов: HighlightColor
Пять цветов: `amber`, `rose`, `sage`, `sky`, `lavender`. Хранятся в `verse_highlights` в bible.sqlite.

### Автосохранение заметок
`NoteEditorViewModel` — `debounce` на `StateFlow` контента/заголовка. При `onCleared` — немедленное сохранение. Пустая заметка удаляется.

### Koin (DI)
Koin 4.x с BOM. Модуль `di/AppModule.kt` объявляет все зависимости. `BibleNoteApplication.onCreate()` вызывает `startKoin { androidContext(this@BibleNoteApplication); modules(appModule) }`. ViewModel-ы получаются через `koinViewModel()` в Composable-функциях.

---

## Фазы реализации (см. docs/plan.md)

1. Основа (зависимости, тема, MainActivity)
2. Данные (Room, BibleReferenceParser, BibleDatabaseService, SettingsRepository)
3. UI: Список заметок
4. UI: Редактор заметок
5. UI: Шторка стиха
6. UI: Читалка Библии (BibleReader + BookPicker)
7. UI: Настройки и онбординг
8. Навигация (TabBar + NavGraph)
9. Папки (FolderScreen, MoveFolderScreen)
10. Полировка (иконка, локализация, анимации, тесты)

---

## Команды

```bash
./gradlew assembleDebug
./gradlew test
./gradlew installDebug
```