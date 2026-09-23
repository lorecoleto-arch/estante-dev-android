package com.estante.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfEntryValidatorTest {

    private fun validate(
        status: ReadingStatus,
        pages: String = "",
        rating: Int? = null,
        note: String = "",
        total: Int? = 200,
    ) = ShelfEntryValidator.validate(status, pages, rating, note, total)

    @Test
    fun `quero ler nao exige paginas nem nota`() {
        assertTrue(validate(ReadingStatus.WANT_TO_READ).isValid)
    }

    @Test
    fun `lendo exige paginas`() {
        assertEquals(FieldError.PAGES_REQUIRED, validate(ReadingStatus.READING, pages = "  ").pagesError)
    }

    @Test
    fun `lendo rejeita texto que nao e numero`() {
        assertEquals(FieldError.PAGES_NOT_A_NUMBER, validate(ReadingStatus.READING, pages = "12a").pagesError)
    }

    @Test
    fun `lendo rejeita paginas acima do total ou negativas`() {
        assertEquals(FieldError.PAGES_OUT_OF_RANGE, validate(ReadingStatus.READING, pages = "201").pagesError)
        assertEquals(FieldError.PAGES_OUT_OF_RANGE, validate(ReadingStatus.READING, pages = "-1").pagesError)
    }

    @Test
    fun `lendo aceita qualquer numero positivo quando o total e desconhecido`() {
        assertNull(validate(ReadingStatus.READING, pages = "5000", total = null).pagesError)
    }

    @Test
    fun `lido exige nota de 1 a 5`() {
        assertEquals(FieldError.RATING_REQUIRED, validate(ReadingStatus.FINISHED).ratingError)
        assertEquals(FieldError.RATING_REQUIRED, validate(ReadingStatus.FINISHED, rating = 6).ratingError)
        assertTrue(validate(ReadingStatus.FINISHED, rating = 4).isValid)
    }

    @Test
    fun `anotacao tem limite de caracteres`() {
        val tooLong = "a".repeat(ShelfEntryValidator.NOTE_MAX_LENGTH + 1)
        assertEquals(FieldError.NOTE_TOO_LONG, validate(ReadingStatus.WANT_TO_READ, note = tooLong).noteError)
        val limit = "a".repeat(ShelfEntryValidator.NOTE_MAX_LENGTH)
        assertTrue(validate(ReadingStatus.WANT_TO_READ, note = "  $limit  ").isValid)
    }
}
