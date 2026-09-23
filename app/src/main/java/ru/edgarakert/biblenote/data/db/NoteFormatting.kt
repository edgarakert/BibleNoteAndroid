package ru.edgarakert.biblenote.data.db

/**
 * BOLD/ITALIC/SIZE переключаются кнопками тулбара. QUOTE (приглушённый цвет текста стиха) и
 * CAPTION (мелкая подпись-ссылка под ним) и VERSE_NUMBER (мелкий янтарный номер стиха в цитате)
 * ставит только вставка стиха из шторки — кнопок у них нет.
 */
enum class FormatType { BOLD, ITALIC, SIZE, QUOTE, CAPTION, VERSE_NUMBER }

/** Диапазон форматирования в тексте заметки. end не входит в диапазон. */
data class FormatRun(
    val type: FormatType,
    val start: Int,
    val end: Int,
    val scale: Float = 1f,
)

/**
 * Сериализация форматирования в одну строку: "b:0-10;i:12-20;s1.25:22-30;q:40-90;n:40-42;c:91-102".
 *
 * Свой формат вместо HTML: штатный HtmlCompat.fromHtml не читает font-size обратно,
 * поэтому размер шрифта не пережил бы круг «сохранили → открыли».
 */
object NoteFormattingCodec {

    fun encode(runs: List<FormatRun>): String = runs
        .filter { it.end > it.start }
        .joinToString(";") { run ->
            val head = when (run.type) {
                FormatType.BOLD -> "b"
                FormatType.ITALIC -> "i"
                FormatType.SIZE -> "s${run.scale}"
                FormatType.QUOTE -> "q"
                FormatType.CAPTION -> "c"
                FormatType.VERSE_NUMBER -> "n"
            }
            "$head:${run.start}-${run.end}"
        }

    /** Битые диапазоны пропускаются: испорченная строка не должна ронять открытие заметки. */
    fun decode(encoded: String?): List<FormatRun> {
        if (encoded.isNullOrEmpty()) return emptyList()

        return encoded.split(';').mapNotNull { segment ->
            val parts = segment.split(':')
            if (parts.size != 2) return@mapNotNull null

            val bounds = parts[1].split('-')
            if (bounds.size != 2) return@mapNotNull null
            val start = bounds[0].toIntOrNull() ?: return@mapNotNull null
            val end = bounds[1].toIntOrNull() ?: return@mapNotNull null
            if (end <= start || start < 0) return@mapNotNull null

            val head = parts[0]
            when {
                head == "b" -> FormatRun(FormatType.BOLD, start, end)
                head == "i" -> FormatRun(FormatType.ITALIC, start, end)
                head == "q" -> FormatRun(FormatType.QUOTE, start, end)
                head == "c" -> FormatRun(FormatType.CAPTION, start, end)
                head == "n" -> FormatRun(FormatType.VERSE_NUMBER, start, end)
                head.startsWith("s") -> {
                    val scale = head.drop(1).toFloatOrNull() ?: return@mapNotNull null
                    FormatRun(FormatType.SIZE, start, end, scale)
                }
                else -> null
            }
        }
    }

    /** Отбрасывает диапазоны, вышедшие за пределы текста, и подрезает частично вышедшие. */
    fun clampTo(runs: List<FormatRun>, textLength: Int): List<FormatRun> = runs
        .mapNotNull { run ->
            if (run.start >= textLength) return@mapNotNull null
            val end = minOf(run.end, textLength)
            if (end <= run.start) null else run.copy(end = end)
        }
}
