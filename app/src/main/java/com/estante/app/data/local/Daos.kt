package com.estante.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query(
        """
        SELECT b.* FROM books b
        INNER JOIN search_results r ON r.bookId = b.id
        WHERE r.query = :query
        ORDER BY r.position
        """,
    )
    suspend fun getSearchResults(query: String): List<BookEntity>

    @Query("SELECT fetchedAt FROM search_queries WHERE query = :query")
    suspend fun getFetchedAt(query: String): Long?

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Upsert
    suspend fun upsertSearchResults(results: List<SearchResultEntity>)

    @Upsert
    suspend fun upsertSearchQuery(query: SearchQueryEntity)

    @Query("DELETE FROM search_results WHERE query = :query")
    suspend fun clearSearchResults(query: String)

    /** Troca o resultado salvo de uma busca de forma atômica (tudo ou nada). */
    @Transaction
    suspend fun replaceSearch(query: String, books: List<BookEntity>, fetchedAt: Long) {
        upsertBooks(books)
        clearSearchResults(query)
        upsertSearchResults(books.mapIndexed { index, book -> SearchResultEntity(query, book.id, index) })
        upsertSearchQuery(SearchQueryEntity(query, fetchedAt))
    }

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: String): BookEntity?

    @Query("SELECT * FROM book_descriptions WHERE bookId = :id")
    fun observeDescription(id: String): Flow<BookDescriptionEntity?>

    @Query("SELECT COUNT(*) FROM book_descriptions WHERE bookId = :id")
    suspend fun hasDescription(id: String): Int

    @Upsert
    suspend fun upsertDescription(description: BookDescriptionEntity)
}

@Dao
interface ShelfDao {

    @Transaction
    @Query("SELECT * FROM shelf_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ShelfRow>>

    @Query("SELECT * FROM shelf_entries WHERE bookId = :bookId")
    fun observeEntry(bookId: String): Flow<ShelfEntryEntity?>

    @Upsert
    suspend fun upsert(entry: ShelfEntryEntity)

    @Query("DELETE FROM shelf_entries WHERE bookId = :bookId")
    suspend fun delete(bookId: String)
}
