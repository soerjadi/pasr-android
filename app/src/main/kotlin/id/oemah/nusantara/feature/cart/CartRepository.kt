package id.oemah.nusantara.feature.cart

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.data.CartDao
import id.oemah.nusantara.feature.cart.data.CartDatabase
import id.oemah.nusantara.feature.cart.data.CartLineEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CartLine(
    val id: Long,
    val productId: Long,
    val sellerId: Long,
    val sellerName: String,
    val name: String,
    val imageUrl: String,
    val unitPrice: Rupiah,
    val quantity: Int,
)

data class CartProductSnapshot(
    val productId: Long,
    val sellerId: Long,
    val sellerName: String,
    val name: String,
    val imageUrl: String,
    val unitPrice: Rupiah,
)

/** The cart lives on the device so it works offline; prices shown here are snapshots until validated. */
interface CartRepository {
    val lines: Flow<List<CartLine>>
    suspend fun add(product: CartProductSnapshot, quantity: Int = 1)

    /** A quantity of zero or less removes the line; anything above 99 is clamped to 99. */
    suspend fun setQuantity(lineId: Long, quantity: Int)
    suspend fun remove(lineId: Long)
    suspend fun removeAll(lineIds: Collection<Long>)
    suspend fun updateSnapshot(productId: Long, name: String, unitPrice: Rupiah)
}

class RoomCartRepository @Inject constructor(private val dao: CartDao) : CartRepository {
    override val lines: Flow<List<CartLine>> = dao.observe().map { rows -> rows.map { it.toLine() } }

    override suspend fun add(product: CartProductSnapshot, quantity: Int) {
        require(quantity >= 1) { "quantity must be at least 1" }
        dao.addOrMerge(product.toEntity(quantity), MAX_QUANTITY)
    }

    override suspend fun setQuantity(lineId: Long, quantity: Int) {
        if (quantity <= 0) dao.delete(lineId) else dao.setQuantity(lineId, quantity.coerceAtMost(MAX_QUANTITY))
    }

    override suspend fun remove(lineId: Long) = dao.delete(lineId)

    override suspend fun removeAll(lineIds: Collection<Long>) = dao.deleteAll(lineIds)

    override suspend fun updateSnapshot(productId: Long, name: String, unitPrice: Rupiah) =
        dao.updateSnapshot(productId, name, unitPrice.amount)

    private fun CartLineEntity.toLine() = CartLine(id, productId, sellerId, sellerName, name, imageUrl, Rupiah(unitPrice), quantity)

    private fun CartProductSnapshot.toEntity(quantity: Int) = CartLineEntity(
        productId = productId, sellerId = sellerId, sellerName = sellerName, name = name,
        imageUrl = imageUrl, unitPrice = unitPrice.amount, quantity = quantity,
    )

    private companion object {
        const val MAX_QUANTITY = 99
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CartModule {
    @Binds abstract fun cartRepository(impl: RoomCartRepository): CartRepository

    companion object {
        @Provides @Singleton
        fun cartDatabase(@ApplicationContext context: Context): CartDatabase =
            Room.databaseBuilder(context, CartDatabase::class.java, "cart.db").build()

        @Provides fun cartDao(database: CartDatabase): CartDao = database.cartDao()
    }
}
