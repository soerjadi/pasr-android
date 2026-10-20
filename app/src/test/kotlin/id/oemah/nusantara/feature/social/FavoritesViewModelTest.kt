package id.oemah.nusantara.feature.social

import androidx.paging.PagingData
import app.cash.turbine.test
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val social = mockk<SocialRepository>()

    private fun TestScope.viewModel(sessions: FakeSessionStore): FavoritesViewModel {
        every { social.favorites() } returns flowOf(PagingData.empty<ProductSummary>())
        val vm = FavoritesViewModel(social, FakeCartRepository(), sessions)
        vm.favorites.onEach { }.launchIn(backgroundScope)
        runCurrent()
        return vm
    }

    @Test fun logged_out_does_not_call_the_api_and_reports_it() = runTest {
        val vm = viewModel(FakeSessionStore(null))

        assertFalse(vm.loggedIn.value)
        verify(exactly = 0) { social.favorites() }
    }

    @Test fun logging_in_starts_loading_the_favorites() = runTest {
        val sessions = FakeSessionStore(null)
        val vm = viewModel(sessions)

        sessions.save(session()); runCurrent()

        assertTrue(vm.loggedIn.value)
        verify(exactly = 1) { social.favorites() }
    }

    @Test fun removing_hides_the_row_immediately_and_unfavorites_on_the_server() = runTest {
        coEvery { social.setFavorite(1, false) } returns Result.success(Unit)
        val vm = viewModel(FakeSessionStore(session()))

        vm.remove(1)

        assertEquals(setOf(1L), vm.removedIds.value)
        runCurrent()
        coVerify(exactly = 1) { social.setFavorite(1, false) }
    }

    @Test fun a_successful_removal_reloads_the_list_from_the_server() = runTest {
        coEvery { social.setFavorite(1, false) } returns Result.success(Unit)
        val vm = viewModel(FakeSessionStore(session()))

        vm.remove(1); runCurrent()

        verify(exactly = 2) { social.favorites() }
        assertEquals(setOf(1L), vm.removedIds.value) // stays hidden until the reloaded list arrives
    }

    @Test fun a_failed_removal_brings_the_row_back_and_tells_the_user() = runTest {
        coEvery { social.setFavorite(1, false) } returns Result.failure(AppError.Offline)
        val vm = viewModel(FakeSessionStore(session()))

        vm.events.test {
            vm.remove(1); runCurrent()
            assertTrue(awaitItem() is UiEvent.Message)
        }
        assertEquals(emptySet<Long>(), vm.removedIds.value)
        verify(exactly = 1) { social.favorites() }
    }
}
