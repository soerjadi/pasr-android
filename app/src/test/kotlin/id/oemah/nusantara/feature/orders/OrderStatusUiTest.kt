package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusUiTest {
    @Test fun labels() {
        assertEquals("Pesanan dibuat", OrderStatus.CREATED.label())
        assertEquals("Diproses penjual", OrderStatus.SELLER_PROCESSING.label())
        assertEquals("Siap diambil", OrderStatus.READY_FOR_PICKUP.label())
        assertEquals("Selesai", OrderStatus.COMPLETED.label())
        assertEquals("Dibatalkan", OrderStatus.CANCELLED.label())
    }

    @Test fun only_completed_and_cancelled_are_finished() {
        assertTrue(OrderStatus.COMPLETED.isFinished())
        assertTrue(OrderStatus.CANCELLED.isFinished())
        assertFalse(OrderStatus.CREATED.isFinished())
        assertFalse(OrderStatus.SELLER_PROCESSING.isFinished())
        assertFalse(OrderStatus.READY_FOR_PICKUP.isFinished())
    }
}
