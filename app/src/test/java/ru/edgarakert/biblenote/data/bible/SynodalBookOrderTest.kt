package ru.edgarakert.biblenote.data.bible

import org.junit.Assert.assertEquals
import org.junit.Test

class SynodalBookOrderTest {

    private val allBooks: List<Book> = (1..66).map { id ->
        Book(id = id, nameRu = "Книга $id", nameEn = "Book $id", abbreviation = "B$id")
    }

    @Test
    fun `synodal puts the general epistles right after Acts`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        val newTestament = ordered.filter { it.id >= 40 }.map { it.id }

        assertEquals(
            listOf(
                40, 41, 42, 43, 44,
                59, 60, 61, 62, 63, 64, 65,
                45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                58,
                66
            ),
            newTestament
        )
    }

    @Test
    fun `synodal leaves the old testament in id order`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        assertEquals((1..39).toList(), ordered.filter { it.id <= 39 }.map { it.id })
    }

    @Test
    fun `kjv keeps the western canonical order`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "kjv")
        assertEquals((1..66).toList(), ordered.map { it.id })
    }

    @Test
    fun `ordering never drops or duplicates a book`() {
        val ordered = BibleDatabaseService.applyBookOrder(allBooks, "synodal")
        assertEquals(66, ordered.size)
        assertEquals(66, ordered.map { it.id }.toSet().size)
    }
}
