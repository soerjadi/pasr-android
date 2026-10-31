package id.oemah.nusantara.feature.store

import id.oemah.nusantara.api.model.CreateStoreRequest
import id.oemah.nusantara.api.model.Session
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.profile.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyStoreRepositoryTest {
    private val store = StoreSummary(id = 7, name = "Warung Bu Sari", status = StoreStatus.VERIFIED, newOrderCount = 2)
    private fun user(id: Long, store: StoreSummary? = this.store) = User(id = id, phone = "+6281234567890", store = store)

    private val session = MutableStateFlow<Session?>(Session("a", "r", user(1)))
    private val sessions = mockk<SessionStore> {
        every { this@mockk.session } returns this@MyStoreRepositoryTest.session
        every { current() } answers { this@MyStoreRepositoryTest.session.value }
    }
    private val profile = mockk<ProfileRepository>()
    private val repository = MyStoreRepository(profile, sessions)

    @Test fun refresh_publishes_the_store_of_the_signed_in_user() = runTest {
        coEvery { profile.me() } returns Result.success(user(1))

        repository.refresh()

        assertEquals(store, repository.store.first())
    }

    @Test fun a_user_without_a_store_yields_null() = runTest {
        coEvery { profile.me() } returns Result.success(user(1, store = null))

        repository.refresh()

        assertNull(repository.store.first())
    }

    @Test fun a_failed_refresh_keeps_the_last_known_store() = runTest {
        repository.update(user(1))
        coEvery { profile.me() } returns Result.failure(AppError.Server)

        repository.refresh()

        assertEquals(store, repository.store.first())
    }

    @Test fun logging_out_hides_the_store() = runTest {
        repository.update(user(1))

        session.value = null

        assertNull(repository.store.first())
    }

    @Test fun another_account_never_sees_the_previous_stores() = runTest {
        repository.update(user(1))

        session.value = Session("a", "r", user(2))

        assertNull(repository.store.first())
    }

    @Test fun logged_out_refresh_makes_no_call() = runTest {
        session.value = null

        repository.refresh()

        coVerify(exactly = 0) { profile.me() }
    }

    private val request = CreateStoreRequest("Warung Bu Sari", 3, "Sari", "Sambirejo", "RT 2")

    @Test fun creating_a_store_publishes_it_without_another_fetch() = runTest {
        val pending = store.copy(status = StoreStatus.PENDING, newOrderCount = 0)
        coEvery { profile.createStore(request) } returns Result.success(pending)

        repository.createStore(request)

        assertEquals(pending, repository.store.first())
        coVerify(exactly = 0) { profile.me() }
    }

    @Test fun a_failed_create_publishes_nothing() = runTest {
        coEvery { profile.createStore(request) } returns Result.failure(AppError.Conflict("STORE_EXISTS", "Sudah punya toko"))

        repository.createStore(request)

        assertNull(repository.store.first())
    }
}
