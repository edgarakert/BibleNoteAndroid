package ru.edgarakert.biblenote.data

import ru.edgarakert.biblenote.data.db.Note

/**
 * Единственное место, где текст попадает в заметку извне редактора.
 * Чистые функции: возвращают новую Note, ничего не сохраняют — сохранение делает вызывающий.
 */
object NoteContentWriter {

    /** Дописывает сниппет в конец. Пустой сниппет — no-op, заметка возвращается как есть. */
    fun append(snippet: String, note: Note, now: Long = System.currentTimeMillis()): Note {
        if (snippet.isBlank()) return note

        val separator = if (note.content.isEmpty()) "" else "\n\n"
        return note.copy(
            content = note.content + separator + snippet,
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
