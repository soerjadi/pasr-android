package id.oemah.nusantara.testing

import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.PaymentMethod
import id.oemah.nusantara.api.model.SellerOrder
import id.oemah.nusantara.api.model.StatusChange

fun testOrderItem(id: Long = 1, productId: Long = 11, reviewed: Boolean = false) = OrderItem(
    id = id, productId = productId, name = "Keripik Tempe", image = ImageSet("s", "m", "l"),
    quantity = 1, unitPrice = 15_000, lineTotal = 15_000, reviewed = reviewed,
)

fun testSellerOrder(
    id: Long = 1,
    status: OrderStatus = OrderStatus.CREATED,
    items: List<OrderItem> = listOf(testOrderItem()),
    note: String? = null,
) = SellerOrder(
    id = id, sellerId = 3, sellerName = "Bu Siti", whatsapp = "+6281234567890",
    pickupAddress = "Jl. Merdeka No. 12, Simo, Boyolali", openingHours = "Setiap hari 07.00 - 17.00",
    status = status, subtotal = items.sumOf { it.lineTotal }, items = items, note = note,
)

fun testOrder(
    id: Long = 1,
    status: OrderStatus = OrderStatus.CREATED,
    sellerOrders: List<SellerOrder> = listOf(testSellerOrder(status = status)),
    history: List<StatusChange> = listOf(StatusChange(OrderStatus.CREATED, "2026-10-15T03:02:00Z")),
) = Order(
    id = id, number = "NM-20261015-%03d".format(id), status = status, createdAt = "2026-10-15T03:02:00Z",
    total = sellerOrders.sumOf { it.subtotal }, paymentMethod = PaymentMethod.CASH_AT_PICKUP,
    sellerOrders = sellerOrders, statusHistory = history,
)
