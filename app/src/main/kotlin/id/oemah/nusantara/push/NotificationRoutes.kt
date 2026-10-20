package id.oemah.nusantara.push

import id.oemah.nusantara.ui.nav.Route

/** The push payload contract (data-only messages): `{ orderId, status, title?, body? }`. */
object NotificationRoutes {
    fun routeFor(data: Map<String, String>): Route? =
        data["orderId"]?.toLongOrNull()?.takeIf { it > 0 }?.let { Route.OrderTracking(it) }

    /** Matches the deep link registered for [Route.OrderTracking] in the nav graph and the manifest. */
    fun trackingUri(orderId: Long): String = "nusantara://order/$orderId"

    fun defaultBody(status: String?): String = when (status) {
        "SELLER_PROCESSING" -> "Pesanan Anda sedang diproses penjual"
        "READY_FOR_PICKUP" -> "Pesanan Anda siap diambil"
        "COMPLETED" -> "Pesanan Anda selesai"
        "CANCELLED" -> "Pesanan Anda dibatalkan"
        else -> "Status pesanan Anda berubah"
    }
}
