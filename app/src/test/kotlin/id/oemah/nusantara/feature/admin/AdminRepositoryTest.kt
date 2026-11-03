package id.oemah.nusantara.feature.admin

import androidx.paging.testing.asSnapshot
import id.oemah.nusantara.api.AdminApi
import id.oemah.nusantara.api.model.AdCampaign
import id.oemah.nusantara.api.model.AdCampaignInput
import id.oemah.nusantara.api.model.AdCampaignPage
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.AdminOrder
import id.oemah.nusantara.api.model.AdminOrderPage
import id.oemah.nusantara.api.model.AdminProduct
import id.oemah.nusantara.api.model.AdminProductPatch
import id.oemah.nusantara.api.model.AdminStore
import id.oemah.nusantara.api.model.AdminStorePage
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.CategoryInput
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.ReadyRequest
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.testing.testOrderItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AdminRepositoryTest {
    private val api = mockk<AdminApi>()
    private val repo = DefaultAdminRepository(api)

    private fun sampleAdminStore(id: Long = 1, status: StoreStatus = StoreStatus.PENDING) = AdminStore(
        id = id,
        ownerId = 10,
        name = "Toko $id",
        ownerName = "Pemilik $id",
        categoryId = 2,
        kelurahan = "Simo",
        address = "Jl. Merdeka 1",
        whatsapp = "+6281234567890",
        status = status,
        isOpen = true,
        createdAt = "2026-10-15T00:00:00Z",
    )

    private fun sampleAdminProduct(id: Long = 1, featured: Boolean = false) = AdminProduct(
        id = id,
        storeId = 5,
        name = "Produk $id",
        price = 10_000,
        stock = 20,
        status = AdminProduct.Status.AVAILABLE,
        featured = featured,
        halalVerified = true,
        halalCertNo = "ID123456789",
    )

    private fun sampleCategory(id: Long = 1, name: String = "Makanan") = Category(
        id = id,
        name = name,
        iconKey = "food",
    )

    private fun sampleAdCampaign(id: Long = 1, title: String = "Promo $id") = AdCampaign(
        id = id,
        placement = AdPlacementKey.HOME_PARTNER_CAROUSEL,
        advertiser = "Partner",
        title = title,
        subtitle = "Sub",
        image = ImageSet("s", "m", "l"),
        targetUrl = "https://example.com",
        label = "Iklan",
        priority = 1,
        active = true,
        startsAt = null,
        endsAt = null,
    )

    private fun sampleAdminOrder(id: Long = 1, status: OrderStatus = OrderStatus.CREATED) = AdminOrder(
        id = id,
        number = "NM-20261015-%03d".format(id),
        status = status,
        createdAt = "2026-10-15T03:02:00Z",
        buyerName = "Buyer $id",
        subtotal = 25_000,
        items = listOf(testOrderItem(id = id)),
        statusHistory = listOf(StatusChange(status, "2026-10-15T03:02:00Z")),
        storeId = 7,
        storeName = "Store 7",
    )

    private fun sampleStoreOrder(id: Long = 1, status: OrderStatus = OrderStatus.CREATED, note: String? = null) = StoreOrder(
        id = id,
        number = "NM-20261015-%03d".format(id),
        status = status,
        createdAt = "2026-10-15T03:02:00Z",
        buyerName = "Buyer $id",
        subtotal = 25_000,
        items = listOf(testOrderItem(id = id)),
        statusHistory = listOf(StatusChange(status, "2026-10-15T03:02:00Z")),
        note = note,
    )

    // 1. Stores: listStores & stores (Paging)
    @Test
    fun listStores_calls_api_and_returns_page() = runTest {
        val expectedPage = AdminStorePage(items = listOf(sampleAdminStore(1)), nextCursor = "c2")
        coEvery { api.listAdminStores(StoreStatus.PENDING, "c1", 20) } returns Response.success(expectedPage)

        val result = repo.listStores(status = StoreStatus.PENDING, cursor = "c1", limit = 20)

        assertTrue(result.isSuccess)
        assertEquals(expectedPage, result.getOrThrow())
        coVerify { api.listAdminStores(StoreStatus.PENDING, "c1", 20) }
    }

    @Test
    fun stores_paging_flow_emits_pages_via_snapshot() = runTest {
        coEvery { api.listAdminStores(StoreStatus.VERIFIED, null, any()) } returns
            Response.success(AdminStorePage(items = listOf(sampleAdminStore(1), sampleAdminStore(2)), nextCursor = "c2"))
        coEvery { api.listAdminStores(StoreStatus.VERIFIED, "c2", any()) } returns
            Response.success(AdminStorePage(items = listOf(sampleAdminStore(3)), nextCursor = null))

        val items = repo.stores(StoreStatus.VERIFIED).asSnapshot { scrollTo(index = 2) }

        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    // 2. verifyStore
    @Test
    fun verifyStore_calls_api_and_returns_updated_store() = runTest {
        val store = sampleAdminStore(1, StoreStatus.VERIFIED)
        coEvery { api.verifyAdminStore(1L) } returns Response.success(store)

        val result = repo.verifyStore(1L)

        assertTrue(result.isSuccess)
        assertEquals(store, result.getOrThrow())
        coVerify { api.verifyAdminStore(1L) }
    }

    // 3. suspendStore
    @Test
    fun suspendStore_calls_api_and_returns_updated_store() = runTest {
        val store = sampleAdminStore(1, StoreStatus.SUSPENDED)
        coEvery { api.suspendAdminStore(1L) } returns Response.success(store)

        val result = repo.suspendStore(1L)

        assertTrue(result.isSuccess)
        assertEquals(store, result.getOrThrow())
        coVerify { api.suspendAdminStore(1L) }
    }

    // 4. reinstateStore
    @Test
    fun reinstateStore_calls_api_and_returns_updated_store() = runTest {
        val store = sampleAdminStore(1, StoreStatus.VERIFIED)
        coEvery { api.reinstateAdminStore(1L) } returns Response.success(store)

        val result = repo.reinstateStore(1L)

        assertTrue(result.isSuccess)
        assertEquals(store, result.getOrThrow())
        coVerify { api.reinstateAdminStore(1L) }
    }

    // 5. moderateProduct
    @Test
    fun moderateProduct_sends_patch_and_returns_product() = runTest {
        val patch = AdminProductPatch(featured = true, halalVerified = true)
        val product = sampleAdminProduct(5, featured = true)
        coEvery { api.updateAdminProduct(5L, patch) } returns Response.success(product)

        val result = repo.moderateProduct(5L, patch)

        assertTrue(result.isSuccess)
        assertEquals(product, result.getOrThrow())
        coVerify { api.updateAdminProduct(5L, patch) }
    }

    // 6. createCategory
    @Test
    fun createCategory_sends_input_and_returns_category() = runTest {
        val input = CategoryInput(name = "Kerajinan", iconKey = "craft")
        val category = sampleCategory(10, name = "Kerajinan")
        coEvery { api.createAdminCategory(input) } returns Response.success(category)

        val result = repo.createCategory(input)

        assertTrue(result.isSuccess)
        assertEquals(category, result.getOrThrow())
        coVerify { api.createAdminCategory(input) }
    }

    // 7. updateCategory
    @Test
    fun updateCategory_sends_id_and_input_and_returns_category() = runTest {
        val input = CategoryInput(name = "Kuliner", iconKey = "food")
        val category = sampleCategory(2, name = "Kuliner")
        coEvery { api.updateAdminCategory(2L, input) } returns Response.success(category)

        val result = repo.updateCategory(2L, input)

        assertTrue(result.isSuccess)
        assertEquals(category, result.getOrThrow())
        coVerify { api.updateAdminCategory(2L, input) }
    }

    // 8. deleteCategory
    @Test
    fun deleteCategory_calls_api_and_returns_unit() = runTest {
        coEvery { api.deleteAdminCategory(3L) } returns Response.success(Unit)

        val result = repo.deleteCategory(3L)

        assertTrue(result.isSuccess)
        coVerify { api.deleteAdminCategory(3L) }
    }

    // 9. Ad campaigns: listAdCampaigns & adCampaigns (Paging)
    @Test
    fun listAdCampaigns_calls_api_and_returns_page() = runTest {
        val expectedPage = AdCampaignPage(items = listOf(sampleAdCampaign(1)), nextCursor = "next")
        coEvery { api.listAdCampaigns("cur", 20) } returns Response.success(expectedPage)

        val result = repo.listAdCampaigns(cursor = "cur", limit = 20)

        assertTrue(result.isSuccess)
        assertEquals(expectedPage, result.getOrThrow())
        coVerify { api.listAdCampaigns("cur", 20) }
    }

    @Test
    fun adCampaigns_paging_flow_emits_pages_via_snapshot() = runTest {
        coEvery { api.listAdCampaigns(null, any()) } returns
            Response.success(AdCampaignPage(items = listOf(sampleAdCampaign(1), sampleAdCampaign(2)), nextCursor = "c2"))
        coEvery { api.listAdCampaigns("c2", any()) } returns
            Response.success(AdCampaignPage(items = listOf(sampleAdCampaign(3)), nextCursor = null))

        val items = repo.adCampaigns().asSnapshot { scrollTo(index = 2) }

        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    // 10. createAdCampaign
    @Test
    fun createAdCampaign_sends_input_and_returns_campaign() = runTest {
        val input = AdCampaignInput(
            placement = AdPlacementKey.HOME_PARTNER_CAROUSEL,
            advertiser = "Partner",
            title = "Promo Baru",
            image = ImageSet("s", "m", "l"),
            targetUrl = "https://promo.id",
            label = "Iklan",
        )
        val campaign = sampleAdCampaign(10, title = "Promo Baru")
        coEvery { api.createAdCampaign(input) } returns Response.success(campaign)

        val result = repo.createAdCampaign(input)

        assertTrue(result.isSuccess)
        assertEquals(campaign, result.getOrThrow())
        coVerify { api.createAdCampaign(input) }
    }

    // 11. updateAdCampaign
    @Test
    fun updateAdCampaign_sends_id_and_input_and_returns_campaign() = runTest {
        val input = AdCampaignInput(title = "Promo Update")
        val campaign = sampleAdCampaign(10, title = "Promo Update")
        coEvery { api.updateAdCampaign(10L, input) } returns Response.success(campaign)

        val result = repo.updateAdCampaign(10L, input)

        assertTrue(result.isSuccess)
        assertEquals(campaign, result.getOrThrow())
        coVerify { api.updateAdCampaign(10L, input) }
    }

    // 12. deleteAdCampaign
    @Test
    fun deleteAdCampaign_calls_api_and_returns_unit() = runTest {
        coEvery { api.deleteAdCampaign(7L) } returns Response.success(Unit)

        val result = repo.deleteAdCampaign(7L)

        assertTrue(result.isSuccess)
        coVerify { api.deleteAdCampaign(7L) }
    }

    // 13. Orders: listOrders & orders (Paging)
    @Test
    fun listOrders_calls_api_and_returns_page() = runTest {
        val expectedPage = AdminOrderPage(items = listOf(sampleAdminOrder(1)), nextCursor = "c2")
        coEvery { api.listAdminOrders(OrderStatus.CREATED, "c1", 20) } returns Response.success(expectedPage)

        val result = repo.listOrders(status = OrderStatus.CREATED, cursor = "c1", limit = 20)

        assertTrue(result.isSuccess)
        assertEquals(expectedPage, result.getOrThrow())
        coVerify { api.listAdminOrders(OrderStatus.CREATED, "c1", 20) }
    }

    @Test
    fun orders_paging_flow_emits_pages_via_snapshot() = runTest {
        coEvery { api.listAdminOrders(OrderStatus.SELLER_PROCESSING, null, any()) } returns
            Response.success(AdminOrderPage(items = listOf(sampleAdminOrder(1), sampleAdminOrder(2)), nextCursor = "c2"))
        coEvery { api.listAdminOrders(OrderStatus.SELLER_PROCESSING, "c2", any()) } returns
            Response.success(AdminOrderPage(items = listOf(sampleAdminOrder(3)), nextCursor = null))

        val items = repo.orders(OrderStatus.SELLER_PROCESSING).asSnapshot { scrollTo(index = 2) }

        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    // 14. acceptSellerOrder
    @Test
    fun acceptSellerOrder_calls_api_and_returns_store_order() = runTest {
        val order = sampleStoreOrder(1, OrderStatus.SELLER_PROCESSING)
        coEvery { api.acceptAdminSellerOrder(1L) } returns Response.success(order)

        val result = repo.acceptSellerOrder(1L)

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify { api.acceptAdminSellerOrder(1L) }
    }

    // 15. rejectSellerOrder
    @Test
    fun rejectSellerOrder_sends_reason_and_returns_store_order() = runTest {
        val order = sampleStoreOrder(1, OrderStatus.CANCELLED)
        coEvery {
            api.rejectAdminSellerOrder(
                1L,
                RejectRequest(RejectRequest.Reason.OUT_OF_STOCK, "Stok habis"),
            )
        } returns Response.success(order)

        val result = repo.rejectSellerOrder(1L, RejectRequest.Reason.OUT_OF_STOCK, "Stok habis")

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify {
            api.rejectAdminSellerOrder(
                1L,
                RejectRequest(RejectRequest.Reason.OUT_OF_STOCK, "Stok habis"),
            )
        }
    }

    // 16. readySellerOrder
    @Test
    fun readySellerOrder_sends_note_and_returns_store_order() = runTest {
        val order = sampleStoreOrder(1, OrderStatus.READY_FOR_PICKUP, note = "Siap di meja depan")
        coEvery {
            api.readyAdminSellerOrder(1L, ReadyRequest("Siap di meja depan"))
        } returns Response.success(order)

        val result = repo.readySellerOrder(1L, "Siap di meja depan")

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify {
            api.readyAdminSellerOrder(1L, ReadyRequest("Siap di meja depan"))
        }
    }

    // 17. completeSellerOrder
    @Test
    fun completeSellerOrder_calls_api_and_returns_store_order() = runTest {
        val order = sampleStoreOrder(1, OrderStatus.COMPLETED)
        coEvery { api.completeAdminSellerOrder(1L) } returns Response.success(order)

        val result = repo.completeSellerOrder(1L)

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify { api.completeAdminSellerOrder(1L) }
    }

    // 18. cancelSellerOrder
    @Test
    fun cancelSellerOrder_sends_reason_and_returns_store_order() = runTest {
        val order = sampleStoreOrder(1, OrderStatus.CANCELLED)
        coEvery {
            api.cancelAdminSellerOrder(
                1L,
                RejectRequest(RejectRequest.Reason.BUSY_OR_CLOSED, "Toko tutup sementara"),
            )
        } returns Response.success(order)

        val result = repo.cancelSellerOrder(1L, RejectRequest.Reason.BUSY_OR_CLOSED, "Toko tutup sementara")

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrThrow())
        coVerify {
            api.cancelAdminSellerOrder(
                1L,
                RejectRequest(RejectRequest.Reason.BUSY_OR_CLOSED, "Toko tutup sementara"),
            )
        }
    }

    // 19. deleteReview
    @Test
    fun deleteReview_calls_api_and_returns_unit() = runTest {
        coEvery { api.deleteAdminReview(8L) } returns Response.success(Unit)

        val result = repo.deleteReview(8L)

        assertTrue(result.isSuccess)
        coVerify { api.deleteAdminReview(8L) }
    }

    // Error handling
    @Test
    fun error_response_returns_failure_result() = runTest {
        coEvery { api.verifyAdminStore(999L) } returns Response.error(404, """{"code":"STORE_NOT_FOUND","message":"Not found"}""".toResponseBody())

        val result = repo.verifyStore(999L)

        assertTrue(result.isFailure)
    }
}
