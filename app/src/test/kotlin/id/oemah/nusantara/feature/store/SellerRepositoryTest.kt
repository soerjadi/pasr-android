package id.oemah.nusantara.feature.store

import androidx.paging.testing.asSnapshot
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ImageUpload
import id.oemah.nusantara.api.model.MyStore
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.ReadyRequest
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.SellerProductPage
import id.oemah.nusantara.api.model.SellerProductPageCounts
import id.oemah.nusantara.api.model.SetOpenRequest
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreOrderPage
import id.oemah.nusantara.api.model.StoreOrderPageCounts
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.api.model.UpdateStoreRequest
import id.oemah.nusantara.testing.testOrderItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SellerRepositoryTest {
    private val api = mockk<MeApi>()
    private val repo = DefaultSellerRepository(api)

    private fun sampleStoreOrder(id: Long = 1, status: OrderStatus = OrderStatus.CREATED, note: String? = null) = StoreOrder(
        id = id,
        number = "NM-20261015-%03d".format(id),
        status = status,
        createdAt = "2026-10-15T03:02:00Z",
        buyerName = "Budi",
        subtotal = 15_000,
        items = listOf(testOrderItem()),
        statusHistory = listOf(StatusChange(status, "2026-10-15T03:02:00Z")),
        note = note,
    )

    private fun sampleSellerProduct(id: Long = 1, name: String = "Product $id") = SellerProduct(
        id = id,
        name = name,
        price = 10_000,
        stock = 5,
        description = "Tasty",
        status = SellerProduct.Status.AVAILABLE,
        images = listOf(ImageSet("s", "m", "l")),
    )

    @Test fun create_store_posts_request() = runTest {
        val request = CreateStoreRequest("Warung Bu Sari", 3, "Sari", "Sambirejo", "RT 2")
        val summary = StoreSummary(7, "Warung Bu Sari", StoreStatus.PENDING, 0)
        coEvery { api.createMyStore(request) } returns Response.success(summary)

        assertEquals(summary, repo.createStore(request).getOrThrow())
    }

    @Test fun get_my_store_fetches_profile() = runTest {
        val store = MyStore(
            id = 7,
            name = "Warung Bu Sari",
            categoryId = 3,
            ownerName = "Sari",
            kelurahan = "Sambirejo",
            address = "RT 2",
            description = "Desc",
            status = StoreStatus.VERIFIED,
            isOpen = true,
            whatsapp = "+6281234567890",
        )
        coEvery { api.getMyStore() } returns Response.success(store)

        assertEquals(store, repo.getMyStore().getOrThrow())
    }

    @Test fun update_my_store_patches_profile() = runTest {
        val request = UpdateStoreRequest(name = "Warung Baru")
        val updated = MyStore(
            id = 7,
            name = "Warung Baru",
            categoryId = 3,
            ownerName = "Sari",
            kelurahan = "Sambirejo",
            address = "RT 2",
            description = "Desc",
            status = StoreStatus.VERIFIED,
            isOpen = true,
            whatsapp = "+6281234567890",
        )
        coEvery { api.updateMyStore(request) } returns Response.success(updated)

        assertEquals(updated, repo.updateMyStore(request).getOrThrow())
    }

    @Test fun set_open_puts_flag() = runTest {
        coEvery { api.setMyStoreOpen(SetOpenRequest(true)) } returns Response.success(Unit)

        assertTrue(repo.setMyStoreOpen(true).isSuccess)
        coVerify(exactly = 1) { api.setMyStoreOpen(SetOpenRequest(true)) }
    }

    @Test fun upload_image_posts_multipart() = runTest {
        val upload = ImageUpload(123, ImageSet("s", "m", "l"))
        coEvery { api.uploadImage(any()) } returns Response.success(upload)

        val result = repo.uploadImage("fake-bytes".toByteArray(), "pic.png", "image/png").getOrThrow()
        assertEquals(123L, result.id)
        coVerify(exactly = 1) { api.uploadImage(any()) }
    }

    @Test fun create_product_posts_body() = runTest {
        val request = CreateProductRequest("Nasi Goreng", 20_000, 15)
        val product = sampleSellerProduct(99, "Nasi Goreng")
        coEvery { api.createMyProduct(request) } returns Response.success(product)

        assertEquals(product, repo.createProduct(request).getOrThrow())
    }

    @Test fun my_products_pages_follow_cursor() = runTest {
        coEvery { api.listMyProducts(status = null, cursor = null, limit = any()) } returns Response.success(
            SellerProductPage(
                items = listOf(sampleSellerProduct(1), sampleSellerProduct(2)),
                nextCursor = "cur2",
                counts = SellerProductPageCounts(all = 2, available = 2, outOfStock = 0, hidden = 0),
            )
        )
        coEvery { api.listMyProducts(status = null, cursor = "cur2", limit = any()) } returns Response.success(
            SellerProductPage(
                items = listOf(sampleSellerProduct(3)),
                nextCursor = null,
                counts = SellerProductPageCounts(all = 3, available = 3, outOfStock = 0, hidden = 0),
            )
        )

        val items = repo.myProducts().asSnapshot { scrollTo(index = 2) }
        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    @Test fun get_my_product_by_id() = runTest {
        val product = sampleSellerProduct(42)
        coEvery { api.getMyProduct(42) } returns Response.success(product)

        assertEquals(product, repo.getMyProduct(42).getOrThrow())
    }

    @Test fun update_my_product_patches_fields() = runTest {
        val request = UpdateProductRequest(price = 25_000)
        val product = sampleSellerProduct(42)
        coEvery { api.updateMyProduct(42, request) } returns Response.success(product)

        assertEquals(product, repo.updateMyProduct(42, request).getOrThrow())
    }

    @Test fun delete_my_product_deletes() = runTest {
        coEvery { api.deleteMyProduct(42) } returns Response.success(Unit)

        assertTrue(repo.deleteMyProduct(42).isSuccess)
    }

    @Test fun get_store_summary_returns_figures() = runTest {
        val summary = StoreSummaryResponse(
            isOpen = true,
            closesAt = "17:00",
            newOrderCount = 2,
            acceptBy = "2026-10-15T03:30:00Z",
            ordersToday = 5,
            salesToday = 120_000,
            latestOrders = listOf(sampleStoreOrder(1)),
        )
        coEvery { api.getMyStoreSummary() } returns Response.success(summary)

        assertEquals(summary, repo.getMyStoreSummary().getOrThrow())
    }

    @Test fun get_store_stats_passes_range() = runTest {
        val stats = StoreStats(
            range = StoreStats.Range.WEEK,
            salesValue = 500_000,
            completedOrders = 20,
            daily = emptyList(),
            topProducts = emptyList(),
        )
        coEvery { api.getMyStoreStats(MeApi.RangeGetMyStoreStats.WEEK) } returns Response.success(stats)

        assertEquals(stats, repo.getMyStoreStats(MeApi.RangeGetMyStoreStats.WEEK).getOrThrow())
    }

    @Test fun my_store_orders_pages_follow_cursor() = runTest {
        coEvery { api.listMyStoreOrders(tab = MeApi.TabListMyStoreOrders.NEW, cursor = null, limit = any()) } returns Response.success(
            StoreOrderPage(
                items = listOf(sampleStoreOrder(1), sampleStoreOrder(2)),
                nextCursor = "cur-order",
                counts = StoreOrderPageCounts(new = 2, processing = 0, done = 0),
            )
        )
        coEvery { api.listMyStoreOrders(tab = MeApi.TabListMyStoreOrders.NEW, cursor = "cur-order", limit = any()) } returns Response.success(
            StoreOrderPage(
                items = listOf(sampleStoreOrder(3)),
                nextCursor = null,
                counts = StoreOrderPageCounts(new = 3, processing = 0, done = 0),
            )
        )

        val items = repo.myStoreOrders(MeApi.TabListMyStoreOrders.NEW).asSnapshot { scrollTo(index = 2) }
        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    @Test fun get_my_store_order_by_id() = runTest {
        val order = sampleStoreOrder(10)
        coEvery { api.getMyStoreOrder(10) } returns Response.success(order)

        assertEquals(order, repo.getMyStoreOrder(10).getOrThrow())
    }

    @Test fun accept_order_moves_status() = runTest {
        val accepted = sampleStoreOrder(10, status = OrderStatus.SELLER_PROCESSING)
        coEvery { api.acceptMyStoreOrder(10) } returns Response.success(accepted)

        assertEquals(OrderStatus.SELLER_PROCESSING, repo.acceptMyStoreOrder(10).getOrThrow().status)
    }

    @Test fun reject_order_with_reason() = runTest {
        val rejected = sampleStoreOrder(10, status = OrderStatus.CANCELLED)
        val expectedReq = RejectRequest(RejectRequest.Reason.OUT_OF_STOCK, "Stok habis")
        coEvery { api.rejectMyStoreOrder(10, expectedReq) } returns Response.success(rejected)

        assertEquals(OrderStatus.CANCELLED, repo.rejectMyStoreOrder(10, RejectRequest.Reason.OUT_OF_STOCK, "Stok habis").getOrThrow().status)
    }

    @Test fun ready_order_sets_pickup_note() = runTest {
        val ready = sampleStoreOrder(10, status = OrderStatus.READY_FOR_PICKUP, note = "Di meja 2")
        val expectedReq = ReadyRequest(note = "Di meja 2")
        coEvery { api.readyMyStoreOrder(10, expectedReq) } returns Response.success(ready)

        val result = repo.readyMyStoreOrder(10, "Di meja 2").getOrThrow()
        assertEquals(OrderStatus.READY_FOR_PICKUP, result.status)
        assertEquals("Di meja 2", result.note)
    }

    @Test fun complete_order_marks_completed() = runTest {
        val completed = sampleStoreOrder(10, status = OrderStatus.COMPLETED)
        coEvery { api.completeMyStoreOrder(10) } returns Response.success(completed)

        assertEquals(OrderStatus.COMPLETED, repo.completeMyStoreOrder(10).getOrThrow().status)
    }
}
