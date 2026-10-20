package id.oemah.nusantara.feature.orders

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.testing.testOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val orders = mockk<OrdersRepository>()
    private val ads = mockk<AdsRepository>()
    private val waitAd = Ad(1, AdPlacementKey.WAIT_STATE, "Indosat", image = ImageSet("s", "m", "l"), targetUrl = "https://a.id", label = "Kemitraan")

    private fun viewModel(status: OrderStatus = OrderStatus.READY_FOR_PICKUP): TrackingViewModel {
        coEvery { orders.order(7) } returns Result.success(testOrder(id = 7, status = status))
        coEvery { ads.ads(AdPlacement.WAIT_STATE) } returns Result.success(listOf(waitAd))
        return TrackingViewModel(SavedStateHandle(mapOf("orderId" to 7L)), orders, ads)
    }

    @Test fun loads_the_order_and_the_wait_state_ad_while_it_is_open() = runTest {
        val vm = viewModel(); runCurrent()

        assertEquals(7L, (vm.state.value as UiState.Content).value.id)
        assertEquals(listOf(waitAd), vm.waitAds.value)
    }

    @Test fun a_completed_order_does_not_request_the_wait_state_ad() = runTest {
        val vm = viewModel(OrderStatus.COMPLETED); runCurrent()

        coVerify(exactly = 0) { ads.ads(any()) }
        assertEquals(emptyList<Ad>(), vm.waitAds.value)
    }

    @Test fun a_cancelled_order_does_not_request_the_wait_state_ad() = runTest {
        viewModel(OrderStatus.CANCELLED); runCurrent()

        coVerify(exactly = 0) { ads.ads(any()) }
    }

    @Test fun failing_ads_leave_the_order_visible() = runTest {
        val vm = viewModel()
        coEvery { ads.ads(any()) } returns Result.failure(AppError.Offline)

        runCurrent()

        assertTrue(vm.state.value is UiState.Content)
        assertEquals(emptyList<Ad>(), vm.waitAds.value)
    }

    @Test fun a_load_failure_is_an_error_and_reload_recovers() = runTest {
        val vm = viewModel()
        coEvery { orders.order(7) } returns Result.failure(AppError.Server)
        vm.reload(); runCurrent()
        assertEquals(UiState.Error(AppError.Server), vm.state.value)

        coEvery { orders.order(7) } returns Result.success(testOrder(id = 7))
        vm.reload(); runCurrent()

        assertTrue(vm.state.value is UiState.Content)
    }

    @Test fun refresh_fetches_the_order_again_and_shows_the_new_status() = runTest {
        val vm = viewModel(OrderStatus.SELLER_PROCESSING); runCurrent()
        coEvery { orders.order(7) } returns Result.success(testOrder(id = 7, status = OrderStatus.READY_FOR_PICKUP))

        vm.refresh(); runCurrent()

        assertEquals(OrderStatus.READY_FOR_PICKUP, (vm.state.value as UiState.Content).value.status)
        assertEquals(false, vm.refreshing.value)
    }

    @Test fun a_failed_refresh_keeps_the_current_order_and_says_so() = runTest {
        val vm = viewModel(); runCurrent()
        coEvery { orders.order(7) } returns Result.failure(AppError.Offline)

        vm.events.test {
            vm.refresh(); runCurrent()
            assertTrue(awaitItem() is UiEvent.Message)
        }
        assertTrue(vm.state.value is UiState.Content)
        assertEquals(false, vm.refreshing.value)
    }
}
