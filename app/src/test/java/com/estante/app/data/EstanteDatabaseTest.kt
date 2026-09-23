package com.estante.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estante.app.data.local.EstanteDatabase
import com.estante.app.data.local.toEntity
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.fakes.testBook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Testa o Room de verdade (banco em memória) através dos repositories. */
@RunWith(AndroidJUnit4::class)
class EstanteDatabaseTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        EstanteDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val shelf = DefaultShelfRepository(db.shelfDao())

    @After
    fun tearDown() = db.close()

    @Test
    fun buscaSalvaMantemAOrdemDosResultados() = runTest {
        val books = listOf(testBook("B"), testBook("A"), testBook("C"))
        db.bookDao().replaceSearch("kotlin", books.map { it.toEntity() }, fetchedAt = 10L)

        assertEquals(listOf("B", "A", "C"), db.bookDao().getSearchResults("kotlin").map { it.id })
        assertEquals(10L, db.bookDao().getFetchedAt("kotlin"))
    }

    @Test
    fun estanteJuntaRegistroELivroEReageASalvarERemover() = runTest {
        val book = testBook("A")
        db.bookDao().upsertBooks(listOf(book.toEntity()))

        shelf.save(ShelfEntry("A", ReadingStatus.READING, pagesRead = 10, rating = null, note = "ótimo", updatedAt = 1L))
        val items = shelf.observeShelf().first()
        assertEquals(1, items.size)
        assertEquals(book, items.first().book)
        assertEquals(ReadingStatus.READING, items.first().entry.status)

        shelf.remove("A")
        assertTrue(shelf.observeShelf().first().isEmpty())
    }
}
