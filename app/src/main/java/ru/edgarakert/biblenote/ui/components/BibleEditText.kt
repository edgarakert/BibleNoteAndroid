package ru.edgarakert.biblenote.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.RichTextSerializer

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BibleEditText(
    text: String,
    contentHtml: String?,
    onContentChanged: (plain: String, html: String?) -> Unit,
    onReferenceTapped: (BibleReference) -> Unit,
    parser: BibleReferenceParser,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    initialCursorPosition: Int = -1,
    onCursorPositionChanged: (Int) -> Unit = {},
    boldTrigger: Int = 0,
    italicTrigger: Int = 0,
    largeTrigger: Int = 0,
    undoTrigger: Int = 0,
    redoTrigger: Int = 0,
    onFormattingChanged: (bold: Boolean, italic: Boolean, large: Boolean) -> Unit = { _, _, _ -> },
    onUndoStateChanged: (canUndo: Boolean, canRedo: Boolean) -> Unit = { _, _ -> },
) {
    val amberArgb = MaterialTheme.colorScheme.primary.toArgb()
    val inkArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintArgb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()

    val onContentChangedState = rememberUpdatedState(onContentChanged)
    val onReferenceTappedState = rememberUpdatedState(onReferenceTapped)
    val onCursorPositionChangedState = rememberUpdatedState(onCursorPositionChanged)
    val onFormattingChangedState = rememberUpdatedState(onFormattingChanged)
    val onUndoStateChangedState = rememberUpdatedState(onUndoStateChanged)

    val handler = remember { Handler(Looper.getMainLooper()) }
    val pendingHighlight = remember { arrayOfNulls<Runnable>(1) }
    val currentAmberArgb = remember { intArrayOf(amberArgb) }
    val currentInkArgb = remember { intArrayOf(inkArgb) }

    val prevBoldTrigger = remember { intArrayOf(boldTrigger) }
    val prevItalicTrigger = remember { intArrayOf(italicTrigger) }
    val prevLargeTrigger = remember { intArrayOf(largeTrigger) }
    val prevUndoTrigger = remember { intArrayOf(undoTrigger) }
    val prevRedoTrigger = remember { intArrayOf(redoTrigger) }

    val viewRef = remember { arrayOfNulls<CursorTrackingEditText>(1) }

    DisposableEffect(Unit) {
        onDispose { pendingHighlight[0]?.let { handler.removeCallbacks(it) } }
    }

    LaunchedEffect(boldTrigger) {
        if (boldTrigger != prevBoldTrigger[0]) {
            prevBoldTrigger[0] = boldTrigger
            viewRef[0]?.applyBold(onFormattingChangedState.value, onUndoStateChangedState.value, onContentChangedState.value)
        }
    }

    LaunchedEffect(italicTrigger) {
        if (italicTrigger != prevItalicTrigger[0]) {
            prevItalicTrigger[0] = italicTrigger
            viewRef[0]?.applyItalic(onFormattingChangedState.value, onUndoStateChangedState.value, onContentChangedState.value)
        }
    }

    LaunchedEffect(largeTrigger) {
        if (largeTrigger != prevLargeTrigger[0]) {
            prevLargeTrigger[0] = largeTrigger
            viewRef[0]?.applyLarge(onFormattingChangedState.value, onUndoStateChangedState.value, onContentChangedState.value)
        }
    }

    LaunchedEffect(undoTrigger) {
        if (undoTrigger != prevUndoTrigger[0]) {
            prevUndoTrigger[0] = undoTrigger
            viewRef[0]?.applyUndo(onUndoStateChangedState.value)
        }
    }

    LaunchedEffect(redoTrigger) {
        if (redoTrigger != prevRedoTrigger[0]) {
            prevRedoTrigger[0] = redoTrigger
            viewRef[0]?.applyRedo(onUndoStateChangedState.value)
        }
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

                viewRef[0] = this

                selectionListener = { pos ->
                    onCursorPositionChangedState.value(pos)
                    reportFormattingState(onFormattingChangedState.value)
                }

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
                        val spanEndLine =
                            layout.getLineForOffset((spanEnd - 1).coerceAtLeast(spanStart))
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
                    override fun afterTextChanged(s: Editable?) {
                        if (isProgrammatic) return

                        val newText = s?.toString() ?: ""
                        val spannable =
                            s as? SpannableStringBuilder ?: SpannableStringBuilder(newText)
                        val html = if (RichTextSerializer.hasFormatting(spannable)) {
                            RichTextSerializer.toJson(spannable)
                        } else null
                        onContentChangedState.value(newText, html)

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

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
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
        update = { view ->
            val colorsChanged = currentAmberArgb[0] != amberArgb || currentInkArgb[0] != inkArgb
            currentAmberArgb[0] = amberArgb
            currentInkArgb[0] = inkArgb
            viewRef[0] = view

            if (colorsChanged) {
                view.setTextColor(inkArgb)
                view.setHintTextColor(hintArgb)
            }

            val currentText = view.text?.toString() ?: ""
            if (currentText != text) {
                view.isProgrammatic = true
                try {
                    if (!contentHtml.isNullOrBlank()) {
                        val rich = RichTextSerializer.fromJson(contentHtml)
                        view.text.clear()
                        view.text.insert(0, rich)
                    } else {
                        view.setText(text)
                    }
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

    // Remove only color/click spans; preserve StyleSpan and RelativeSizeSpan
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

@SuppressLint("AppCompatCustomView")
private class CursorTrackingEditText(context: Context) : EditText(context) {
    var isProgrammatic = false
    var selectionListener: ((Int) -> Unit)? = null

    private val undoStack = ArrayDeque<SpannableStringBuilder>()
    private val redoStack = ArrayDeque<SpannableStringBuilder>()
    private val maxUndoLevels = 50

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!isProgrammatic) selectionListener?.invoke(selEnd)
    }

    private fun pushUndoState(onUndoState: (Boolean, Boolean) -> Unit) {
        val snapshot = SpannableStringBuilder(text)
        undoStack.addLast(snapshot)
        if (undoStack.size > maxUndoLevels) undoStack.removeFirst()
        redoStack.clear()
        onUndoState(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    fun applyUndo(onUndoState: (Boolean, Boolean) -> Unit) {
        if (undoStack.isEmpty()) return
        val snapshot = SpannableStringBuilder(text)
        redoStack.addLast(snapshot)
        val prev = undoStack.removeLast()
        // No isProgrammatic — let afterTextChanged fire to update ViewModel content+html
        text.clear()
        text.insert(0, prev)
        onUndoState(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    fun applyRedo(onUndoState: (Boolean, Boolean) -> Unit) {
        if (redoStack.isEmpty()) return
        val snapshot = SpannableStringBuilder(text)
        undoStack.addLast(snapshot)
        val next = redoStack.removeLast()
        // No isProgrammatic — let afterTextChanged fire to update ViewModel content+html
        text.clear()
        text.insert(0, next)
        onUndoState(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    fun applyBold(
        onFormattingChanged: (Boolean, Boolean, Boolean) -> Unit,
        onUndoState: (Boolean, Boolean) -> Unit,
        onContentChanged: (String, String?) -> Unit,
    ) {
        pushUndoState(onUndoState)
        applyStyleTrait(Typeface.BOLD)
        reportFormattingState(onFormattingChanged)
        notifyContentChanged(onContentChanged)
    }

    fun applyItalic(
        onFormattingChanged: (Boolean, Boolean, Boolean) -> Unit,
        onUndoState: (Boolean, Boolean) -> Unit,
        onContentChanged: (String, String?) -> Unit,
    ) {
        pushUndoState(onUndoState)
        applyStyleTrait(Typeface.ITALIC)
        reportFormattingState(onFormattingChanged)
        notifyContentChanged(onContentChanged)
    }

    fun applyLarge(
        onFormattingChanged: (Boolean, Boolean, Boolean) -> Unit,
        onUndoState: (Boolean, Boolean) -> Unit,
        onContentChanged: (String, String?) -> Unit,
    ) {
        pushUndoState(onUndoState)
        val s = selectionStart
        val e = selectionEnd
        val start: Int
        val end: Int
        if (s == e) {
            val word = getWordRangeAt(s); start = word.first; end = word.second
        } else {
            start = minOf(s, e); end = maxOf(s, e)
        }
        if (start >= end) {
            reportFormattingState(onFormattingChanged); return
        }

        val spannable = text
        val existing = spannable.getSpans(start, end, RelativeSizeSpan::class.java)
        val allLarge = existing.isNotEmpty() && existing.all {
            spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end
        }
        existing.forEach { spannable.removeSpan(it) }
        if (!allLarge) {
            spannable.setSpan(
                RelativeSizeSpan(1.5f),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        reportFormattingState(onFormattingChanged)
        notifyContentChanged(onContentChanged)
    }

    private fun notifyContentChanged(onContentChanged: (String, String?) -> Unit) {
        val plain = text.toString()
        val ssb = SpannableStringBuilder(text)
        val html = if (RichTextSerializer.hasFormatting(ssb)) RichTextSerializer.toJson(ssb) else null
        onContentChanged(plain, html)
    }

    private fun applyStyleTrait(trait: Int) {
        val s = selectionStart
        val e = selectionEnd
        val start: Int
        val end: Int
        if (s == e) {
            val word = getWordRangeAt(s); start = word.first; end = word.second
        } else {
            start = minOf(s, e); end = maxOf(s, e)
        }
        if (start >= end) return

        val spannable = text
        val existing = spannable.getSpans(start, end, StyleSpan::class.java)

        val allHave = existing.isNotEmpty() && existing.all { sp ->
            val ss = spannable.getSpanStart(sp)
            val se = spannable.getSpanEnd(sp)
            ss <= start && se >= end && (sp.style == trait || sp.style == Typeface.BOLD_ITALIC)
        }

        for (sp in existing) {
            val ss = spannable.getSpanStart(sp)
            val se = spannable.getSpanEnd(sp)
            if (ss < 0 || se < 0) continue
            val oldStyle = sp.style
            spannable.removeSpan(sp)
            if (ss < start) spannable.setSpan(
                StyleSpan(oldStyle),
                ss,
                start,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (se > end) spannable.setSpan(
                StyleSpan(oldStyle),
                end,
                se,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (!allHave) {
            val remaining = spannable.getSpans(start, end, StyleSpan::class.java)
            var mergedBold = trait == Typeface.BOLD
            var mergedItalic = trait == Typeface.ITALIC
            for (sp in remaining) {
                if (sp.style == Typeface.BOLD || sp.style == Typeface.BOLD_ITALIC) mergedBold = true
                if (sp.style == Typeface.ITALIC || sp.style == Typeface.BOLD_ITALIC) mergedItalic =
                    true
            }
            remaining.forEach { spannable.removeSpan(it) }
            val finalStyle = when {
                mergedBold && mergedItalic -> Typeface.BOLD_ITALIC
                mergedBold -> Typeface.BOLD
                mergedItalic -> Typeface.ITALIC
                else -> return
            }
            spannable.setSpan(StyleSpan(finalStyle), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun reportFormattingState(onFormattingChanged: (Boolean, Boolean, Boolean) -> Unit) {
        val s = selectionStart.coerceAtLeast(0)
        val e = selectionEnd.coerceAtLeast(0)
        val checkStart = minOf(s, e)
        val checkEnd = maxOf(s, e).coerceAtLeast(checkStart + 1).coerceAtMost(text.length)

        val styleSpans = if (checkStart < text.length) {
            text.getSpans(checkStart, checkEnd, StyleSpan::class.java)
        } else emptyArray()
        val isBold =
            styleSpans.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
        val isItalic =
            styleSpans.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }

        val sizeSpans = if (checkStart < text.length) {
            text.getSpans(checkStart, checkEnd, RelativeSizeSpan::class.java)
        } else emptyArray()
        val isLarge = sizeSpans.isNotEmpty()

        onFormattingChanged(isBold, isItalic, isLarge)
    }

    private fun getWordRangeAt(pos: Int): Pair<Int, Int> {
        val s = text.toString()
        val len = s.length
        if (len == 0) return 0 to 0
        var start = pos.coerceIn(0, len)
        var end = pos.coerceIn(0, len)
        while (start > 0 && !s[start - 1].isWhitespace()) start--
        while (end < len && !s[end].isWhitespace()) end++
        return start to end
    }
}

internal class BibleClickSpan(val reference: BibleReference) : ClickableSpan() {
    override fun onClick(widget: View) {}
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}
