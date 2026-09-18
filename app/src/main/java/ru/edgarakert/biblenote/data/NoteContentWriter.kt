package ru.edgarakert.biblenote.data

import ru.edgarakert.biblenote.data.db.Note
import ru.edgarakert.biblenote.data.db.NoteFormattingCodec

/**
 * Единственное место, где текст попадает в заметку извне редактора.
 * Чистые функции: возвращают новую Note, ничего не сохраняют — сохранение делает вызывающий.
 */
object NoteContentWriter {

    /** Дописывает сниппет в конец. Пустой сниппет — no-op, заметка возвращается как есть. */
    fun append(snippet: String, note: Note, now: Long = System.currentTimeMillis()): Note {
        if (snippet.isBlank()) return note

        // isBlank, а не isEmpty, симметрично проверке сниппета выше: заметка из одних
        // пробелов — это пустая заметка, и приписывать к ней разделитель значит начать
        // содержимое с пустой строки. trimEnd заодно не даёт накопить хвостовые переводы
        // строк при повторных дописываниях.
        val base = note.content.trimEnd()
        val separator = if (base.isBlank()) "" else "\n\n"
        // trimEnd мог укоротить текст — диапазон форматирования, доходивший до самого конца
        // (например, жирным было выделено всё до хвостового пробела), иначе остался бы
        // ссылаться на позиции внутри дописанного разделителя/сниппета, а не на исходный текст.
        val formatting = note.formatting?.let {
            NoteFormattingCodec.encode(NoteFormattingCodec.clampTo(NoteFormattingCodec.decode(it), base.length))
        }
        return note.copy(
            content = if (base.isBlank()) snippet else base + separator + snippet,
            formatting = formatting,
            updatedAt = now
        )
    }

    /** Новая заметка со сниппетом в теле. Заголовок вызывающий формирует сам (обычно это ссылка). */
    fun makeNote(
        snippet: String,
        title: String,
        folderId: Long? = null,
        now: Long = System.currentTimeMillis(),
    ): Note = Note(
        title = title,
        content = snippet,
        folderId = folderId,
        createdAt = now,
        updatedAt = now
    )
}
