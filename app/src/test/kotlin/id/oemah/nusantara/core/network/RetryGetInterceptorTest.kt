package id.oemah.nusantara.core.network

import java.io.IOException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RetryGetInterceptorTest {
    private lateinit var server: MockWebServer
    private val delays = mutableListOf<Long>()
    private lateinit var client: OkHttpClient

    @Before fun start() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder().noSilentRetry()
            .addInterceptor(RetryGetInterceptor { delays += it })
            .build()
    }

    @After fun stop() = server.close()

    private fun disconnect() = MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build()

    private fun get() = client.newCall(Request.Builder().url(server.url("/x")).build()).execute()

    @Test fun get_failing_with_io_exception_is_retried_with_backoff() {
        server.enqueue(disconnect()); server.enqueue(disconnect()); server.enqueue(json(200, "{}"))

        get().use { assertEquals(200, it.code) }

        assertEquals(3, server.requestCount)
        assertEquals(listOf(300L, 900L), delays)
    }

    @Test fun get_503_is_retried() {
        server.enqueue(json(503, "{}")); server.enqueue(json(200, "{}"))

        get().use { assertEquals(200, it.code) }

        assertEquals(2, server.requestCount)
        assertEquals(listOf(300L), delays)
    }

    @Test fun get_gives_up_after_two_retries() {
        repeat(3) { server.enqueue(disconnect()) }

        assertThrows(IOException::class.java) { get() }

        assertEquals(3, server.requestCount)
        assertEquals(listOf(300L, 900L), delays)
    }

    @Test fun post_is_never_retried() {
        server.enqueue(disconnect()); server.enqueue(json(200, "{}"))
        val post = Request.Builder().url(server.url("/x")).post("{}".toRequestBody("application/json".toMediaType())).build()

        assertThrows(IOException::class.java) { client.newCall(post).execute() }

        assertEquals(1, server.requestCount)
    }

    @Test fun client_errors_are_not_retried() {
        server.enqueue(json(404, "{}"))

        get().use { assertEquals(404, it.code) }

        assertEquals(1, server.requestCount)
    }
}
