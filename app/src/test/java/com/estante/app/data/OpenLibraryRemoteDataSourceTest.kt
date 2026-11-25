package com.estante.app.data

import com.estante.app.data.remote.BookDocDto
import com.estante.app.data.remote.OpenLibraryApi
import com.estante.app.data.remote.OpenLibraryRemoteDataSource
import com.estante.app.data.remote.SearchResponseDto
import com.estante.app.data.remote.WorkDto
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Nova tentativa automática quando a Open Library falha de forma passageira. */
class OpenLibraryRemoteDataSourceTest {

    private val api = FakeApi()
    private val remote = OpenLibraryRemoteDataSource(api)

    @Test
    fun `falha passageira e contornada com uma segunda tentativa`() = runTest {
        api.failures += SocketTimeoutException()
        assertEquals(1, remote.search("kotlin").size)
        assertEquals(2, api.calls)
    }

    @Test
    fun `erro 503 do servidor tambem e tentado de novo`() = runTest {
        api.failures += httpError(503)
        assertEquals(1, remote.search("kotlin").size)
        assertEquals(2, api.calls)
    }

    @Test
    fun `se a segunda tentativa falhar o erro chega a tela`() = runTest {
        api.failures += SocketTimeoutException()
        api.failures += SocketTimeoutException()
        val error = runCatching { remote.search("kotlin") }.exceptionOrNull()
        assertTrue(error is SocketTimeoutException)
        assertEquals(2, api.calls)
    }

    @Test
    fun `sem internet ou pedido invalido nao insiste`() = runTest {
        api.failures += UnknownHostException()
        runCatching { remote.search("kotlin") }
        assertEquals(1, api.calls)

        api.failures += httpError(404)
        runCatching { remote.search("kotlin") }
        assertEquals(2, api.calls)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private class FakeApi : OpenLibraryApi {
        val failures = ArrayDeque<Exception>()
        var calls = 0

        override suspend fun search(query: String, fields: String, limit: Int): SearchResponseDto {
            calls++
            failures.removeFirstOrNull()?.let { throw it }
            return SearchResponseDto(numFound = 1, docs = listOf(BookDocDto(key = "/works/OL1W", title = "Código Limpo")))
        }

        override suspend fun work(workId: String) = WorkDto()
    }
}
