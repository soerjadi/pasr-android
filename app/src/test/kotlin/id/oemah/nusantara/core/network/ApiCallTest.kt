package id.oemah.nusantara.core.network

import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.DeviceRequest
import id.oemah.nusantara.core.AppError
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiCallTest {
    private lateinit var server: MockWebServer
    private lateinit var me: MeApi

    @Before fun start() {
        server = MockWebServer().apply { start() }
        me = server.retrofit(OkHttpClient.Builder().noSilentRetry().build()).create(MeApi::class.java)
    }

    @After fun stop() = server.close()

    private fun failure(response: MockResponse): Throwable? = runBlocking {
        server.enqueue(response)
        apiCall { me.getMe() }.exceptionOrNull()
    }

    @Test fun io_failure_is_offline() {
        val response = MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build()
        assertEquals(AppError.Offline, failure(response))
    }

    @Test fun http_401_is_unauthorized() {
        assertEquals(AppError.Unauthorized, failure(json(401, """{"code":"X","message":"x"}""")))
    }

    @Test fun http_409_is_a_conflict_with_the_server_code() {
        val error = failure(json(409, """{"code":"INSUFFICIENT_STOCK","message":"x"}"""))
        assertEquals(AppError.Conflict("INSUFFICIENT_STOCK", "x"), error)
    }

    @Test fun other_4xx_is_rejected_with_the_server_code() {
        assertEquals(AppError.Rejected("OTP_INVALID", "Kode salah"), failure(json(400, """{"code":"OTP_INVALID","message":"Kode salah"}""")))
        assertEquals(AppError.Rejected("OTP_RATE_LIMITED", "tunggu"), failure(json(429, """{"code":"OTP_RATE_LIMITED","message":"tunggu"}""")))
    }

    @Test fun a_4xx_without_a_parsable_body_is_still_rejected() {
        val error = failure(json(422, "not json"))
        assertTrue(error is AppError.Rejected)
    }

    @Test fun http_500_is_server() {
        assertEquals(AppError.Server, failure(json(500, "{}")))
    }

    @Test fun malformed_success_body_is_unknown() {
        assertTrue(failure(json(200, "{bad")) is AppError.Unknown)
    }

    @Test fun success_returns_the_body() = runBlocking {
        server.enqueue(json(200, USER_JSON))
        assertEquals("+6281234567890", apiCall { me.getMe() }.getOrThrow().phone)
    }

    @Test fun no_content_is_success_for_unit_responses() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertTrue(apiCall { me.registerDevice(DeviceRequest("token")) }.isSuccess)
    }
}
