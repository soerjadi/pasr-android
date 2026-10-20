package id.oemah.nusantara.feature.products.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.social.SocialRepository
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()
    private val social = mockk<SocialRepository>()
    private val cart = FakeCartRepository()

    private fun viewModel(
        detail: Result<id.oemah.nusantara.api.model.ProductDetail> = Result.success(testDetail()),
        loggedIn: Boolean = true,
    ): ProductDetailViewModel {
        coEvery { catalog.product(7) } returns detail
        return ProductDetailViewModel(
            SavedStateHandle(mapOf("productId" to 7L)), catalog, social, cart,
            FakeSessionStore(if (loggedIn) session() else null),
        )
    }

    @Test fun loads_the_product() = runTest {
        val vm = viewModel(); runCurrent()

        assertEquals("Keripik Tempe", (vm.state.value as UiState.Content).value.product.name)
    }

    @Test fun a_load_failure_is_an_error_and_retry_loads_again() = runTest {
        val vm = viewModel(Result.failure(AppError.Offline)); runCurrent()
        assertEquals(UiState.Error(AppError.Offline), vm.state.value)

        coEvery { catalog.product(7) } returns Result.success(testDetail())
        vm.reload(); runCurrent()

        assertTrue(vm.state.value is UiState.Content)
    }

    @Test fun quantity_is_clamped_between_one_and_the_available_stock() = runTest {
        val vm = viewModel(Result.success(testDetail(stock = 5))); runCurrent()

        vm.setQuantity(0); assertEquals(1, vm.quantity.value)
        vm.setQuantity(3); assertEquals(3, vm.quantity.value)
        vm.setQuantity(9); assertEquals(5, vm.quantity.value)
    }

    @Test fun quantity_never_exceeds_99_even_with_huge_stock() = runTest {
        val vm = viewModel(Result.success(testDetail(stock = 500))); runCurrent()

        vm.setQuantity(150)

        assertEquals(99, vm.quantity.value)
    }

    @Test fun a_sold_out_product_cannot_be_added_and_says_so() = runTest {
        val vm = viewModel(Result.success(testDetail(stock = 0))); runCurrent()

        vm.events.test {
            vm.addToCart(); runCurrent()
            assertEquals(UiEvent.Message("Stok habis"), awaitItem())
        }
        assertEquals(emptyList<Any>(), cart.current)
    }

    @Test fun adding_stores_a_snapshot_with_the_chosen_quantity_seller_and_price() = runTest {
        val vm = viewModel(); runCurrent()
        vm.setQuantity(3)

        vm.events.test {
            vm.addToCart(); runCurrent()
            assertEquals(UiEvent.Message("Ditambahkan ke keranjang"), awaitItem())
        }

        val line = cart.current.single()
        assertEquals(7L, line.productId)
        assertEquals(3L, line.sellerId)
        assertEquals("Bu Siti", line.sellerName)
        assertEquals(Rupiah(15_000), line.unitPrice)
        assertEquals("https://img/s.png", line.imageUrl)
        assertEquals(3, line.quantity)
    }

    @Test fun favoriting_while_logged_out_asks_for_login_and_calls_nothing() = runTest {
        val vm = viewModel(loggedIn = false); runCurrent()

        vm.events.test {
            vm.toggleFavorite(); runCurrent()
            assertEquals(UiEvent.LoginRequired, awaitItem())
        }
        coVerify(exactly = 0) { social.setFavorite(any(), any()) }
        assertFalse(vm.favorite.value)
    }

    @Test fun favoriting_flips_immediately_and_calls_the_api() = runTest {
        coEvery { social.setFavorite(7, true) } returns Result.success(Unit)
        val vm = viewModel(); runCurrent()

        vm.toggleFavorite()
        assertTrue(vm.favorite.value)
        runCurrent()

        coVerify(exactly = 1) { social.setFavorite(7, true) }
        assertTrue(vm.favorite.value)
    }

    @Test fun a_failed_favorite_call_rolls_back_and_tells_the_user() = runTest {
        coEvery { social.setFavorite(7, true) } returns Result.failure(AppError.Offline)
        val vm = viewModel(); runCurrent()

        vm.events.test {
            vm.toggleFavorite(); runCurrent()
            assertEquals(
                UiEvent.Message("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi."),
                awaitItem(),
            )
        }
        assertFalse(vm.favorite.value)
    }

    @Test fun an_already_favorite_product_starts_as_favorite() = runTest {
        val vm = viewModel(Result.success(testDetail(favorite = true))); runCurrent()

        assertTrue(vm.favorite.value)
    }
}
