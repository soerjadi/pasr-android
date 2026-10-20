package id.oemah.nusantara.feature.cart.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One product in the cart. The name, price and image are a display snapshot; the server re-prices at checkout. */
@Entity(tableName = "cart_lines", indices = [Index(value = ["productId"], unique = true)])
data class CartLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val sellerId: Long,
    val sellerName: String,
    val name: String,
    val imageUrl: String,
    val unitPrice: Long,
    val quantity: Int,
)
