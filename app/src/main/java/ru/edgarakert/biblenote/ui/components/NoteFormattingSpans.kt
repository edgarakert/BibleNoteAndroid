// Span-manipulation helpers for note rich-text formatting — split out from BibleEditText.kt to
// keep Bible-reference highlighting and formatting concerns visually separate, see
// docs/superpowers/plans/2026-09-05-phase16-rich-text.md task 16.4.
package ru.edgarakert.biblenote.ui.components

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.EditText
import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec

/** Команда тулбара: переключить стиль. token отсекает повторное применение — тот же приём, что у PendingEdit. */
data class FormatCommand(val token: Long, val type: FormatType)

/** Коэффициент увеличенного размера текста — бинарный переключатель, не цикл (см. «Уточнения поведения»). */
internal const val LARGE_TEXT_SCALE = 1.3f

/** Накладывает диапазоны форматирования на живой Editable. Вызывается при загрузке текста, до applyHighlighting. */
internal fun applyFormatting(editText: EditText, runs: List<FormatRun>) {
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
internal fun extractFormatting(editable: Editable): List<FormatRun> {
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
 * тулбара (задача 16.4, applyFormatCommand) — не-приватный, чтобы тулбар мог его переиспользовать
 * без дублирования.
 */
internal fun applyStyle(editable: Editable, from: Int, to: Int, type: FormatType) {
    val span: Any = when (type) {
        FormatType.BOLD -> StyleSpan(Typeface.BOLD)
        FormatType.ITALIC -> StyleSpan(Typeface.ITALIC)
        FormatType.SIZE -> RelativeSizeSpan(LARGE_TEXT_SCALE)
    }
    // SPAN_EXCLUSIVE_INCLUSIVE: следующий введённый символ вплотную к концу диапазона
    // тоже подхватывает стиль без повторного вызова.
    editable.setSpan(span, from, to, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
}

/**
 * Обрабатывает нажатие кнопки тулбара для стиля [type].
 *
 * Есть выделение ([selectionStart] != [selectionEnd]): переключает стиль на всём диапазоне —
 * снимает, если он уже покрывает выделение целиком ([hasStyle]), иначе применяет ([applyStyle]).
 *
 * Каретка без выделения: сам [editable] не трогается — переключается членство [type] в
 * [pendingTypingFormats] («режим ввода»), предварительно синхронизированном с контекстом каретки
 * ([syncFromContext]), чтобы не переключить стиль, «протёкший» из предыдущей позиции курсора.
 *
 * Возвращает свежий набор активных стилей для немедленного обновления подсветки кнопок тулбара
 * (onActiveFormatsChanged), не дожидаясь следующего события смены выделения.
 */
internal fun applyFormatCommand(
    editable: Editable,
    selectionStart: Int,
    selectionEnd: Int,
    pendingTypingFormats: MutableSet<FormatType>,
    type: FormatType,
): Set<FormatType> {
    val start = selectionStart.coerceAtLeast(0)
    val end = selectionEnd.coerceAtLeast(0)
    val from = minOf(start, end)
    val to = maxOf(start, end)

    if (from != to) {
        // Есть выделение: переключаем стиль на всём диапазоне.
        if (hasStyle(editable, from, to, type)) {
            removeStyle(editable, from, to, type)
        } else {
            applyStyle(editable, from, to, type)
        }
    } else {
        // Каретка без выделения: переключаем режим ввода для последующих символов.
        pendingTypingFormats.syncFromContext(editable, from)
        if (type in pendingTypingFormats) {
            pendingTypingFormats -= type
        } else {
            pendingTypingFormats += type
        }
    }

    return activeFormatsAt(editable, start, end, pendingTypingFormats)
}

/**
 * «Активен» значит спаны типа [type] покрывают ВЕСЬ диапазон [from, to) без разрывов —
 * иначе неоднозначно, что показывать на кнопке и что переключит следующее нажатие.
 */
internal fun hasStyle(editable: Editable, from: Int, to: Int, type: FormatType): Boolean {
    if (from >= to) return false
    return when (type) {
        FormatType.BOLD -> rangeFullyCoveredByStyle(editable, from, to, Typeface.BOLD)
        FormatType.ITALIC -> rangeFullyCoveredByStyle(editable, from, to, Typeface.ITALIC)
        FormatType.SIZE -> rangeFullyCoveredBySize(editable, from, to)
    }
}

/**
 * Снимает стиль [type] с диапазона [from, to), обрабатывая частичное пересечение: спан,
 * выходящий за снимаемый диапазон, удаляется и заменяется одним или двумя обрезками — слева
 * от [from], если спан начинался раньше, и справа от [to], если заканчивался позже. Так снятие
 * жирного из середины длинного жирного предложения оставляет жирными куски по краям, а не
 * стирает форматирование за пределами переключаемого диапазона.
 */
internal fun removeStyle(editable: Editable, from: Int, to: Int, type: FormatType) {
    when (type) {
        FormatType.BOLD -> removePartial(
            editable,
            from,
            to,
            editable.getSpans(from, to, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
        ) { StyleSpan(Typeface.BOLD) }

        FormatType.ITALIC -> removePartial(
            editable,
            from,
            to,
            editable.getSpans(from, to, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
        ) { StyleSpan(Typeface.ITALIC) }

        FormatType.SIZE -> removePartial(
            editable,
            from,
            to,
            editable.getSpans(from, to, RelativeSizeSpan::class.java).toList()
        ) { RelativeSizeSpan(it.sizeChange) }
    }
}

/**
 * Разрезает каждый из [spans], реально пересекающийся с [from, to), на обрезки вне этого
 * диапазона. [copy] строит новый спан того же типа для сохраняемого обрезка (для SIZE — с тем
 * же коэффициентом, что был у исходного спана).
 */
private fun <T : Any> removePartial(editable: Editable, from: Int, to: Int, spans: List<T>, copy: (T) -> Any) {
    for (span in spans) {
        val spanStart = editable.getSpanStart(span)
        val spanEnd = editable.getSpanEnd(span)
        // getSpans может вернуть спан, лишь касающийся границы диапазона без настоящего
        // пересечения — такой не трогаем.
        if (spanStart >= to || spanEnd <= from) continue

        editable.removeSpan(span)
        if (spanStart < from) {
            editable.setSpan(copy(span), spanStart, from, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        if (spanEnd > to) {
            editable.setSpan(copy(span), to, spanEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        }
    }
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
    // Копия, а не сам pendingTypingFormats: это долгоживущее изменяемое поле поля ввода,
    // syncFromContext переиспользует и очищает его при каждой смене каретки — без копии
    // наблюдатель (например, StateFlow тулбара) получил бы ссылку, которая молча
    // мутирует позже, и мог бы не заметить изменение при сравнении по ссылке.
    if (selStart == selEnd) return pendingTypingFormats.toSet()

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
