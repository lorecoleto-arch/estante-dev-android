package com.estante.app.domain

/** Livro do catálogo (vem da Open Library e fica em cache no Room). */
data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val firstPublishYear: Int?,
    val coverId: Long?,
    val pageCount: Int?,
    val subjects: List<String>,
) {
    val coverUrl: String?
        get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }

    val shareUrl: String
        get() = "https://openlibrary.org/works/$id"
}

/** Situação de leitura que o usuário registra. */
enum class ReadingStatus { WANT_TO_READ, READING, FINISHED }

/** Registro do usuário para um livro da sua estante. */
data class ShelfEntry(
    val bookId: String,
    val status: ReadingStatus,
    /** Só faz sentido enquanto está lendo. */
    val pagesRead: Int?,
    /** Nota de 1 a 5, obrigatória apenas para livros lidos. */
    val rating: Int?,
    val note: String?,
    val updatedAt: Long,
)

/** Livro + registro, usado na tela "Minha estante". */
data class ShelfItem(
    val book: Book,
    val entry: ShelfEntry,
)

/** Resultado de uma busca: [isStale] indica que veio do cache porque a rede falhou. */
data class SearchResult(
    val books: List<Book>,
    val isStale: Boolean,
)
