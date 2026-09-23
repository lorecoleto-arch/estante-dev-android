package com.estante.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.estante.app.data.BookRepository
import com.estante.app.data.DefaultBookRepository
import com.estante.app.data.DefaultShelfRepository
import com.estante.app.data.ShelfRepository
import com.estante.app.data.local.EstanteDatabase
import com.estante.app.data.preferences.DataStoreUserPreferencesRepository
import com.estante.app.data.preferences.UserPreferencesRepository
import com.estante.app.data.remote.OpenLibraryApi
import com.estante.app.data.remote.OpenLibraryRemoteDataSource
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Injeção de dependências manual: um único lugar cria e guarda as dependências do app.
 * As telas recebem só as interfaces (BookRepository, ShelfRepository...).
 */
interface AppContainer {
    val bookRepository: BookRepository
    val shelfRepository: ShelfRepository
    val preferences: UserPreferencesRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // A Open Library pede que apps se identifiquem pelo User-Agent.
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "EstanteDev/1.0 (projeto academico Android)")
                        .build(),
                )
            }
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val api: OpenLibraryApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenLibraryApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenLibraryApi::class.java)
    }

    private val database: EstanteDatabase by lazy { EstanteDatabase.create(appContext) }

    override val bookRepository: BookRepository by lazy {
        DefaultBookRepository(OpenLibraryRemoteDataSource(api), database.bookDao())
    }

    override val shelfRepository: ShelfRepository by lazy {
        DefaultShelfRepository(database.shelfDao())
    }

    override val preferences: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(appContext.dataStore)
    }
}
