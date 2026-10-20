package id.oemah.nusantara.push

import id.oemah.nusantara.ui.nav.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRoutesTest {
    @Test fun an_order_update_opens_that_orders_tracking() {
        assertEquals(Route.OrderTracking(7), NotificationRoutes.routeFor(mapOf("orderId" to "7", "status" to "READY_FOR_PICKUP")))
    }

    @Test fun a_missing_order_id_has_no_route() {
        assertNull(NotificationRoutes.routeFor(mapOf("status" to "READY_FOR_PICKUP")))
        assertNull(NotificationRoutes.routeFor(emptyMap()))
    }

    @Test fun a_non_numeric_or_non_positive_order_id_has_no_route() {
        assertNull(NotificationRoutes.routeFor(mapOf("orderId" to "abc")))
        assertNull(NotificationRoutes.routeFor(mapOf("orderId" to "")))
        assertNull(NotificationRoutes.routeFor(mapOf("orderId" to "-3")))
        assertNull(NotificationRoutes.routeFor(mapOf("orderId" to "0")))
    }

    @Test fun the_deep_link_uri_matches_the_tracking_route() {
        assertEquals("nusantara://order/7", NotificationRoutes.trackingUri(7))
    }

    @Test fun the_default_message_follows_the_status() {
        assertEquals("Pesanan Anda sedang diproses penjual", NotificationRoutes.defaultBody("SELLER_PROCESSING"))
        assertEquals("Pesanan Anda siap diambil", NotificationRoutes.defaultBody("READY_FOR_PICKUP"))
        assertEquals("Pesanan Anda selesai", NotificationRoutes.defaultBody("COMPLETED"))
        assertEquals("Pesanan Anda dibatalkan", NotificationRoutes.defaultBody("CANCELLED"))
    }

    @Test fun an_unknown_or_missing_status_gets_a_generic_message() {
        assertEquals("Status pesanan Anda berubah", NotificationRoutes.defaultBody("SOMETHING_NEW"))
        assertEquals("Status pesanan Anda berubah", NotificationRoutes.defaultBody(null))
    }
}
