package id.oemah.nusantara.feature.catalog

import androidx.paging.testing.asSnapshot
import id.oemah.nusantara.api.CatalogApi
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.CategoryList
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductPage
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.api.model.ReviewPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class CatalogRepositoryTest {
    private val api = mockk<CatalogApi>()
    private val repo = DefaultCatalogRepository(api)

    private fun product(id: Long) = ProductSummary(
        id = id, name = "P$id", sellerId = 1, sellerName = "S", price = 1_000, image = ImageSet("s", "m", "l"),
        rating = 4.5, ratingCount = 1, stock = 5, halalVerified = false,
    )

    @Test fun product_pages_follow_the_next_cursor_until_it_is_null() = runTest {
        coEvery { api.listProducts(any(), any(), any(), any(), isNull(), any()) } returns
            Response.success(ProductPage(items = listOf(product(1), product(2)), nextCursor = "c2"))
        coEvery { api.listProducts(any(), any(), any(), any(), "c2", any()) } returns
            Response.success(ProductPage(items = listOf(product(3)), nextCursor = null))

        val items = repo.products(ProductQuery()).asSnapshot { scrollTo(index = 2) }

        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    @Test fun padded_queries_are_trimmed_and_blank_queries_are_omitted() = runTest {
        val queries = mutableListOf<String?>()
        coEvery { api.listProducts(any(), captureNullable(queries), any(), any(), any(), any()) } returns
            Response.success(ProductPage(items = emptyList(), nextCursor = null))

        repo.products(ProductQuery(query = "  tempe ")).asSnapshot()
        repo.products(ProductQuery(query = "   ")).asSnapshot()

        assertEquals(listOf("tempe", null), queries)
    }

    @Test fun filters_are_sent_and_featured_is_only_sent_when_true() = runTest {
        data class Call(val categoryId: Long?, val featured: Boolean?, val sellerId: Long?)
        val calls = mutableListOf<Call>()
        coEvery { api.listProducts(any(), any(), any(), any(), any(), any()) } answers {
            calls += Call(categoryId = firstArg(), featured = thirdArg(), sellerId = arg(3))
            Response.success(ProductPage(items = emptyList(), nextCursor = null))
        }

        repo.products(ProductQuery(categoryId = 5, sellerId = 9)).asSnapshot()
        repo.products(ProductQuery(featured = true)).asSnapshot()

        assertEquals(listOf(Call(5, null, 9), Call(null, true, null)), calls)
    }

    @Test fun every_request_stays_within_the_api_limit_of_50() = runTest {
        val limits = mutableListOf<Int?>()
        coEvery { api.listProducts(any(), any(), any(), any(), any(), captureNullable(limits)) } returns
            Response.success(ProductPage(items = emptyList(), nextCursor = null))

        repo.products(ProductQuery()).asSnapshot()

        assertEquals(listOf(20), limits)
    }

    @Test fun featured_products_are_the_first_page_of_featured_items_only() = runTest {
        data class Call(val featured: Boolean?, val cursor: String?, val limit: Int?)
        val calls = mutableListOf<Call>()
        coEvery { api.listProducts(any(), any(), any(), any(), any(), any()) } answers {
            calls += Call(featured = thirdArg(), cursor = arg(4), limit = arg(5))
            Response.success(ProductPage(items = listOf(product(1), product(2)), nextCursor = "more"))
        }

        val items = repo.featuredProducts().getOrThrow()

        assertEquals(listOf(1L, 2L), items.map { it.id })
        assertEquals(listOf(Call(featured = true, cursor = null, limit = 8)), calls)
    }

    @Test fun categories_are_unwrapped_from_the_list() = runTest {
        coEvery { api.listCategories() } returns Response.success(CategoryList(listOf(Category(1, "Makanan & Minuman", "food"))))

        assertEquals(listOf("Makanan & Minuman"), repo.categories().getOrThrow().map { it.name })
    }

    @Test fun seller_reviews_page_by_cursor() = runTest {
        val review = Review(id = 1, rating = 5, authorName = "A", createdAt = "2026-10-10T08:00:00Z")
        coEvery { api.listSellerReviews(7, isNull(), any()) } returns Response.success(ReviewPage(listOf(review), nextCursor = null))

        val items = repo.sellerReviews(7).asSnapshot()

        assertEquals(listOf(1L), items.map { it.id })
    }
}
