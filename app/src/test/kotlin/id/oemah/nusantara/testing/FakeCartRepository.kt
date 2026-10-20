package id.oemah.nusantara.testing

import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.CartLine
import id.oemah.nusantara.feature.cart.CartProductSnapshot
import id.oemah.nusantara.feature.cart.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory cart with the same merge and clamp rules as the Room implementation. */
class FakeCartRepository(initial: List<CartLine> = emptyList()) : CartRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override val lines: Flow<List<CartLine>> = state.map { it }
    val current: List<CartLine> get() = state.value
    var updatedSnapshots = mutableListOf<Triple<Long, String, Rupiah>>()

    override suspend fun add(product: CartProductSnapshot, quantity: Int) {
        val existing = state.value.firstOrNull { it.productId == product.productId }
        state.value = if (existing == null) {
            state.value + CartLine(nextId++, product.productId, product.sellerId, product.sellerName, product.name, product.imageUrl, product.unitPrice, quantity.coerceAtMost(99))
        } else {
            state.value.map { if (it.id == existing.id) it.copy(quantity = (it.quantity + quantity).coerceAtMost(99)) else it }
        }
    }

    override suspend fun setQuantity(lineId: Long, quantity: Int) {
        state.value = if (quantity <= 0) state.value.filterNot { it.id == lineId }
        else state.value.map { if (it.id == lineId) it.copy(quantity = quantity.coerceAtMost(99)) else it }
    }

    override suspend fun remove(lineId: Long) { state.value = state.value.filterNot { it.id == lineId } }

    override suspend fun removeAll(lineIds: Collection<Long>) { state.value = state.value.filterNot { it.id in lineIds } }

    override suspend fun updateSnapshot(productId: Long, name: String, unitPrice: Rupiah) {
        updatedSnapshots += Triple(productId, name, unitPrice)
        state.value = state.value.map { if (it.productId == productId) it.copy(name = name, unitPrice = unitPrice) else it }
    }
}
