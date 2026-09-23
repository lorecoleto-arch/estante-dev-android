package com.estante.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.domain.ShelfItem

/** Livro em cache (catálogo). */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authors: List<String>,
    val firstPublishYear: Int?,
    val coverId: Long?,
    val pageCount: Int?,
    val subjects: List<String>,
)

/** Descrição do livro, buscada sob demanda na tela de detalhes. */
@Entity(tableName = "book_descriptions")
data class BookDescriptionEntity(
    @PrimaryKey val bookId: String,
    /** `null` = a API respondeu, mas o livro não tem descrição. */
    val description: String?,
)

/** Quais livros apareceram (e em que ordem) para cada termo buscado. */
@Entity(tableName = "search_results", primaryKeys = ["query", "bookId"])
data class SearchResultEntity(
    val query: String,
    val bookId: String,
    val position: Int,
)

/** Quando cada termo foi buscado na API — base da política de expiração do cache. */
@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val fetchedAt: Long,
)

/** Registro de leitura do usuário (os dados "do usuário" do app). */
@Entity(tableName = "shelf_entries")
data class ShelfEntryEntity(
    @PrimaryKey val bookId: String,
    val status: ReadingStatus,
    val pagesRead: Int?,
    val rating: Int?,
    val note: String?,
    val updatedAt: Long,
)

/** Resultado do JOIN estante + livro. */
data class ShelfRow(
    @Embedded val entry: ShelfEntryEntity,
    @Relation(parentColumn = "bookId", entityColumn = "id")
    val book: BookEntity?,
)

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun fromStatus(value: ReadingStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ReadingStatus = ReadingStatus.valueOf(value)

    private companion object {
        /** Separador improvável em nomes de autores/assuntos. */
        const val SEPARATOR = "\u001F"
    }
}

// --- Mapeamentos entre entidades e domínio ---

fun BookEntity.toDomain() = Book(id, title, authors, firstPublishYear, coverId, pageCount, subjects)

fun Book.toEntity() = BookEntity(id, title, authors, firstPublishYear, coverId, pageCount, subjects)

fun ShelfEntryEntity.toDomain() = ShelfEntry(bookId, status, pagesRead, rating, note, updatedAt)

fun ShelfEntry.toEntity() = ShelfEntryEntity(bookId, status, pagesRead, rating, note, updatedAt)

fun ShelfRow.toDomain(): ShelfItem? = book?.let { ShelfItem(it.toDomain(), entry.toDomain()) }
