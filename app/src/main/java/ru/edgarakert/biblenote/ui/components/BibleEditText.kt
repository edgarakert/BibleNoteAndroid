package ru.edgarakert.biblenote.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec
import kotlin.math.abs

/**
 * Одноразовая правка текста извне редактора. token отсекает повторное применение.
 *
 * [formatting] — стиль вставляемого текста, смещения от начала [text]. null — стили не
 * трогаются (замена ссылки на месте); список, даже пустой, — вставка получает ровно эти стили
 * и не наследует стиль соседнего текста, к которому прилипла.
 */
data class PendingEdit(
    val token: Long,
    val start: Int,
    val end: Int,
    val text: String,
    val formatting: List<FormatRun>? = null,
)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BibleEditText(
    text: String,
    formatting: List<FormatRun>,
    onContentChanged: (text: String, runs: List<FormatRun>) -> Unit,
    onReferenceTapped: (BibleReference) -> Unit,
    parser: BibleReferenceParser,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    initialCursorPosition: Int = -1,
    onCursorPositionChanged: (Int) -> Unit = {},
    onActiveFormatsChanged: (Set<FormatType>) -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    formatCommand: FormatCommand? = null,
    onFormatCommandApplied: (token: Long) -> Unit = {},
    pendingEdit: PendingEdit? = null,
    /** Вызывается ровно один раз на токен; applied=false, если границы не подошли к живому тексту. */
    onPendingEditApplied: (token: Long, applied: Boolean) -> Unit = { _, _ -> },
) {
    val amberArgb = MaterialTheme.colorScheme.primary.toArgb()
    val inkArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintArgb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()
    val mutedArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val onContentChangedState = rememberUpdatedState(onContentChanged)
    val onReferenceTappedState = rememberUpdatedState(onReferenceTapped)
    val onCursorPositionChangedState = rememberUpdatedState(onCursorPositionChanged)
    val onActiveFormatsChangedState = rememberUpdatedState(onActiveFormatsChanged)
    val onFocusChangedState = rememberUpdatedState(onFocusChanged)

    val handler = remember { Handler(Looper.getMainLooper()) }
    val pendingHighlight = remember { arrayOfNulls<Runnable>(1) }
    val currentAmberArgb = remember { intArrayOf(amberArgb) }
    val currentInkArgb = remember { intArrayOf(inkArgb) }
    val currentMutedArgb = remember { intArrayOf(mutedArgb) }
    // Токен последней применённой внешней правки — отсекает повторное применение
    // при рекомпозиции, пока вызывающая сторона ещё не успела сбросить pendingEdit в null.
    val lastAppliedToken = remember { mutableLongStateOf(-1L) }
    // Тот же приём для formatCommand — тулбар сбрасывает его в null асинхронно.
    val lastAppliedFormatToken = remember { mutableLongStateOf(-1L) }
    // title/content/formatting — три независимых StateFlow во ViewModel; formatting может
    // прийти отдельной рекомпозицией без изменения text (например, если открыть заметку до
    // того, как отработает декодирование formatting). Без этого applyFormatting вызывалась бы
    // только вместе со сменой текста и рисковала молча пропустить применение форматирования.
    val lastAppliedFormatting = remember { arrayOf<List<FormatRun>?>(null) }

    DisposableEffect(Unit) {
        onDispose { pendingHighlight[0]?.let { handler.removeCallbacks(it) } }
    }

    AndroidView(
        factory = { context ->
            CursorTrackingEditText(context).apply {
                background = null
                setTextColor(currentInkArgb[0])
                textSize = 17f
                typeface = Typeface.SERIF
                hint = placeholder
                setHintTextColor(hintArgb)
                gravity = Gravity.TOP or Gravity.START

                val density = context.resources.displayMetrics.density
                val padH = (16 * density).toInt()
                val padV = (12 * density).toInt()
                setPadding(padH, padV, padH, padV)

                selectionListener = { pos -> onCursorPositionChangedState.value(pos) }
                activeFormatsListener = { formats -> onActiveFormatsChangedState.value(formats) }
                setOnFocusChangeListener { _, hasFocus -> onFocusChangedState.value(hasFocus) }

                // Тап по ссылке отслеживается с ACTION_DOWN: EditText в фокусе сам обрабатывает
                // DOWN/MOVE (ставит каретку, начинает её перетаскивание при малейшем сдвиге
                // пальца), и если ловить только ACTION_UP, курсор успевает уехать на ссылку.
                // DOWN/MOVE по-прежнему отдаём EditText — иначе сломается долгое нажатие
                // (выделение текста ссылки), — а на UP короткого тапа отменяем жест для
                // EditText и возвращаем каретку туда, где она была до касания.
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                val longPressTimeout = ViewConfiguration.getLongPressTimeout()
                // Факт «палец опустился на ссылку». Сам спан на UP ищем заново: applyHighlighting
                // пересоздаёт все BibleClickSpan (в т.ч. по debounce после ввода), и объект,
                // найденный на DOWN, к моменту UP может быть уже снят с текста.
                var downOnLink = false
                var downX = 0f
                var downY = 0f
                var downSelStart = -1
                var downSelEnd = -1

                setOnTouchListener { view, event ->
                    val editText = view as EditText
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downOnLink = editText.bibleSpanAt(event.x, event.y) != null
                            downX = event.x
                            downY = event.y
                            downSelStart = editText.selectionStart
                            downSelEnd = editText.selectionEnd
                            false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (downOnLink &&
                                (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop)
                            ) {
                                downOnLink = false
                            }
                            false
                        }

                        MotionEvent.ACTION_UP -> {
                            val wasOnLink = downOnLink
                            downOnLink = false
                            if (!wasOnLink || event.eventTime - event.downTime >= longPressTimeout) {
                                return@setOnTouchListener false
                            }
                            val span = editText.bibleSpanAt(event.x, event.y)
                                ?: return@setOnTouchListener false

                            val spannable = editText.text
                            val liveStart = spannable.getSpanStart(span)
                            val liveEnd = spannable.getSpanEnd(span)
                            if (liveStart < 0 || liveEnd > spannable.length) {
                                return@setOnTouchListener false
                            }

                            // Сбрасываем начатый EditText жест (pressed, long-press, перетаскивание
                            // каретки) и возвращаем каретку, если она успела сдвинуться.
                            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                            editText.onTouchEvent(cancel)
                            cancel.recycle()
                            val length = spannable.length
                            if (downSelStart in 0..length && downSelEnd in 0..length &&
                                (editText.selectionStart != downSelStart || editText.selectionEnd != downSelEnd)
                            ) {
                                editText.setSelection(downSelStart, downSelEnd)
                            }

                            // Диапазон и текст берём из живого Editable, а не из момента
                            // разбора: пока пользователь печатал, спан мог сдвинуться.
                            onReferenceTappedState.value(
                                span.reference.copy(
                                    startIndex = liveStart,
                                    endIndex = liveEnd,
                                    displayText = spannable.subSequence(liveStart, liveEnd).toString()
                                )
                            )
                            view.performClick()
                            true
                        }

                        // Второй палец — это уже не тап по ссылке.
                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                            downOnLink = false
                            false
                        }

                        else -> false
                    }
                }

                addTextChangedListener(object : android.text.TextWatcher {
                    // Границы последней вставки: afterTextChanged применяет к ним
                    // стили из pendingTypingFormats («режим ввода» без выделения).
                    private var insertStart = -1
                    private var insertCount = 0

                    // Снимок заменяемого текста и стиля каждого его символа. Клавиатура при наборе
                    // заменяет слово целиком (composing text): без снимка «новым» считалось бы всё
                    // слово, и включённый стиль красил бы и старые буквы.
                    private var oldText = ""
                    private var oldFormats: List<Set<FormatType>> = emptyList()

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        oldText = ""
                        oldFormats = emptyList()
                        if (isProgrammatic || count <= 0 || s !is android.text.Editable) return
                        oldText = s.subSequence(start, start + count).toString()
                        oldFormats = (start until start + count).map { i ->
                            FormatType.entries.filter { hasStyle(s, i, i + 1, it) }.toSet()
                        }
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        insertStart = start
                        insertCount = count
                        // Каретка сдвигается при вставке ДО afterTextChanged; пока стили нового
                        // текста не выставлены, onSelectionChanged не должен пересобирать режим
                        // ввода из «унаследованного» окружения — иначе он затрёт выбор пользователя.
                        if (!isProgrammatic && count > 0) isInserting = true
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (isProgrammatic) return
                        val editable = s ?: return

                        try {
                            if (insertCount > 0) {
                                val from = insertStart
                                val to = insertStart + insertCount
                                // Спаны растут вправо (SPAN_EXCLUSIVE_INCLUSIVE), поэтому текст,
                                // набранный вплотную к жирному слову, сам становится жирным.
                                // Режим ввода должен работать в обе стороны: включённые стили
                                // добавляем, выключенные — снимаем с только что вставленного.
                                // Незатронутые символы (общий префикс/суффикс со старым текстом)
                                // получают обратно свой прежний стиль, режим ввода — только новые.
                                val newText = editable.subSequence(from, to)
                                val oldLen = oldText.length
                                var p = 0
                                while (p < oldLen && p < insertCount && oldText[p] == newText[p]) p++
                                var q = 0
                                while (q < oldLen - p && q < insertCount - p &&
                                    oldText[oldLen - 1 - q] == newText[insertCount - 1 - q]
                                ) q++
                                val typing = pendingTypingFormats.toSet()
                                val perChar = List(insertCount) { i ->
                                    when {
                                        i < p -> oldFormats[i]
                                        i >= insertCount - q -> oldFormats[oldLen - (insertCount - i)]
                                        else -> typing
                                    }
                                }
                                for (type in FormatType.entries) {
                                    removeStyle(editable, from, to, type)
                                    var runStart = -1
                                    for (i in 0..insertCount) {
                                        val on = i < insertCount && type in perChar[i]
                                        if (on && runStart < 0) {
                                            runStart = i
                                        } else if (!on && runStart >= 0) {
                                            applyStyle(editable, from + runStart, from + i, type)
                                            runStart = -1
                                        }
                                    }
                                }
                            }
                        } finally {
                            isInserting = false
                        }
                        val caret = selectionStart
                        activeFormatsListener?.invoke(activeFormatsAt(editable, caret, caret, pendingTypingFormats))

                        onContentChangedState.value(editable.toString(), extractFormatting(editable))

                        pendingHighlight[0]?.let { handler.removeCallbacks(it) }
                        val runnable = Runnable {
                            applyHighlighting(
                                this@apply,
                                parser,
                                currentAmberArgb[0],
                                currentInkArgb[0],
                                currentMutedArgb[0]
                            )
                        }
                        pendingHighlight[0] = runnable
                        handler.postDelayed(runnable, 400)
                    }
                })
            }
        },
        update = { view ->
            val colorsChanged = currentAmberArgb[0] != amberArgb || currentInkArgb[0] != inkArgb ||
                currentMutedArgb[0] != mutedArgb
            currentAmberArgb[0] = amberArgb
            currentInkArgb[0] = inkArgb
            currentMutedArgb[0] = mutedArgb

            if (colorsChanged) {
                view.setTextColor(inkArgb)
                view.setHintTextColor(hintArgb)
            }

            val edit = pendingEdit
            if (edit != null && edit.token != lastAppliedToken.longValue) {
                val editable = view.text
                val fits = editable != null &&
                    edit.start >= 0 && edit.end <= editable.length && edit.start <= edit.end
                if (fits) {
                    view.isProgrammatic = true
                    // replace, а не пересборка Spannable: правка попадает в стек отмены
                    // и не сбрасывает позицию курсора.
                    editable.replace(edit.start, edit.end, edit.text)
                    edit.formatting?.let { runs ->
                        val insertedEnd = edit.start + edit.text.length
                        // Вставка вплотную к концу стилизованной строки растягивает её спаны
                        // (SPAN_EXCLUSIVE_INCLUSIVE) — снимаем их, чтобы остались только свои.
                        for (type in FormatType.entries) removeStyle(editable, edit.start, insertedEnd, type)
                        for (run in NoteFormattingCodec.clampTo(runs, edit.text.length)) {
                            applyRun(editable, run, offset = edit.start)
                        }
                    }
                    view.isProgrammatic = false

                    applyHighlighting(view, parser, amberArgb, inkArgb, mutedArgb)
                    onContentChangedState.value(editable.toString(), extractFormatting(editable))
                }
                lastAppliedToken.longValue = edit.token
                // Сообщаем и об отказе: вызывающий заранее сдвинул свой диапазон в расчёте
                // на успех, и без этого сигнала он остался бы рассинхронизирован с текстом
                // навсегда, молча промахиваясь мимо ссылки на каждом следующем тапе.
                onPendingEditApplied(edit.token, fits)
                return@AndroidView
            }

            val command = formatCommand
            if (command != null && command.token != lastAppliedFormatToken.longValue) {
                val editable = view.text
                if (editable != null) {
                    val hadSelection = view.selectionStart != view.selectionEnd
                    val newActiveFormats = applyFormatCommand(
                        editable,
                        view.selectionStart,
                        view.selectionEnd,
                        view.pendingTypingFormats,
                        command.type
                    )
                    // Реальное выделение меняет спаны Editable напрямую — TextWatcher на это
                    // не реагирует (span-изменения не запускают afterTextChanged), поэтому
                    // текст/диапазоны наружу нужно отдать здесь же, иначе форматирование не
                    // переживёт следующее сохранение. Каретка без выделения текст не меняет —
                    // там достаточно свежей подсветки кнопок.
                    if (hadSelection) {
                        onContentChangedState.value(editable.toString(), extractFormatting(editable))
                    }
                    onActiveFormatsChangedState.value(newActiveFormats)
                }
                lastAppliedFormatToken.longValue = command.token
                onFormatCommandApplied(command.token)
                return@AndroidView
            }

            if (view.text?.toString() != text) {
                view.isProgrammatic = true
                try {
                    view.setText(text)
                    val len = view.text?.length ?: 0
                    val target = if (initialCursorPosition >= 0) {
                        initialCursorPosition.coerceIn(0, len)
                    } else {
                        len
                    }
                    view.setSelection(target)
                    applyFormatting(view, formatting)
                    lastAppliedFormatting[0] = formatting
                    applyHighlighting(view, parser, amberArgb, inkArgb, mutedArgb)
                    // setSelection выше не долетает до onSelectionChanged — оно подавлено
                    // isProgrammatic, поэтому pendingTypingFormats иначе остался бы пустым до
                    // первого реального события смены выделения. Без этой синхронизации первое
                    // нажатие кнопки тулбара сразу после открытия заметки переключало бы стиль
                    // от пустого набора, а не от того, что действительно на каретке.
                    view.text?.let { editable ->
                        view.pendingTypingFormats.syncFromContext(editable, target)
                        onActiveFormatsChangedState.value(
                            activeFormatsAt(editable, target, target, view.pendingTypingFormats)
                        )
                    }
                } finally {
                    view.isProgrammatic = false
                }
                handler.post { if (view.isAttachedToWindow) view.requestFocus() }
            } else if (formatting != lastAppliedFormatting[0]) {
                applyFormatting(view, formatting)
                lastAppliedFormatting[0] = formatting
                // Цвет цитаты стиха (QUOTE) рисует подсветка — её нужно пересчитать.
                applyHighlighting(view, parser, amberArgb, inkArgb, mutedArgb)
            } else if (colorsChanged) {
                applyHighlighting(view, parser, amberArgb, inkArgb, mutedArgb)
            }
        },
        modifier = modifier
    )
}

private fun applyHighlighting(
    editText: EditText,
    parser: BibleReferenceParser,
    amberArgb: Int,
    inkArgb: Int,
    mutedArgb: Int
) {
    val spannable = editText.text as? Spannable ?: return
    val len = spannable.length
    val selStart = editText.selectionStart.coerceIn(0, len)
    val selEnd = editText.selectionEnd.coerceIn(0, len)

    for (span in spannable.getSpans(0, len, ForegroundColorSpan::class.java)) spannable.removeSpan(span)
    for (span in spannable.getSpans(0, len, BibleClickSpan::class.java)) spannable.removeSpan(span)

    spannable.setSpan(ForegroundColorSpan(inkArgb), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    // Цитата стиха — поверх чернил, но под янтарём ссылок: спаны рисуются в порядке добавления.
    for (quote in spannable.getSpans(0, len, VerseQuoteSpan::class.java)) {
        spannable.setSpan(
            ForegroundColorSpan(mutedArgb),
            spannable.getSpanStart(quote),
            spannable.getSpanEnd(quote),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    for (ref in parser.parse(spannable.toString())) {
        if (ref.startIndex < 0 || ref.endIndex > len) continue
        spannable.setSpan(
            ForegroundColorSpan(amberArgb),
            ref.startIndex,
            ref.endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            BibleClickSpan(ref),
            ref.startIndex,
            ref.endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    editText.setSelection(selStart, selEnd)
}

@SuppressLint("AppCompatCustomView")
private class CursorTrackingEditText(context: Context) : EditText(context) {
    var isProgrammatic = false

    /** true между onTextChanged вставки и концом afterTextChanged, см. TextWatcher. */
    var isInserting = false
    var selectionListener: ((Int) -> Unit)? = null
    var activeFormatsListener: ((Set<FormatType>) -> Unit)? = null

    /** «Режим ввода»: стили, которые получит следующий введённый символ, когда выделения нет.
     * Чисто рантайм-состояние поля ввода, как isProgrammatic — не сохраняется и не сериализуется. */
    val pendingTypingFormats = mutableSetOf<FormatType>()

    // EditText's Java constructor synchronously calls setText(), which invokes this overridden
    // onSelectionChanged BEFORE Kotlin runs this subclass's own property initializers above —
    // pendingTypingFormats is still null at that point, crashing with an NPE. `constructed`
    // exploits the same trick that already made isProgrammatic/selectionListener safe by
    // accident: the JVM zero-initializes a field to its declared-false default before any
    // initializer runs, so during that one early call `constructed` reads false even though
    // its own initializer says `true`, letting us bail out before touching pendingTypingFormats.
    private val constructed = true

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!constructed) return
        if (isProgrammatic) return
        selectionListener?.invoke(selEnd)

        val editable = text ?: return
        // При наборе символа режим ввода — выбор пользователя, а не «эхо» окружения.
        if (selStart == selEnd && !isInserting) pendingTypingFormats.syncFromContext(editable, selStart)
        activeFormatsListener?.invoke(activeFormatsAt(editable, selStart, selEnd, pendingTypingFormats))
    }
}

/**
 * Ссылка на стих под точкой касания (координаты — из MotionEvent, относительно view) или null.
 * Проверяет попадание именно в глифы ссылки, а не просто в ближайшее смещение строки: иначе тап
 * справа от ссылки в конце строки тоже считался бы тапом по ней.
 */
private fun EditText.bibleSpanAt(eventX: Float, eventY: Float): BibleClickSpan? {
    val layout = layout ?: return null
    val x = eventX - totalPaddingLeft + scrollX
    val y = (eventY - totalPaddingTop).toInt() + scrollY
    val line = layout.getLineForVertical(y)
    if (y < layout.getLineTop(line) || y > layout.getLineBottom(line)) return null

    val offset = layout.getOffsetForHorizontal(line, x)
    val spannable = text
    val span = spannable.getSpans(offset, offset, BibleClickSpan::class.java).firstOrNull() ?: return null

    val spanStart = spannable.getSpanStart(span)
    val spanEnd = spannable.getSpanEnd(span)
    val spanStartLine = layout.getLineForOffset(spanStart)
    val spanEndLine = layout.getLineForOffset((spanEnd - 1).coerceAtLeast(spanStart))
    val spanStartX = layout.getPrimaryHorizontal(spanStart)
    val spanEndX = layout.getPrimaryHorizontal(spanEnd)

    val hit = when (line) {
        spanStartLine if line == spanEndLine -> x in spanStartX..spanEndX
        spanStartLine -> x >= spanStartX
        spanEndLine -> x <= spanEndX
        in (spanStartLine + 1) until spanEndLine -> true
        else -> false
    }
    return if (hit) span else null
}

internal class BibleClickSpan(val reference: BibleReference) : ClickableSpan() {
    override fun onClick(widget: View) {}
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}
