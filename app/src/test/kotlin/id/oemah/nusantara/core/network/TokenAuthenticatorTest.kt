package id.oemah.nusantara.core.network

import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.RefreshRequest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private val refreshCalls = AtomicInteger()
    private val meCalls = AtomicInteger()
    private var refreshResponse: () -> MockResponse = { json(200, sessionJson("new", "r2")) }

    @Before fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/v1/auth/refresh" -> { refreshCalls.incrementAndGet(); refreshResponse() }
                "/v1/me" -> {
                    meCalls.incrementAndGet()
                    if (request.headers["Authorization"] == "Bearer new") json(200, USER_JSON) else json(401, """{"code":"X","message":"x"}""")
                }
                else -> json(404, """{"code":"NOT_FOUND","message":"x"}""")
            }
        }
        server.start()
    }

    @After fun stop() = server.close()

    private fun clientFor(store: FakeSessionStore): OkHttpClient {
        val bare = OkHttpClient.Builder().noSilentRetry().build()
        val authApi = server.retrofit(bare).create(AuthApi::class.java)
        return OkHttpClient.Builder().noSilentRetry()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(TokenAuthenticator(store, authApi))
            .build()
    }

    @Test fun a_401_refreshes_once_then_retries_with_the_new_token() = runBlocking {
        val store = FakeSessionStore(session())
        val me = server.retrofit(clientFor(store)).create(MeApi::class.java)

        val response = me.getMe()

        assertEquals(200, response.code())
        assertEquals(1, refreshCalls.get())
        assertEquals(2, meCalls.get())
        assertEquals("new", store.current()?.accessToken)
    }

    @Test fun concurrent_401s_trigger_a_single_refresh() = runBlocking(Dispatchers.IO) {
        refreshResponse = {
            MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
                .body(sessionJson("new", "r2")).bodyDelay(300, TimeUnit.MILLISECONDS).build()
        }
        val store = FakeSessionStore(session())
        val me = server.retrofit(clientFor(store)).create(MeApi::class.java)

        val codes = awaitAll(async { me.getMe().code() }, async { me.getMe().code() })

        assertEquals(listOf(200, 200), codes)
        assertEquals(1, refreshCalls.get())
    }

    @Test fun a_failed_refresh_clears_the_session_and_surfaces_the_401() = runBlocking {
        refreshResponse = { json(401, """{"code":"X","message":"x"}""") }
        val store = FakeSessionStore(session())
        val me = server.retrofit(clientFor(store)).create(MeApi::class.java)

        val response = me.getMe()

        assertEquals(401, response.code())
        assertEquals(1, store.clearCalls)
        assertNull(store.current())
    }

    @Test fun auth_endpoints_are_never_refreshed() = runBlocking {
        refreshResponse = { json(401, """{"code":"X","message":"x"}""") }
        val store = FakeSessionStore(session())
        val auth = server.retrofit(clientFor(store)).create(AuthApi::class.java)

        val response = auth.refreshSession(RefreshRequest(refreshToken = "r1"))

        assertEquals(401, response.code())
        assertEquals(1, refreshCalls.get())
        assertEquals(0, store.clearCalls)
    }

    @Test fun without_a_session_there_is_no_refresh_attempt() = runBlocking {
        val store = FakeSessionStore(null)
        val me = server.retrofit(clientFor(store)).create(MeApi::class.java)

        assertEquals(401, me.getMe().code())
        assertEquals(0, refreshCalls.get())
    }

    @Test fun bearer_token_is_sent_except_to_auth_endpoints() = runBlocking {
        val store = FakeSessionStore(session(access = "new"))
        val retrofit = server.retrofit(clientFor(store))

        retrofit.create(MeApi::class.java).getMe()
        retrofit.create(AuthApi::class.java).refreshSession(RefreshRequest("r1"))

        assertEquals("Bearer new", server.takeRequest().headers["Authorization"])
        assertNull(server.takeRequest().headers["Authorization"])
    }
}
