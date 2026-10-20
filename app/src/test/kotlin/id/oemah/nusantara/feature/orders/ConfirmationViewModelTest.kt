package id.oemah.nusantara.feature.orders

import androidx.lifecycle.SavedStateHandle
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.testing.testOrder
import io.mockk.coEvery
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
class ConfirmationViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val orders = mockk<OrdersRepository>()
    private val ads = mockk<AdsRepository>()
    private val ad = Ad(1, AdPlacementKey.POST_PURCHASE, "J&T", image = ImageSet("s", "m", "l"), targetUrl = "https://a.id", label = "Iklan")

    private fun viewModel(): ConfirmationViewModel {
        coEvery { orders.order(7) } returns Result.success(testOrder(id = 7))
        coEvery { ads.ads(AdPlacement.POST_PURCHASE) } returns Result.success(listOf(ad))
        return ConfirmationViewModel(SavedStateHandle(mapOf("orderId" to 7L)), orders, ads)
    }

    @Test fun loads_the_order_and_the_post_purchase_ad() = runTest {
        val vm = viewModel(); runCurrent()

        assertEquals("NM-20261015-007", (vm.state.value as UiState.Content).value.number)
        assertEquals(listOf(ad), vm.postPurchaseAds.value)
    }

    @Test fun failing_ads_leave_the_confirmation_intact() = runTest {
        val vm = viewModel()
        coEvery { ads.ads(any()) } returns Result.failure(AppError.Offline)

        runCurrent()

        assertTrue(vm.state.value is UiState.Content)
        assertEquals(emptyList<Ad>(), vm.postPurchaseAds.value)
    }

    @Test fun a_load_failure_is_an_error_state() = runTest {
        val vm = viewModel()
        coEvery { orders.order(7) } returns Result.failure(AppError.Offline)

        vm.reload(); runCurrent()

        assertEquals(UiState.Error(AppError.Offline), vm.state.value)
    }
}
