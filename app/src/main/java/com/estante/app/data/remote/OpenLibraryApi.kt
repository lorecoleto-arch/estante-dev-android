package com.estante.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API pública da Open Library (https://openlibrary.org/developers/api).
 * Não exige chave de acesso, então não há nenhum segredo no projeto.
 */
interface OpenLibraryApi {

    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("limit") limit: Int = 30,
    ): SearchResponseDto

    @GET("works/{id}.json")
    suspend fun work(@Path("id") workId: String): WorkDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        private const val SEARCH_FIELDS =
            "key,title,author_name,first_publish_year,cover_i,number_of_pages_median,subject"
    }
}

// --- DTOs: espelham exatamente o JSON da API ---

@Serializable
data class SearchResponseDto(
    @SerialName("numFound") val numFound: Int = 0,
    val docs: List<BookDocDto> = emptyList(),
)

@Serializable
data class BookDocDto(
    /** Ex.: "/works/OL45804W" */
    val key: String,
    val title: String? = null,
    @SerialName("author_name") val authorNames: List<String>? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("number_of_pages_median") val pageCount: Int? = null,
    @SerialName("subject") val subjects: List<String>? = null,
)

@Serializable
data class WorkDto(
    /** A API devolve às vezes uma string, às vezes um objeto {"type": ..., "value": ...}. */
    val description: JsonElement? = null,
)
