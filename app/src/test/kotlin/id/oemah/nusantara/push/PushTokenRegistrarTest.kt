package id.oemah.nusantara.push

import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.profile.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PushTokenRegistrarTest {
    private val profile = mockk<ProfileRepository>()

    private fun TestScope.registrar(sessions: FakeSessionStore): PushTokenRegistrar {
        coEvery { profile.registerDevice(any()) } returns Result.success(Unit)
        return PushTokenRegistrar(profile, sessions, backgroundScope).also { runCurrent() }
    }

    @Test fun a_token_received_while_logged_in_is_registered_once() = runTest {
        val registrar = registrar(FakeSessionStore(session()))

        registrar.onToken("t1"); runCurrent()

        coVerify(exactly = 1) { profile.registerDevice("t1") }
    }

    @Test fun a_token_received_while_logged_out_is_held_until_login() = runTest {
        val sessions = FakeSessionStore(null)
        val registrar = registrar(sessions)

        registrar.onToken("t1"); runCurrent()
        coVerify(exactly = 0) { profile.registerDevice(any()) }

        sessions.save(session()); runCurrent()

        coVerify(exactly = 1) { profile.registerDevice("t1") }
    }

    @Test fun the_same_token_is_not_registered_twice() = runTest {
        val registrar = registrar(FakeSessionStore(session()))

        registrar.onToken("t1"); runCurrent()
        registrar.onToken("t1"); runCurrent()

        coVerify(exactly = 1) { profile.registerDevice("t1") }
    }

    @Test fun a_refreshed_token_is_registered_too() = runTest {
        val registrar = registrar(FakeSessionStore(session()))

        registrar.onToken("t1"); runCurrent()
        registrar.onToken("t2"); runCurrent()

        coVerify(exactly = 1) { profile.registerDevice("t1") }
        coVerify(exactly = 1) { profile.registerDevice("t2") }
    }

    @Test fun a_failed_registration_is_retried_by_the_next_trigger() = runTest {
        val registrar = registrar(FakeSessionStore(session()))
        coEvery { profile.registerDevice("t1") } returns Result.failure(AppError.Offline)

        registrar.onToken("t1"); runCurrent()
        coEvery { profile.registerDevice("t1") } returns Result.success(Unit)
        registrar.onToken("t1"); runCurrent()

        coVerify(exactly = 2) { profile.registerDevice("t1") }
    }

    @Test fun logging_out_and_in_again_registers_the_device_for_the_new_user() = runTest {
        val sessions = FakeSessionStore(session())
        val registrar = registrar(sessions)
        registrar.onToken("t1"); runCurrent()

        sessions.clear(); runCurrent()
        sessions.save(session()); runCurrent()

        coVerify(exactly = 2) { profile.registerDevice("t1") }
    }
}
