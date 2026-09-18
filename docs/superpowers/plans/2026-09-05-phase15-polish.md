# Фаза 15 — Полировка и оставшийся долг

> **For agentic workers:** REQUIRED SUB-SKILL: используйте `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, чтобы выполнять этот план задача-за-задачей. Шаги размечены чекбоксами (`- [ ]`).

**Goal:** Закрыть остаток паритета с iOS (промпт оценки приложения, фиксы редактора) и три расхождения, найденные при аудите Android: мёртвые цвета подсветки в тёмной теме, захардкоженные русские названия переводов и устаревшая документация.

**Что сюда НЕ входит:** приём текста через `ACTION_SEND` и индексация заметок в системном поиске — по решениям Q2 и Q3 (роадмап, раздел 2) они не нужны. Задача про `ACTION_SEND` была в черновике плана и удалена.

**Architecture:** Задачи независимы друг от друга и от фаз 13–14; выполнять можно в любом порядке. Единственная задача с новой зависимостью — 15.1 (Play In-App Review).

**Tech Stack:** Kotlin 2.3.21, Compose, DataStore, Google Play In-App Review, JUnit 4.13.2.

---

## Задача 15.1: Промпт оценки приложения

iOS показывает шторку с просьбой оценить приложение после нескольких запусков. Логика — три ключа и два числа:

- первый показ на **5-м** запуске;
- «напомнить позже» откладывает на **15** запусков (то есть 5 → 20 → 35 → …);
- «оценить» или «нет, спасибо» отключают промпт **навсегда**;
- счётчик запусков **не растёт, пока не пройден онбординг**;
- закрытие шторки свайпом ничего не записывает — промпт появится на следующем запуске.

На Android оценка запрашивается через Google Play In-App Review (`ReviewManager`), который сам решает, показывать ли системный диалог. Наша шторка — «мягкий» предварительный вопрос перед ним, ровно как в iOS.

**Files:**
- Create: `app/src/main/java/ru/edgarakert/biblenote/data/settings/ReviewPromptService.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/ReviewPromptSheet.kt`
- Test: `app/src/test/java/ru/edgarakert/biblenote/data/settings/ReviewPromptServiceTest.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/MainActivity.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, строки

- [ ] **Шаг 1: Написать падающий тест**

Тест проверяет чистое решение «показывать или нет» — без DataStore и без Play-сервисов.

```kotlin
package ru.edgarakert.biblenote.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptServiceTest {

    @Test
    fun `does not prompt before the fifth launch`() {
        for (count in 1..4) {
            assertFalse(
                "launch $count",
                ReviewPromptService.shouldPrompt(
                    launchCount = count, lastShownAtLaunchCount = null, dismissedPermanently = false
                )
            )
        }
    }

    @Test
    fun `prompts on the fifth launch`() {
        assertTrue(
            ReviewPromptService.shouldPrompt(
                launchCount = 5, lastShownAtLaunchCount = null, dismissedPermanently = false
            )
        )
    }

    @Test
    fun `after remind later it stays quiet for fifteen launches`() {
        for (count in 6..19) {
            assertFalse(
                "launch $count",
                ReviewPromptService.shouldPrompt(
                    launchCount = count, lastShownAtLaunchCount = 5, dismissedPermanently = false
                )
            )
        }
        assertTrue(
            ReviewPromptService.shouldPrompt(
                launchCount = 20, lastShownAtLaunchCount = 5, dismissedPermanently = false
            )
        )
    }

    @Test
    fun `dismissing permanently silences it forever`() {
        for (count in 6..100) {
            assertFalse(
                "launch $count",
                ReviewPromptService.shouldPrompt(
                    launchCount = count, lastShownAtLaunchCount = null, dismissedPermanently = true
                )
            )
        }
    }
}
```

- [ ] **Шаг 2: Запустить и убедиться, что падает**

Run: `./gradlew testDebugUnitTest --tests "*ReviewPromptServiceTest*"`
Expected: FAIL — `Unresolved reference: ReviewPromptService`.

- [ ] **Шаг 3: Реализовать**

```kotlin
package ru.edgarakert.biblenote.data.settings

object ReviewPromptService {

    /** Первый показ — на пятом запуске. */
    const val INITIAL_THRESHOLD = 5

    /** «Напомнить позже» откладывает на столько запусков. */
    const val REPEAT_INTERVAL = 15

    /**
     * Отсутствие lastShownAtLaunchCount означает «промпт ещё ни разу не откладывали»,
     * и тогда работает порог первого показа, а не интервал повтора.
     */
    fun shouldPrompt(
        launchCount: Int,
        lastShownAtLaunchCount: Int?,
        dismissedPermanently: Boolean,
    ): Boolean {
        if (dismissedPermanently) return false
        if (lastShownAtLaunchCount == null) return launchCount >= INITIAL_THRESHOLD
        return launchCount - lastShownAtLaunchCount >= REPEAT_INTERVAL
    }
}
```

Ключи DataStore добавить в `SettingsRepository`: `review.launchCount` (Int), `review.lastShownAtLaunchCount` (Int, отсутствие значимо — читать как `Int?`), `review.dismissedPermanently` (Boolean).

- [ ] **Шаг 4: Запустить и убедиться, что проходит**

Run: `./gradlew testDebugUnitTest --tests "*ReviewPromptServiceTest*"`
Expected: PASS, 4 теста.

- [ ] **Шаг 5: Добавить строки**

`values/strings.xml` / `values-ru/strings.xml`:

- `review_prompt_title` — «Enjoying Faith Notes?» / «Нравятся „Заметки веры“?»
- `review_prompt_message` — «If the app helps with your Bible study, please consider leaving a quick review — it really helps.» / «Если приложение помогает вам в изучении Библии, оставьте, пожалуйста, короткий отзыв — это очень важно для нас.»
- `review_prompt_rate` — «★ Leave a Review» / «★ Оставить отзыв»
- `review_prompt_later` — «Remind Me Later» / «Напомнить позже»
- `review_prompt_no_thanks` — «No, Thanks» / «Нет, спасибо»

- [ ] **Шаг 6: Подключить Play In-App Review**

`gradle/libs.versions.toml`:

```toml
play-review = { group = "com.google.android.play", name = "review-ktx", version = "2.0.2" }
```

`app/build.gradle.kts`: `implementation(libs.play.review)`.

Шторка: три кнопки. «Оценить» → закрыть шторку → `ReviewManagerFactory.create(context)` → `launchReviewFlow` → отметить «отключено навсегда». «Напомнить позже» → записать текущий `launchCount`. «Нет, спасибо» → отключить навсегда.

- [ ] **Шаг 7: Считать запуск**

В `MainActivity` — увеличивать `review.launchCount` **только после пройденного онбординга** и показывать шторку, если `shouldPrompt(...)` вернул `true`.

- [ ] **Шаг 8: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ app/src/test/java/ru/edgarakert/biblenote/data/settings/ReviewPromptServiceTest.kt gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/
git commit -m "feat: ask for a review after repeated launches"
```

---

## Задача 15.2: Цвета подсветки в тёмной теме

Найдено при аудите. `HighlightColor` объявляет пары `lightColor`/`darkColor`, но `darkColor` **не используется нигде в кодовой базе** — и читалка, и шторка, и палитра выбора цвета всегда берут `lightColor`. В тёмной теме на тёмном фоне рисуются светлые пастельные плашки.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/data/bible/HighlightColor.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleVerse.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/bible/BibleReaderScreen.kt` (палитра выбора)

- [ ] **Шаг 1: Добавить выбор по теме**

В `HighlightColor`:

```kotlin
    /** Плашка подсветки под текущую тему. darkColor до этого был мёртвым кодом. */
    @Composable
    fun surfaceColor(): Color = if (isSystemInDarkTheme()) darkColor else lightColor
```

Лучше — прокидывать `darkTheme: Boolean` параметром, потому что тема в приложении управляется настройкой `AppearanceMode`, а не только системой. Взять то же значение, что `MainActivity` передаёт в `BibleNoteTheme`.

- [ ] **Шаг 2: Использовать во всех трёх местах**

`BibleVerse` (фон стиха), `ColorPicker` в читалке (кружки выбора) и шторка стиха.

- [ ] **Шаг 3: Проверить**

Run: `./gradlew installDebug`
Expected: в тёмной теме подсветки — приглушённые тёмные, текст читается; в светлой ничего не изменилось.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/
git commit -m "fix: use the dark highlight swatches in dark theme"
```

---

## Задача 15.3: Локализованные названия переводов

Найдено при аудите. Названия переводов захардкожены по-русски в двух приватных функциях `BibleReaderScreen.kt` (`translationDisplayName`, `translationShortName` → «Синодальный», «Синод.»), а в `BibleVerseSheet.kt` выводятся сырым `t.uppercase()` — то есть буквально «SYNODAL» и «KJV». При этом ключи `translation_synodal_name`, `translation_kjv_name` и подписи к ним **уже есть в обеих локалях**.

**Files:**
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/screens/bible/BibleReaderScreen.kt`
- Modify: `app/src/main/java/ru/edgarakert/biblenote/ui/components/BibleVerseSheet.kt`
- Create: `app/src/main/java/ru/edgarakert/biblenote/ui/components/TranslationLabels.kt`

- [ ] **Шаг 1: Вынести общий хелпер**

```kotlin
package ru.edgarakert.biblenote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.edgarakert.biblenote.R

/** Одно место для человекочитаемых названий переводов вместо трёх разных вариантов. */
@Composable
fun translationName(translation: String): String = when (translation) {
    "synodal" -> stringResource(R.string.translation_synodal_name)
    "kjv" -> stringResource(R.string.translation_kjv_name)
    else -> translation.uppercase()
}
```

Добавить короткие варианты (`translation_synodal_short` = «Синод.» / «Synodal», `translation_kjv_short` = «KJV») в обе локали и хелпер `translationShortName`.

- [ ] **Шаг 2: Заменить все три места использования**

Удалить приватные функции из `BibleReaderScreen.kt` и `uppercase()` в `BibleVerseSheet.kt`.

- [ ] **Шаг 3: Проверить**

Run: `./gradlew installDebug`
Expected: переключатель перевода в шторке показывает «Синодальный» / «KJV», а не «SYNODAL». При системном английском — английские названия.

- [ ] **Шаг 4: Коммит**

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/ app/src/main/res/
git commit -m "fix: localize translation names instead of hardcoding them"
```

---

## Задача 15.4: Привести документацию в соответствие с кодом

`CLAUDE.md` расходится с кодом — это сбивает и людей, и агентов.

**Files:**
- Modify: `CLAUDE.md`
- Modify: `docs/plan.md`, `docs/progress.md`, `CHANGELOG.md`

- [ ] **Шаг 1: Исправить раздел про переводы**

`CLAUDE.md` утверждает: «Переводы: `"synodal"`, `"nrt"`, `"kjv"`, `"niv"`» и «4 перевода, ~25 МБ». В коде `SettingsRepository.ALL_TRANSLATIONS = listOf("synodal", "kjv")`, а `nrt` и `niv` закомментированы повсюду. Привести к фактическому состоянию: два активных перевода, остальные отключены.

- [ ] **Шаг 2: Обновить структуру проекта**

После фаз 13–14 в `CLAUDE.md` нужно добавить: `data/prayer/`, `ui/screens/prayer/`, `ui/components/prayer/`, `NoteVerseIndexService`, `VerseSnippetBuilder`, `NoteContentWriter`, четвёртую вкладку в разделе «Навигация» и `PrayerRequest`/`PrayerEntry` в разделе моделей.

- [ ] **Шаг 3: Добавить фазы 12–15 в `docs/plan.md`** и итоги в `docs/progress.md`.

- [ ] **Шаг 4: Коммит**

```bash
git add CLAUDE.md docs/ CHANGELOG.md
git commit -m "docs: sync project docs with the actual code after iOS parity work"
```

---

## Задача 15.5: Проверка поведения редактора у клавиатуры

В iOS вышли три фикса редактора (`cf1127a` — восстановлен скролл, `ab03325` — зазор между кареткой и клавиатурой, `8fbf201` — нижний отступ над клавиатурой). Они специфичны для UIKit, но класс проблем на Android тот же: `BibleEditText` — это `EditText` внутри `AndroidView`, и поведение при появлении клавиатуры нужно проверить, а не переносить код.

Это задача-проверка: если проблем нет — закрыть её без правок и записать это в `docs/progress.md`.

- [ ] **Шаг 1: Проверить три сценария на устройстве**

1. Длинная заметка (несколько экранов текста): поставить курсор в конец → клавиатура не перекрывает каретку, текст скроллится.
2. Печать в середине длинной заметки: каретка остаётся видимой, между ней и клавиатурой есть зазор.
3. Поворот экрана с открытой клавиатурой: позиция курсора и скролл сохраняются (за это отвечает `CursorTrackingEditText` + `rememberSaveable`, добавленные в фазе 11).

- [ ] **Шаг 2: Починить найденное**

Если каретка перекрывается — добавить нижний отступ контента и `windowInsets`-обработку в контейнере редактора. Если сломан скролл — проверить, что `EditText` не имеет `isSingleLine` и что родительский контейнер не перехватывает вертикальный скролл.

- [ ] **Шаг 3: Коммит** (только если были правки)

```bash
git add app/src/main/java/ru/edgarakert/biblenote/ui/
git commit -m "fix: keep the caret visible above the keyboard in the note editor"
```

---

## Завершение фазы и всей работы

- [ ] **Прогнать всё**

Run: `./gradlew test && ./gradlew assembleDebug`
Expected: все тесты зелёные, сборка успешна.

- [ ] **Пройти Definition of done** из роадмапа (`2026-09-05-ios-parity-roadmap.md`, раздел 6) — на этом паритет с iOS достигнут.

- [ ] **Перейти к фазе 16** (rich text) — она сверх паритета, по решению Q1.

- [ ] **Поднять версию** в `app/build.gradle.kts` (`versionCode`, `versionName`) и записать релизные заметки в `CHANGELOG.md`.

- [ ] **Ревью субагентом** всей работы: диффа от точки начала фазы 12 до конца фазы 15.
