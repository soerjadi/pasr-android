package id.oemah.nusantara.feature.orders

import androidx.paging.testing.asSnapshot
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.model.OrderPage
import id.oemah.nusantara.testing.testOrder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class OrdersRepositoryTest {
    private val api = mockk<OrdersApi>()
    private val repo = DefaultOrdersRepository(api)

    @Test fun one_order_is_fetched_by_id() = runTest {
        coEvery { api.getOrder(7) } returns Response.success(testOrder(id = 7))

        assertEquals(7L, repo.order(7).getOrThrow().id)
    }

    @Test fun each_history_tab_requests_its_api_value() = runTest {
        val tabs = mutableListOf<OrdersApi.TabListOrders?>()
        coEvery { api.listOrders(captureNullable(tabs), any(), any()) } returns Response.success(OrderPage(emptyList(), null))

        OrderTab.entries.forEach { repo.orders(it).asSnapshot() }

        assertEquals(listOf("ALL", "PROCESSING", "READY", "DONE"), tabs.map { it?.value })
    }

    @Test fun tab_labels_are_the_spec_labels() {
        assertEquals(listOf("Semua", "Diproses", "Siap Diambil", "Selesai"), OrderTab.entries.map { it.label })
    }

    @Test fun order_pages_follow_the_cursor() = runTest {
        coEvery { api.listOrders(any(), isNull(), any()) } returns Response.success(OrderPage(listOf(testOrder(1), testOrder(2)), "c2"))
        coEvery { api.listOrders(any(), "c2", any()) } returns Response.success(OrderPage(listOf(testOrder(3)), null))

        val items = repo.orders(OrderTab.ALL).asSnapshot { scrollTo(index = 2) }

        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }
}
