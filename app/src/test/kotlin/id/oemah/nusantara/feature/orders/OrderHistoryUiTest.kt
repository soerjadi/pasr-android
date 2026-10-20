package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.testing.testOrder
import id.oemah.nusantara.testing.testOrderItem
import id.oemah.nusantara.testing.testSellerOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderHistoryUiTest {
    @Test fun the_history_chip_groups_the_two_processing_statuses() {
        assertEquals("Diproses", OrderStatus.CREATED.historyChip())
        assertEquals("Diproses", OrderStatus.SELLER_PROCESSING.historyChip())
        assertEquals("Siap Diambil", OrderStatus.READY_FOR_PICKUP.historyChip())
        assertEquals("Selesai", OrderStatus.COMPLETED.historyChip())
        assertEquals("Dibatalkan", OrderStatus.CANCELLED.historyChip())
    }

    @Test fun every_tab_has_its_own_empty_message() {
        assertEquals(
            listOf(
                "Belum ada pesanan",
                "Tidak ada pesanan yang sedang diproses",
                "Belum ada pesanan yang siap diambil",
                "Belum ada pesanan selesai",
            ),
            OrderTab.entries.map { it.emptyMessage() },
        )
    }

    @Test fun thumbnails_take_at_most_three_item_images_across_sellers() {
        fun item(id: Long) = testOrderItem(id = id).copy(image = ImageSet("s$id", "m$id", "l$id"))
        val order = testOrder(
            sellerOrders = listOf(
                testSellerOrder(id = 1, items = listOf(item(1), item(2))),
                testSellerOrder(id = 2, items = listOf(item(3), item(4))),
            ),
        )

        assertEquals(listOf("s1", "s2", "s3"), order.thumbnails().map { it.small })
    }

    @Test fun an_order_with_one_item_has_one_thumbnail() {
        assertEquals(1, testOrder().thumbnails().size)
    }
}
