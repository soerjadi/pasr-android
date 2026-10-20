package id.oemah.nusantara.feature.cart.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CartDao {
    @Query("SELECT * FROM cart_lines ORDER BY id")
    abstract fun observe(): Flow<List<CartLineEntity>>

    @Query("SELECT * FROM cart_lines WHERE productId = :productId")
    protected abstract suspend fun byProduct(productId: Long): CartLineEntity?

    @Insert
    protected abstract suspend fun insert(line: CartLineEntity)

    @Query("UPDATE cart_lines SET quantity = :quantity WHERE id = :id")
    abstract suspend fun setQuantity(id: Long, quantity: Int)

    @Query("DELETE FROM cart_lines WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM cart_lines WHERE id IN (:ids)")
    abstract suspend fun deleteAll(ids: Collection<Long>)

    @Query("UPDATE cart_lines SET name = :name, unitPrice = :unitPrice WHERE productId = :productId")
    abstract suspend fun updateSnapshot(productId: Long, name: String, unitPrice: Long)

    /** Adds [line], or adds [line]'s quantity to the existing line for the same product, capped at [maxQuantity]. */
    @Transaction
    open suspend fun addOrMerge(line: CartLineEntity, maxQuantity: Int) {
        val existing = byProduct(line.productId)
        if (existing == null) {
            insert(line.copy(quantity = line.quantity.coerceAtMost(maxQuantity)))
        } else {
            setQuantity(existing.id, (existing.quantity + line.quantity).coerceAtMost(maxQuantity))
        }
    }
}
