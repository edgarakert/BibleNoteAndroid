package ru.edgarakert.biblenote.data

import ru.edgarakert.biblenote.data.db.FormatRun
import ru.edgarakert.biblenote.data.db.FormatType

/**
 * Как вставлять текст, который приходит в заметку из шторки ссылки.
 *
 * Вынесено из экрана редактора отдельной чистой функцией по замечанию ревью: арифметика с
 * границами строк слишком легко ошибается на краях, чтобы жить внутри Compose-лямбды без тестов.
 */
object NoteTextInsertion {

    /**
     * Правка «заменить [start, end) на [text]» с цитатой на отдельной строке.
     * [textOffset] — где внутри [text] начинается исходная вставка (после добавленного переноса).
     */
    data class Replacement(val start: Int, val end: Int, val text: String, val textOffset: Int)

    /**
     * Заменяет ссылку [start, end) на [block] так, чтобы блок стоял на своей строке: перед ним
     * перенос, если ссылка была не в начале строки, после — если за ней на той же строке есть
     * текст или это конец заметки (чтобы свой текст пользователь продолжал с чистой строки,
     * а не вплотную к цитате). Пробелы вокруг ссылки съедаются — иначе они повисли бы в конце
     * предыдущей строки и в начале следующей. Границы приводятся к тексту.
     */
    fun replaceOnOwnLine(content: String, start: Int, end: Int, block: String): Replacement {
        var from = start.coerceIn(0, content.length)
        var to = end.coerceIn(from, content.length)
        while (from > 0 && content[from - 1].isInlineSpace()) from--
        while (to < content.length && content[to].isInlineSpace()) to++

        val prefix = if (from > 0 && content[from - 1] != '\n') "\n" else ""
        val suffix = if (to < content.length && content[to] == '\n') "" else "\n"
        return Replacement(from, to, prefix + block + suffix, prefix.length)
    }

    /**
     * Обратное вставке: если ссылка [refStart, refEnd) — подпись (CAPTION) под вставленной
     * цитатой стиха (QUOTE на строке прямо над ней), возвращает правку, которая убирает цитату
     * и оставляет от подписи голую ссылку — её текст ставится без стилей. Иначе null: ссылка
     * обычная, убирать нечего.
     */
    fun removeQuoteAbove(
        content: String,
        runs: List<FormatRun>,
        refStart: Int,
        refEnd: Int,
    ): Replacement? {
        if (refStart < 0 || refEnd > content.length || refStart >= refEnd) return null
        val caption = runs.firstOrNull {
            it.type == FormatType.CAPTION && it.start <= refStart && it.end >= refEnd
        } ?: return null
        val lineBreak = caption.start - 1
        if (lineBreak < 0 || content[lineBreak] != '\n') return null

        // Набор внутри цитаты может разрезать её на несколько смежных диапазонов — идём от
        // диапазона, кончающегося прямо над подписью, вверх, пока они стыкуются.
        val quotes = runs.filter { it.type == FormatType.QUOTE }
        var quoteStart = quotes.firstOrNull { it.end == lineBreak }?.start ?: return null
        while (true) {
            val above = quotes.firstOrNull { it.end >= quoteStart && it.start < quoteStart } ?: break
            quoteStart = above.start
        }

        val end = maxOf(caption.end, refEnd).coerceAtMost(content.length)
        return Replacement(quoteStart, end, content.substring(caption.start, end), textOffset = 0)
    }

    private fun Char.isInlineSpace() = this == ' ' || this == '\t'
}
