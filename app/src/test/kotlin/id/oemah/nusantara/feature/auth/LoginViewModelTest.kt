package id.oemah.nusantara.feature.auth

import app.cash.turbine.test
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        val otpRequests = mutableListOf<String>()
        val verifications = mutableListOf<Pair<String, String>>()
        var requestResult: Result<Unit> = Result.success(Unit)
        var verifyResult: Result<Unit> = Result.success(Unit)
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun requestOtp(phoneE164: String): Result<Unit> {
            otpRequests += phoneE164
            gate?.await()
            return requestResult
        }

        override suspend fun verifyOtp(phoneE164: String, code: String): Result<Unit> {
            verifications += phoneE164 to code
            return verifyResult
        }

        override suspend fun logout() = Unit
    }

    private val repo = FakeAuthRepository()
    private val viewModel by lazy { LoginViewModel(repo) }
    private val phone = "+6281234567890"

    private fun TestScope.enterCodeStep() {
        viewModel.submitPhone("0812-3456-7890")
        runCurrent()
    }

    @Test fun invalid_phone_shows_an_error_and_sends_nothing() = runTest {
        viewModel.submitPhone("12345")
        runCurrent()

        assertEquals(LoginUiState.Phone(error = "Nomor HP tidak valid"), viewModel.state.value)
        assertEquals(emptyList<String>(), repo.otpRequests)
    }

    @Test fun valid_phone_requests_the_normalized_number_and_moves_to_the_code_step() = runTest {
        enterCodeStep()

        assertEquals(listOf(phone), repo.otpRequests)
        assertEquals(LoginUiState.Code(phoneE164 = phone, resendInSeconds = 60), viewModel.state.value)
    }

    @Test fun a_second_submit_while_the_request_is_in_flight_is_ignored() = runTest {
        repo.gate = CompletableDeferred()

        viewModel.submitPhone("0812-3456-7890")
        viewModel.submitPhone("0812-3456-7890")
        runCurrent()

        assertEquals(1, repo.otpRequests.size)
    }

    @Test fun rate_limiting_on_the_phone_step_shows_a_message() = runTest {
        repo.requestResult = Result.failure(AppError.Rejected("OTP_RATE_LIMITED", "x"))

        viewModel.submitPhone("0812-3456-7890")
        runCurrent()

        assertEquals(LoginUiState.Phone(error = "Terlalu banyak percobaan. Coba lagi nanti."), viewModel.state.value)
    }

    @Test fun offline_shows_the_offline_message() = runTest {
        repo.requestResult = Result.failure(AppError.Offline)

        viewModel.submitPhone("0812-3456-7890")
        runCurrent()

        val error = (viewModel.state.value as LoginUiState.Phone).error
        assertEquals("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.", error)
    }

    @Test fun a_wrong_code_shows_a_message() = runTest {
        enterCodeStep()
        repo.verifyResult = Result.failure(AppError.Rejected("OTP_INVALID", "x"))

        viewModel.submitCode("1234")
        runCurrent()

        assertEquals("Kode salah. Coba lagi.", (viewModel.state.value as LoginUiState.Code).error)
        assertEquals(listOf(phone to "1234"), repo.verifications)
    }

    @Test fun a_successful_verification_emits_logged_in() = runTest {
        enterCodeStep()

        viewModel.events.test {
            viewModel.submitCode("1234")
            runCurrent()
            assertEquals(LoginEvent.LoggedIn, awaitItem())
        }
    }

    @Test fun resend_is_disabled_for_60_seconds() = runTest {
        enterCodeStep()

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, (viewModel.state.value as LoginUiState.Code).resendInSeconds)
        viewModel.resend()
        runCurrent()
        assertEquals(1, repo.otpRequests.size)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(0, (viewModel.state.value as LoginUiState.Code).resendInSeconds)
        viewModel.resend()
        runCurrent()
        assertEquals(2, repo.otpRequests.size)
        assertEquals(60, (viewModel.state.value as LoginUiState.Code).resendInSeconds)
    }

    @Test fun rate_limiting_on_resend_shows_a_message_and_rearms_the_cooldown() = runTest {
        enterCodeStep()
        advanceTimeBy(60_000)
        runCurrent()
        repo.requestResult = Result.failure(AppError.Rejected("OTP_RATE_LIMITED", "x"))

        viewModel.resend()
        runCurrent()

        val state = viewModel.state.value as LoginUiState.Code
        assertEquals("Terlalu banyak percobaan. Coba lagi nanti.", state.error)
        assertEquals(60, state.resendInSeconds)
    }
}
