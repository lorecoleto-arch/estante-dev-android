package com.estante.app.data

import com.estante.app.data.local.ShelfDao
import com.estante.app.data.local.toDomain
import com.estante.app.data.local.toEntity
import com.estante.app.domain.ShelfEntry
import com.estante.app.domain.ShelfItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Estante do usuário. Os dados são expostos como Flow: a tela atualiza sozinha quando algo muda. */
interface ShelfRepository {
    fun observeShelf(): Flow<List<ShelfItem>>
    fun observeEntry(bookId: String): Flow<ShelfEntry?>
    suspend fun save(entry: ShelfEntry)
    suspend fun remove(bookId: String)
}

class DefaultShelfRepository(
    private val shelfDao: ShelfDao,
) : ShelfRepository {

    override fun observeShelf(): Flow<List<ShelfItem>> =
        shelfDao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeEntry(bookId: String): Flow<ShelfEntry?> =
        shelfDao.observeEntry(bookId).map { it?.toDomain() }

    override suspend fun save(entry: ShelfEntry) = shelfDao.upsert(entry.toEntity())

    override suspend fun remove(bookId: String) = shelfDao.delete(bookId)
}
