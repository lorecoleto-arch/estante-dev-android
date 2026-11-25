package com.estante.app.data.remote

import com.estante.app.domain.Book
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException

/** Fonte remota de livros. É uma interface para podermos usar um fake nos testes. */
interface BookRemoteDataSource {
    suspend fun search(query: String): List<Book>
    suspend fun fetchDescription(bookId: String): String?
}

class OpenLibraryRemoteDataSource(
    private val api: OpenLibraryApi,
) : BookRemoteDataSource {

    override suspend fun search(query: String): List<Book> =
        retryOnce { api.search(query) }.docs.mapNotNull { it.toDomain() }.distinctBy { it.id }

    override suspend fun fetchDescription(bookId: String): String? =
        retryOnce { api.work(bookId) }.toDescription()

    /** A Open Library falha de vez em quando e responde na chamada seguinte: tenta mais uma vez. */
    private suspend fun <T> retryOnce(call: suspend () -> T): T =
        try {
            call()
        } catch (e: Exception) {
            if (!e.isTransient()) throw e
            delay(RETRY_DELAY_MILLIS)
            call()
        }

    private fun Exception.isTransient(): Boolean = when (this) {
        is SocketTimeoutException, is ConnectException -> true // demorou demais ou recusou a conexão
        is HttpException -> code() >= 500
        else -> false // sem internet, cancelamento, pedido inválido...
    }

    private companion object {
        const val RETRY_DELAY_MILLIS = 1_000L
    }
}

/** DTO -> modelo de domínio. Itens sem título são descartados. */
internal fun BookDocDto.toDomain(): Book? {
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isEmpty()) return null
    return Book(
        id = key.removePrefix("/works/"),
        title = cleanTitle,
        authors = authorNames.orEmpty().take(3),
        firstPublishYear = firstPublishYear,
        coverId = coverId?.takeIf { it > 0 },
        pageCount = pageCount?.takeIf { it > 0 },
        subjects = subjects.orEmpty().map { it.trim() }.distinctBy { it.lowercase() }.take(8),
    )
}

/** A descrição vem como string ou como objeto {"value": ...}, e às vezes com Markdown. */
internal fun WorkDto.toDescription(): String? {
    val raw = when (description) {
        is JsonPrimitive -> description.content
        is JsonObject -> description["value"]?.jsonPrimitive?.content
        else -> null
    } ?: return null
    return cleanDescription(raw).takeIf { it.isNotEmpty() }
}

private val MarkdownLink = Regex("""\[([^\]]+)]\([^)]*\)""")

internal fun cleanDescription(raw: String): String = raw
    .replace("\r\n", "\n")
    .substringBefore("----------") // a Open Library costuma anexar listas de edições depois disso
    .replace(MarkdownLink, "$1") // [texto](link) -> texto
    .replace("**", "")
    .replace("__", "")
    .trim()
