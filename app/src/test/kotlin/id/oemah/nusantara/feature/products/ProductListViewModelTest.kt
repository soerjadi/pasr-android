package id.oemah.nusantara.feature.products

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductListViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()
    private val cart = FakeCartRepository()

    private fun route(categoryId: Long?, title: String, featured: Boolean) = SavedStateHandle(
        buildMap {
            categoryId?.let { put("categoryId", it) }
            put("title", title)
            put("featured", featured)
        },
    )

    private fun viewModel(handle: SavedStateHandle): ProductListViewModel {
        every { catalog.products(any()) } returns flowOf(PagingData.empty())
        return ProductListViewModel(handle, catalog, cart)
    }

    @Test fun a_category_list_queries_that_category() = runTest {
        val vm = viewModel(route(5, "Makanan & Minuman", false))
        vm.products // touch the lazily built flow

        verify { catalog.products(ProductQuery(categoryId = 5)) }
        assertEquals("Makanan & Minuman", vm.title)
    }

    @Test fun the_featured_list_queries_featured_products() = runTest {
        val vm = viewModel(route(null, "Produk Pilihan", true))
        vm.products

        verify { catalog.products(ProductQuery(featured = true)) }
    }

    @Test fun adding_a_product_puts_it_in_the_cart() = runTest {
        val vm = viewModel(route(5, "x", false))
        val product = ProductSummary(9, "Madu", 3, "Pak Slamet", 45_000, ImageSet("s", "m", "l"), 4.9, 3, 4, true)

        vm.addToCart(product); advanceUntilIdle()

        assertEquals(listOf(9L), cart.current.map { it.productId })
    }
}
