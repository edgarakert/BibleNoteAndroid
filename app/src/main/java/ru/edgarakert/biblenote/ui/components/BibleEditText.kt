package ru.edgarakert.biblenote.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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

/** Одноразовая правка текста извне редактора. token отсекает повторное применение. */
data class PendingEdit(val token: Long, val start: Int, val end: Int, val text: String)

/** Команда тулбара: переключить стиль. token отсекает повторное применение — тот же приём, что у PendingEdit. */
data class FormatCommand(val token: Long, val type: FormatType)

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
    formatCommand: FormatCommand? = null,
    onFormatCommandApplied: (token: Long) -> Unit = {},
    pendingEdit: PendingEdit? = null,
    /** Вызывается ровно один раз на токен; applied=false, если границы не подошли к живому тексту. */
    onPendingEditApplied: (token: Long, applied: Boolean) -> Unit = { _, _ -> },
) {
    val amberArgb = MaterialTheme.colorScheme.primary.toArgb()
    val inkArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintArgb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()

    val onContentChangedState = rememberUpdatedState(onContentChanged)
    val onReferenceTappedState = rememberUpdatedState(onReferenceTapped)
    val onCursorPositionChangedState = rememberUpdatedState(onCursorPositionChanged)
    val onActiveFormatsChangedState = rememberUpdatedState(onActiveFormatsChanged)

    val handler = remember { Handler(Looper.getMainLooper()) }
    val pendingHighlight = remember { arrayOfNulls<Runnable>(1) }
    val currentAmberArgb = remember { intArrayOf(amberArgb) }
    val currentInkArgb = remember { intArrayOf(inkArgb) }
    // Токен последней применённой внешней правки — отсекает повторное применение
    // при рекомпозиции, пока вызывающая сторона ещё не успела сбросить pendingEdit в null.
    val lastAppliedToken = remember { mutableLongStateOf(-1L) }

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

                setOnTouchListener { view, event ->
                    if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

                    val editText = view as EditText
                    val layout = editText.layout ?: return@setOnTouchListener false

                    val x = event.x - editText.totalPaddingLeft
                    val y = (event.y - editText.totalPaddingTop).toInt()
                    val line = layout.getLineForVertical(y)

                    if (y < layout.getLineTop(line) || y > layout.getLineBottom(line)) return@setOnTouchListener false

                    val offset = layout.getOffsetForHorizontal(line, x)

                    val spans = editText.text.getSpans(offset, offset, BibleClickSpan::class.java)
                    if (spans.isNotEmpty()) {
                        val spannable = editText.text
                        val spanStart = spannable.getSpanStart(spans[0])
                        val spanEnd = spannable.getSpanEnd(spans[0])
                        val spanStartLine = layout.getLineForOffset(spanStart)
                        val spanEndLine = layout.getLineForOffset((spanEnd - 1).coerceAtLeast(spanStart))
                        val spanStartX = layout.getPrimaryHorizontal(spanStart)
                        val spanEndX = layout.getPrimaryHorizontal(spanEnd)

                        val hit = when (line) {
                            spanStartLine if line == spanEndLine ->
                                x in spanStartX..spanEndX

                            spanStartLine ->
                                x >= spanStartX

                            spanEndLine ->
                                x <= spanEndX

                            in (spanStartLine + 1) until spanEndLine ->
                                true

                            else -> false
                        }

                        if (hit) {
                            val span = spans[0]
                            val liveStart = spannable.getSpanStart(span)
                            val liveEnd = spannable.getSpanEnd(span)
                            if (liveStart < 0 || liveEnd > spannable.length) {
                                return@setOnTouchListener false
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
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

                addTextChangedListener(object : android.text.TextWatcher {
                    // Границы последней вставки: afterTextChanged применяет к ним
                    // стили из pendingTypingFormats («режим ввода» без выделения).
                    private var insertStart = -1
                    private var insertCount = 0

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        insertStart = start
                        insertCount = count
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (isProgrammatic) return
                        val editable = s ?: return

                        if (insertCount > 0 && pendingTypingFormats.isNotEmpty()) {
                            for (type in pendingTypingFormats) {
                                applyStyle(editable, insertStart, insertStart + insertCount, type)
                            }
                        }

                        onContentChangedState.value(editable.toString(), extractFormatting(editable))

                        pendingHighlight[0]?.let { handler.removeCallbacks(it) }
                        val runnable = Runnable {
                            applyHighlighting(
                                this@apply,
                                parser,
                                currentAmberArgb[0],
                                currentInkArgb[0]
                            )
                        }
                        pendingHighlight[0] = runnable
                        handler.postDelayed(runnable, 400)
                    }
                })
            }
        },
        update = { view ->
            val colorsChanged = currentAmberArgb[0] != amberArgb || currentInkArgb[0] != inkArgb
            currentAmberArgb[0] = amberArgb
            currentInkArgb[0] = inkArgb

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
                    editable!!.replace(edit.start, edit.end, edit.text)
                    view.isProgrammatic = false

                    applyHighlighting(view, parser, amberArgb, inkArgb)
                    onContentChangedState.value(editable.toString(), extractFormatting(editable))
                }
                lastAppliedToken.longValue = edit.token
                // Сообщаем и об отказе: вызывающий заранее сдвинул свой диапазон в расчёте
                // на успех, и без этого сигнала он остался бы рассинхронизирован с текстом
                // навсегда, молча промахиваясь мимо ссылки на каждом следующем тапе.
                onPendingEditApplied(edit.token, fits)
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
                    applyHighlighting(view, parser, amberArgb, inkArgb)
                } finally {
                    view.isProgrammatic = false
                }
                handler.post { if (view.isAttachedToWindow) view.requestFocus() }
            } else if (colorsChanged) {
                applyHighlighting(view, parser, amberArgb, inkArgb)
            }
        },
        modifier = modifier
    )
}

private fun applyHighlighting(
    editText: EditText,
    parser: BibleReferenceParser,
    amberArgb: Int,
    inkArgb: Int
) {
    val spannable = editText.text as? Spannable ?: return
    val len = spannable.length
    val selStart = editText.selectionStart.coerceIn(0, len)
    val selEnd = editText.selectionEnd.coerceIn(0, len)

    for (span in spannable.getSpans(0, len, ForegroundColorSpan::class.java)) spannable.removeSpan(span)
    for (span in spannable.getSpans(0, len, BibleClickSpan::class.java)) spannable.removeSpan(span)

    spannable.setSpan(ForegroundColorSpan(inkArgb), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

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

/** Накладывает диапазоны форматирования на живой Editable. Вызывается при загрузке текста, до applyHighlighting. */
private fun applyFormatting(editText: EditText, runs: List<FormatRun>) {
    val editable = editText.text ?: return

    // Снимаем только свои стилевые спаны, чтобы не задеть подсветку ссылок.
    editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
    editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { editable.removeSpan(it) }

    for (run in NoteFormattingCodec.clampTo(runs, editable.length)) {
        val span: Any = when (run.type) {
            FormatType.BOLD -> StyleSpan(Typeface.BOLD)
            FormatType.ITALIC -> StyleSpan(Typeface.ITALIC)
            FormatType.SIZE -> RelativeSizeSpan(run.scale)
        }
        // SPAN_EXCLUSIVE_INCLUSIVE: текст, дописанный вплотную к концу стилизованного участка,
        // продолжает нести стиль — так ведут себя привычные редакторы.
        editable.setSpan(span, run.start, run.end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
    }
}

/** Читает текущие диапазоны форматирования из живого Editable. Спаны система уже сдвинула сама. */
private fun extractFormatting(editable: Editable): List<FormatRun> {
    val runs = mutableListOf<FormatRun>()

    editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { span ->
        val type = when (span.style) {
            Typeface.BOLD -> FormatType.BOLD
            Typeface.ITALIC -> FormatType.ITALIC
            else -> return@forEach
        }
        runs += FormatRun(type, editable.getSpanStart(span), editable.getSpanEnd(span))
    }

    editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { span ->
        runs += FormatRun(
            FormatType.SIZE,
            editable.getSpanStart(span),
            editable.getSpanEnd(span),
            span.sizeChange
        )
    }

    return runs.filter { it.end > it.start }
}

/**
 * Применяет один стиль к диапазону [from, to) живого Editable.
 *
 * Общий хелпер для «режима ввода» (задача 16.3) и ручного переключения стилем по кнопке
 * тулбара (задача 16.4, toggleStyle/applyFormatCommand) — оставлен не-приватным, чтобы
 * тулбар мог его переиспользовать без дублирования.
 *
 * NOTE: коэффициент для SIZE здесь — временный плейсхолдер 1.3f. Задача 16.4 вводит
 * именованную константу LARGE_TEXT_SCALE рядом с логикой тулбара и может уточнить эту строку.
 */
internal fun applyStyle(editable: Editable, from: Int, to: Int, type: FormatType) {
    val span: Any = when (type) {
        FormatType.BOLD -> StyleSpan(Typeface.BOLD)
        FormatType.ITALIC -> StyleSpan(Typeface.ITALIC)
        FormatType.SIZE -> RelativeSizeSpan(1.3f)
    }
    // SPAN_EXCLUSIVE_INCLUSIVE: следующий введённый символ вплотную к концу диапазона
    // тоже подхватывает стиль без повторного вызова.
    editable.setSpan(span, from, to, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
}

/**
 * Пересчитывает «режим ввода» из контекста: стиль символа непосредственно перед [position]
 * (или после, если [position] — начало текста). Используется при схлопывании выделения
 * в каретку (тап, стрелки), чтобы режим ввода не «протекал» из прежней позиции курсора —
 * аналог normalizeTypingAttributes в iOS.
 */
internal fun MutableSet<FormatType>.syncFromContext(editable: Editable, position: Int) {
    clear()
    if (editable.isEmpty()) return

    // Символ перед кареткой — тот, что попадает в [checkAt, checkAt + 1). У начала текста
    // смотрим на первый символ вместо несуществующего "перед позицией 0".
    val checkAt = if (position == 0) 0 else position - 1

    editable.getSpans(checkAt, checkAt + 1, StyleSpan::class.java).forEach { span ->
        when (span.style) {
            Typeface.BOLD -> add(FormatType.BOLD)
            Typeface.ITALIC -> add(FormatType.ITALIC)
        }
    }
    if (editable.getSpans(checkAt, checkAt + 1, RelativeSizeSpan::class.java).isNotEmpty()) {
        add(FormatType.SIZE)
    }
}

/**
 * Стили, «активные» прямо сейчас — для подсветки кнопок тулбара амбером.
 *
 * Схлопнутое выделение (каретка): активность равна режиму ввода [pendingTypingFormats] как есть —
 * на пустом месте у символов стиля нет, важно только то, чем будет напечатан следующий символ.
 *
 * Реальное выделение: тип активен, только если спаны этого типа покрывают ВЕСЬ диапазон
 * [selStart, selEnd) без разрывов — иначе неоднозначно, что показывать на кнопке и что
 * переключит следующее нажатие.
 */
internal fun activeFormatsAt(
    editable: Editable,
    selStart: Int,
    selEnd: Int,
    pendingTypingFormats: Set<FormatType>
): Set<FormatType> {
    if (selStart == selEnd) return pendingTypingFormats

    val from = minOf(selStart, selEnd)
    val to = maxOf(selStart, selEnd)
    if (from >= to) return emptySet()

    val result = mutableSetOf<FormatType>()

    if (rangeFullyCoveredByStyle(editable, from, to, Typeface.BOLD)) result += FormatType.BOLD
    if (rangeFullyCoveredByStyle(editable, from, to, Typeface.ITALIC)) result += FormatType.ITALIC
    if (rangeFullyCoveredBySize(editable, from, to)) result += FormatType.SIZE

    return result
}

private fun rangeFullyCoveredByStyle(editable: Editable, from: Int, to: Int, style: Int): Boolean {
    val spans = editable.getSpans(from, to, StyleSpan::class.java).filter { it.style == style }
    return coversRangeFully(editable, spans, from, to)
}

private fun rangeFullyCoveredBySize(editable: Editable, from: Int, to: Int): Boolean {
    val spans = editable.getSpans(from, to, RelativeSizeSpan::class.java).toList()
    return coversRangeFully(editable, spans, from, to)
}

/**
 * true, если [from, to) целиком покрыт объединением интервалов [spans] — без разрывов.
 * Обрабатывает несколько смежных/перекрывающихся спанов одного типа, не только случай одного спана.
 */
private fun <T : Any> coversRangeFully(editable: Editable, spans: List<T>, from: Int, to: Int): Boolean {
    if (spans.isEmpty()) return false

    val intervals = spans
        .map { span ->
            val start = editable.getSpanStart(span).coerceAtLeast(from)
            val end = editable.getSpanEnd(span).coerceAtMost(to)
            start to end
        }
        .filter { it.second > it.first }
        .sortedBy { it.first }

    if (intervals.isEmpty()) return false

    var coveredUpTo = from
    for ((start, end) in intervals) {
        if (start > coveredUpTo) return false
        coveredUpTo = maxOf(coveredUpTo, end)
    }
    return coveredUpTo >= to
}

@SuppressLint("AppCompatCustomView")
private class CursorTrackingEditText(context: Context) : EditText(context) {
    var isProgrammatic = false
    var selectionListener: ((Int) -> Unit)? = null
    var activeFormatsListener: ((Set<FormatType>) -> Unit)? = null

    /** «Режим ввода»: стили, которые получит следующий введённый символ, когда выделения нет.
     * Чисто рантайм-состояние поля ввода, как isProgrammatic — не сохраняется и не сериализуется. */
    val pendingTypingFormats = mutableSetOf<FormatType>()

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (isProgrammatic) return
        selectionListener?.invoke(selEnd)

        val editable = text ?: return
        if (selStart == selEnd) pendingTypingFormats.syncFromContext(editable, selStart)
        activeFormatsListener?.invoke(activeFormatsAt(editable, selStart, selEnd, pendingTypingFormats))
    }
}

internal class BibleClickSpan(val reference: BibleReference) : ClickableSpan() {
    override fun onClick(widget: View) {}
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}
