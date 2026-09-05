package ru.edgarakert.biblenote.data.settings

/** Один шаг пути навигации во вкладке «Заметки». */
sealed interface NotesPathEntry {
    val id: Long

    data class Folder(override val id: Long) : NotesPathEntry
    data class Note(override val id: Long) : NotesPathEntry
}

/**
 * Кодирует путь навигации в одну строку для DataStore: "f:12,n:34".
 * Собственный формат вместо JSON — в проекте нет kotlinx.serialization,
 * а формат тривиален и полностью покрыт тестами.
 */
object NotesPathCodec {

    fun encode(path: List<NotesPathEntry>): String = path.joinToString(",") { entry ->
        when (entry) {
            is NotesPathEntry.Folder -> "f:${entry.id}"
            is NotesPathEntry.Note -> "n:${entry.id}"
        }
    }

    /** Битые сегменты пропускаются: испорченная настройка не должна ронять запуск. */
    fun decode(encoded: String): List<NotesPathEntry> =
        encoded.split(',')
            .mapNotNull { segment ->
                val parts = segment.split(':')
                if (parts.size != 2) return@mapNotNull null
                val id = parts[1].toLongOrNull() ?: return@mapNotNull null
                when (parts[0]) {
                    "f" -> NotesPathEntry.Folder(id)
                    "n" -> NotesPathEntry.Note(id)
                    else -> null
                }
            }
}
