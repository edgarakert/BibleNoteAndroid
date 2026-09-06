package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.NoteContentWriter
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.bible.VerseSnippetBuilder
import ru.edgarakert.biblenote.ui.components.NoteDestination

/**
 * Сохраняет отредактированный пользователем сниппет стихов в существующую или новую заметку.
 * bookName/chapter/verseNumbers нужны только для заголовка новой заметки — сам текст
 * (сниппет) в шторке уже готов и мог быть урезан пользователем.
 */
class SaveVersesToNoteViewModel(
    private val bookName: String,
    private val chapter: Int,
    private val verseNumbers: List<Int>,
    private val repository: NoteRepository
) : ViewModel() {

    fun save(
        text: String,
        destination: NoteDestination,
        onSaved: (noteId: Long, noteTitle: String) -> Unit
    ) {
        val body = text.trim()
        if (body.isEmpty()) return

        viewModelScope.launch {
            val noteId = destination.noteId
            val (title, id) = if (noteId != null) {
                val existing = repository.getNoteById(noteId) ?: return@launch
                val updated = NoteContentWriter.append(body, existing)
                repository.saveNote(updated)
                updated.title to noteId
            } else {
                // Заголовок новой заметки — сама ссылка ("Иоанна 3:16-17").
                val title = VerseSnippetBuilder.reference(bookName, chapter, verseNumbers)
                val note = NoteContentWriter.makeNote(body, title, destination.folderId)
                val id = repository.saveNote(note)
                title to id
            }
            onSaved(id, title)
        }
    }
}
