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

/** Одноразовая правка текста извне редактора. token отсекает повторное применение. */
data class PendingEdit(val token: Long, val start: Int, val end: Int, val text: String)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BibleEditText(
    text: String,
    onTextChanged: (String) -> Unit,
    onReferenceTapped: (BibleReference) -> Unit,
    parser: BibleReferenceParser,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    initialCursorPosition: Int = -1,
    onCursorPositionChanged: (Int) -> Unit = {},
    pendingEdit: PendingEdit? = null,
    onPendingEditApplied: (Long) -> Unit = {},
) {
    val amberArgb = MaterialTheme.colorScheme.primary.toArgb()
    val inkArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintArgb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()

    val onTextChangedState = rememberUpdatedState(onTextChanged)
    val onReferenceTappedState = rememberUpdatedState(onReferenceTapped)
    val onCursorPositionChangedState = rememberUpdatedState(onCursorPositionChanged)

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
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (isProgrammatic) return

                        val newText = s?.toString() ?: ""
                        onTextChangedState.value(newText)
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

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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
                if (editable != null && edit.start >= 0 && edit.end <= editable.length && edit.start <= edit.end) {
                    view.isProgrammatic = true
                    // replace, а не пересборка Spannable: правка попадает в стек отмены
                    // и не сбрасывает позицию курсора.
                    editable.replace(edit.start, edit.end, edit.text)
                    view.isProgrammatic = false

                    applyHighlighting(view, parser, amberArgb, inkArgb)
                    onTextChangedState.value(editable.toString())
                }
                lastAppliedToken.longValue = edit.token
                onPendingEditApplied(edit.token)
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

@SuppressLint("AppCompatCustomView")
private class CursorTrackingEditText(context: Context) : EditText(context) {
    var isProgrammatic = false
    var selectionListener: ((Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!isProgrammatic) selectionListener?.invoke(selEnd)
    }
}

internal class BibleClickSpan(val reference: BibleReference) : ClickableSpan() {
    override fun onClick(widget: View) {}
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}
