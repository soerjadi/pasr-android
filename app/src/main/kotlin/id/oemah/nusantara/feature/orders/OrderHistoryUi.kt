package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.api.model.OrderStatus

/** The history list folds "created" and "processed by seller" into one "Diproses" chip. */
fun OrderStatus.historyChip(): String = when (this) {
    OrderStatus.CREATED, OrderStatus.SELLER_PROCESSING -> "Diproses"
    OrderStatus.READY_FOR_PICKUP -> "Siap Diambil"
    OrderStatus.COMPLETED -> "Selesai"
    OrderStatus.CANCELLED -> "Dibatalkan"
}

fun OrderTab.emptyMessage(): String = when (this) {
    OrderTab.ALL -> "Belum ada pesanan"
    OrderTab.PROCESSING -> "Tidak ada pesanan yang sedang diproses"
    OrderTab.READY -> "Belum ada pesanan yang siap diambil"
    OrderTab.DONE -> "Belum ada pesanan selesai"
}

/** Up to three item pictures for the history row, across all sellers. */
fun Order.thumbnails(max: Int = 3): List<ImageSet> =
    sellerOrders.flatMap { it.items }.map { it.image }.take(max)
