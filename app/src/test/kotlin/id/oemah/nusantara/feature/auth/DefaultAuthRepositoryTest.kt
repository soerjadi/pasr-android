package id.oemah.nusantara.feature.auth

import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.model.OtpVerify
import id.oemah.nusantara.api.model.RefreshRequest
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class DefaultAuthRepositoryTest {
    private val api = mockk<AuthApi>()
    private val store = FakeSessionStore()
    private val repo = DefaultAuthRepository(api, store)

    @Test fun a_successful_verification_saves_the_session() = runBlocking {
        coEvery { api.verifyOtp(OtpVerify("+6281234567890", "1234")) } returns Response.success(session(access = "a", refresh = "r"))

        val result = repo.verifyOtp("+6281234567890", "1234")

        assertEquals(Unit, result.getOrThrow())
        assertEquals("a", store.current()?.accessToken)
    }

    @Test fun a_failed_verification_saves_nothing() = runBlocking {
        coEvery { api.verifyOtp(any()) } returns Response.error(400, """{"code":"OTP_INVALID","message":"x"}""".toResponseBody())

        val result = repo.verifyOtp("+6281234567890", "0000")

        assertEquals(AppError.Rejected("OTP_INVALID", "x"), result.exceptionOrNull())
        assertNull(store.current())
    }

    @Test fun logout_revokes_the_refresh_token_then_clears_the_session() = runBlocking {
        val loggedIn = FakeSessionStore(session(refresh = "r9"))
        coEvery { api.logout(RefreshRequest("r9")) } returns Response.success(Unit)

        DefaultAuthRepository(api, loggedIn).logout()

        coVerify { api.logout(RefreshRequest("r9")) }
        assertNull(loggedIn.current())
    }

    @Test fun logout_clears_the_session_even_when_the_server_is_unreachable() = runBlocking {
        val loggedIn = FakeSessionStore(session())
        coEvery { api.logout(any()) } throws IOException("offline")

        DefaultAuthRepository(api, loggedIn).logout()

        assertNull(loggedIn.current())
    }
}
