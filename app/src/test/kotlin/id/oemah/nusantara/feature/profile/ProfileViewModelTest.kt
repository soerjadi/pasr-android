package id.oemah.nusantara.feature.profile

import app.cash.turbine.test
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.auth.AuthRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val profile = mockk<ProfileRepository>()
    private val user = User(id = 1, phone = "+6281234567890", name = "Budi")

    /** Logging out through the real contract clears the shared session, like [DefaultAuthRepository]. */
    private class FakeAuth(private val sessions: FakeSessionStore) : AuthRepository {
        var logouts = 0
        override suspend fun requestOtp(phoneE164: String) = Result.success(Unit)
        override suspend fun verifyOtp(phoneE164: String, code: String) = Result.success(Unit)
        override suspend fun logout() { logouts++; sessions.clear() }
    }

    private fun viewModel(sessions: FakeSessionStore = FakeSessionStore(session()), support: String = "081234567890"): Triple<ProfileViewModel, FakeAuth, FakeSessionStore> {
        coEvery { profile.me() } returns Result.success(user)
        val auth = FakeAuth(sessions)
        return Triple(ProfileViewModel(profile, auth, sessions, support), auth, sessions)
    }

    @Test fun loads_the_user_when_logged_in() = runTest {
        val (vm, _, _) = viewModel(); runCurrent()

        assertTrue(vm.loggedIn.value)
        assertEquals(UiState.Content(user), vm.user.value)
    }

    @Test fun logged_out_has_no_user_and_never_asks_the_server() = runTest {
        val (vm, _, _) = viewModel(FakeSessionStore(null)); runCurrent()

        assertFalse(vm.loggedIn.value)
        coVerify(exactly = 0) { profile.me() }
    }

    @Test fun a_load_failure_is_an_error_state() = runTest {
        val (vm, _, _) = viewModel()
        coEvery { profile.me() } returns Result.failure(AppError.Offline)

        vm.reload(); runCurrent()

        assertEquals(UiState.Error(AppError.Offline), vm.user.value)
    }

    @Test fun a_name_is_trimmed_and_saved() = runTest {
        val (vm, _, _) = viewModel(); runCurrent()
        coEvery { profile.updateName("Siti") } returns Result.success(user.copy(name = "Siti"))

        vm.updateName("  Siti "); runCurrent()

        coVerify(exactly = 1) { profile.updateName("Siti") }
        assertEquals("Siti", (vm.user.value as UiState.Content).value.name)
    }

    @Test fun a_blank_or_too_long_name_is_refused_without_calling_the_server() = runTest {
        val (vm, _, _) = viewModel(); runCurrent()

        vm.events.test {
            vm.updateName("   "); runCurrent()
            assertEquals(UiEvent.Message("Nama harus 1-50 karakter."), awaitItem())
            vm.updateName("a".repeat(51)); runCurrent()
            assertEquals(UiEvent.Message("Nama harus 1-50 karakter."), awaitItem())
        }
        coVerify(exactly = 0) { profile.updateName(any()) }
    }

    @Test fun a_fifty_character_name_is_accepted() = runTest {
        val (vm, _, _) = viewModel(); runCurrent()
        val name = "a".repeat(50)
        coEvery { profile.updateName(name) } returns Result.success(user.copy(name = name))

        vm.updateName(name); runCurrent()

        coVerify(exactly = 1) { profile.updateName(name) }
    }

    @Test fun a_failed_save_tells_the_user() = runTest {
        val (vm, _, _) = viewModel(); runCurrent()
        coEvery { profile.updateName(any()) } returns Result.failure(AppError.Server)

        vm.events.test {
            vm.updateName("Siti"); runCurrent()
            assertEquals(UiEvent.Message("Terjadi gangguan pada server. Coba lagi nanti."), awaitItem())
        }
    }

    @Test fun logging_out_revokes_the_session_and_reports_logged_out() = runTest {
        val (vm, auth, _) = viewModel(); runCurrent()

        vm.logout(); runCurrent()

        assertEquals(1, auth.logouts)
        assertFalse(vm.loggedIn.value)
    }

    @Test fun the_support_link_is_built_from_the_configured_number() = runTest {
        assertEquals("https://wa.me/6281234567890", viewModel(support = "081234567890").first.supportUrl)
    }

    @Test fun a_blank_or_invalid_support_number_hides_the_help_row() = runTest {
        assertNull(viewModel(support = "").first.supportUrl)
        assertNull(viewModel(support = "12345").first.supportUrl)
    }
}
