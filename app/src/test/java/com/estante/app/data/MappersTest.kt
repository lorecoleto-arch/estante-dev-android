package com.estante.app.data

import com.estante.app.data.remote.BookDocDto
import com.estante.app.data.remote.WorkDto
import com.estante.app.data.remote.cleanDescription
import com.estante.app.data.remote.toDescription
import com.estante.app.data.remote.toDomain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** DTO (formato da API) -> modelo de domínio (formato do app). */
class MappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `converte o JSON da busca em Book tratando campos ausentes`() {
        val dto = json.decodeFromString<BookDocDto>(
            """{"key": "/works/OL1W", "title": " Código Limpo ", "author_name": ["Robert C. Martin"],
               "subject": ["Programming", "programming", "Software engineering"], "campo_novo": 1}""",
        )

        val book = dto.toDomain()!!

        assertEquals("OL1W", book.id)
        assertEquals("Código Limpo", book.title)
        assertNull(book.firstPublishYear)
        assertNull(book.coverUrl)
        assertEquals(listOf("Programming", "Software engineering"), book.subjects)
    }

    @Test
    fun `livro sem titulo e descartado`() {
        assertNull(BookDocDto(key = "/works/OL2W", title = "  ").toDomain())
    }

    @Test
    fun `descricao pode vir como texto ou como objeto`() {
        val asObject = json.decodeFromString<WorkDto>("""{"description": {"type": "/type/text", "value": "Olá"}}""")
        assertEquals("Olá", asObject.toDescription())
        assertEquals("Oi", WorkDto(JsonPrimitive("Oi")).toDescription())
        assertNull(WorkDto(null).toDescription())
    }

    @Test
    fun `limpa markdown e listas anexadas na descricao`() {
        val raw = "**Código Limpo** é um [clássico](https://x.org).\r\n----------\nContém: ..."
        assertEquals("Código Limpo é um clássico.", cleanDescription(raw))
    }
}
