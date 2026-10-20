package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.OrderStatus

fun OrderStatus.label(): String = when (this) {
    OrderStatus.CREATED -> "Pesanan dibuat"
    OrderStatus.SELLER_PROCESSING -> "Diproses penjual"
    OrderStatus.READY_FOR_PICKUP -> "Siap diambil"
    OrderStatus.COMPLETED -> "Selesai"
    OrderStatus.CANCELLED -> "Dibatalkan"
}

fun OrderStatus.isFinished(): Boolean = this == OrderStatus.COMPLETED || this == OrderStatus.CANCELLED
