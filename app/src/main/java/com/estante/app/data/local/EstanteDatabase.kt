package com.estante.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookEntity::class,
        BookDescriptionEntity::class,
        SearchResultEntity::class,
        SearchQueryEntity::class,
        ShelfEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class EstanteDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun shelfDao(): ShelfDao

    companion object {
        fun create(context: Context): EstanteDatabase =
            Room.databaseBuilder(context, EstanteDatabase::class.java, "estante.db").build()
    }
}
