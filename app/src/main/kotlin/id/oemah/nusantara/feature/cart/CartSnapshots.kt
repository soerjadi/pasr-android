package id.oemah.nusantara.feature.cart

import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.Rupiah

/** What the cart remembers about a product for offline display; the server re-prices at checkout. */
fun ProductSummary.toCartSnapshot() = CartProductSnapshot(
    productId = id,
    sellerId = sellerId,
    sellerName = sellerName,
    name = name,
    imageUrl = image.small,
    unitPrice = Rupiah(price),
)
