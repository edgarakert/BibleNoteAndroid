package ru.edgarakert.biblenote.ui.components

import android.annotation.SuppressLint
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.WarmGray

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BibleEditText(
    text: String,
    onTextChanged: (String) -> Unit,
    onReferenceTapped: (BibleReference) -> Unit,
    parser: BibleReferenceParser,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val amberArgb = Amber.toArgb()
    val inkArgb = Ink.toArgb()
    val hintArgb = WarmGray.copy(alpha = 0.6f).toArgb()

    val onTextChangedState = rememberUpdatedState(onTextChanged)
    val onReferenceTappedState = rememberUpdatedState(onReferenceTapped)

    // flag to suppress TextWatcher during programmatic setText
    val isProgrammatic = remember { BooleanArray(1) { false } }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val pendingHighlight = remember { arrayOfNulls<Runnable>(1) }

    DisposableEffect(Unit) {
        onDispose { pendingHighlight[0]?.let { handler.removeCallbacks(it) } }
    }

    AndroidView(
        factory = { context ->
            EditText(context).apply {
                background = null
                setTextColor(inkArgb)
                textSize = 17f
                typeface = Typeface.SERIF
                hint = placeholder
                setHintTextColor(hintArgb)
                gravity = Gravity.TOP or Gravity.START

                val density = context.resources.displayMetrics.density
                val padH = (16 * density).toInt()
                val padV = (12 * density).toInt()
                setPadding(padH, padV, padH, padV)

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
                        val spanStartX = layout.getPrimaryHorizontal(spanStart)
                        val spanEndX = layout.getPrimaryHorizontal(spanEnd)

                        val spanLine = layout.getLineForOffset(spanStart)
                        if (line == spanLine && x >= spanStartX && x <= spanEndX) {
                            onReferenceTappedState.value(spans[0].reference)
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
                        if (isProgrammatic[0]) return

                        val newText = s?.toString() ?: ""
                        onTextChangedState.value(newText)
                        pendingHighlight[0]?.let { handler.removeCallbacks(it) }
                        val runnable = Runnable {
                            applyHighlighting(this@apply, parser, amberArgb, inkArgb)
                        }
                        pendingHighlight[0] = runnable
                        handler.postDelayed(runnable, 400)
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }
                })
            }
        },
        update = { editText ->
            if (editText.text.toString() != text) {
                isProgrammatic[0] = true
                try {
                    editText.setText(text)
                    editText.setSelection(editText.text.length)
                    applyHighlighting(editText, parser, amberArgb, inkArgb)
                } finally {
                    isProgrammatic[0] = false
                }
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

    for (span in spannable.getSpans(0, len, ForegroundColorSpan::class.java)) spannable.removeSpan(
        span
    )
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

internal class BibleClickSpan(val reference: BibleReference) : ClickableSpan() {
    override fun onClick(widget: View) {}
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}
