package id.oemah.nusantara.feature.home

import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()
    private val ads = mockk<AdsRepository>()
    private val cart = FakeCartRepository()

    private val images = ImageSet("https://img/s.png", "https://img/m.png", "https://img/l.png")
    private fun category(id: Long) = Category(id, "Kategori $id", "food")
    private fun product(id: Long, stock: Int = 5) = ProductSummary(
        id = id, name = "Produk $id", sellerId = 7, sellerName = "Bu Siti", price = 15_000, image = images,
        rating = 4.8, ratingCount = 12, stock = stock, halalVerified = true,
    )
    private fun ad(id: Long) = Ad(id, AdPlacementKey.HOME_PARTNER_CAROUSEL, "Iklan $id", image = images, targetUrl = "https://a.id", label = "Kemitraan")

    private fun stubAll() {
        coEvery { catalog.categories() } returns Result.success((1L..6L).map(::category))
        coEvery { catalog.featuredProducts() } returns Result.success(listOf(product(1), product(2)))
        coEvery { ads.ads(AdPlacement.HOME_PARTNER_CAROUSEL) } returns Result.success(listOf(ad(1)))
        coEvery { ads.ads(AdPlacement.HOME_SPONSORED_CARD) } returns Result.success(listOf(ad(2)))
    }

    private fun viewModel() = HomeViewModel(catalog, ads, cart)

    @Test fun every_section_loads() = runTest {
        stubAll()

        val vm = viewModel(); advanceUntilIdle()

        val state = vm.state.value
        assertEquals(6, (state.categories as UiState.Content).value.size)
        assertEquals(listOf(1L, 2L), (state.featured as UiState.Content).value.map { it.id })
        assertEquals(listOf(1L), state.partnerAds.map { it.id })
        assertEquals(listOf(2L), state.sponsoredAds.map { it.id })
    }

    @Test fun a_failing_categories_call_leaves_featured_loaded() = runTest {
        stubAll()
        coEvery { catalog.categories() } returns Result.failure(AppError.Server)

        val vm = viewModel(); advanceUntilIdle()

        assertEquals(UiState.Error(AppError.Server), vm.state.value.categories)
        assertTrue(vm.state.value.featured is UiState.Content)
    }

    @Test fun failing_ads_still_load_the_content() = runTest {
        stubAll()
        coEvery { ads.ads(any()) } returns Result.failure(AppError.Offline)

        val vm = viewModel(); advanceUntilIdle()

        assertTrue(vm.state.value.categories is UiState.Content)
        assertTrue(vm.state.value.featured is UiState.Content)
        assertEquals(emptyList<Ad>(), vm.state.value.partnerAds)
        assertEquals(emptyList<Ad>(), vm.state.value.sponsoredAds)
    }

    @Test fun an_empty_catalogue_is_content_not_an_error() = runTest {
        stubAll()
        coEvery { catalog.featuredProducts() } returns Result.success(emptyList())

        val vm = viewModel(); advanceUntilIdle()

        assertEquals(UiState.Content(emptyList<ProductSummary>()), vm.state.value.featured)
    }

    @Test fun shortcuts_are_the_first_four_categories() = runTest {
        stubAll()

        val vm = viewModel(); advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L, 4L), vm.state.value.categoryShortcuts.map { it.id })
    }

    @Test fun adding_to_the_cart_stores_a_snapshot_with_price_and_small_image() = runTest {
        stubAll()
        val vm = viewModel(); advanceUntilIdle()

        vm.addToCart(product(3))
        advanceUntilIdle()

        val line = cart.current.single()
        assertEquals(3L, line.productId)
        assertEquals(7L, line.sellerId)
        assertEquals("Bu Siti", line.sellerName)
        assertEquals("Produk 3", line.name)
        assertEquals(Rupiah(15_000), line.unitPrice)
        assertEquals("https://img/s.png", line.imageUrl)
    }

    @Test fun a_sold_out_product_is_not_added() = runTest {
        stubAll()
        val vm = viewModel(); advanceUntilIdle()

        vm.addToCart(product(4, stock = 0))
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), cart.current)
    }

    @Test fun reload_fetches_everything_again() = runTest {
        stubAll()
        val vm = viewModel(); advanceUntilIdle()

        vm.reload(); advanceUntilIdle()

        coVerify(exactly = 2) { catalog.categories() }
        coVerify(exactly = 2) { catalog.featuredProducts() }
    }
}
