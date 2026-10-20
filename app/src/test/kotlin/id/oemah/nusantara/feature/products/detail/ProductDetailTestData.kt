package id.oemah.nusantara.feature.products.detail

import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductDetail
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.SellerSummary

val testImages = ImageSet("https://img/s.png", "https://img/m.png", "https://img/l.png")

fun testSeller() = SellerSummary(
    id = 3, name = "Bu Siti", location = "Simo, Boyolali", rating = 4.8, ratingCount = 120,
    pickupAddress = "Jl. Merdeka No. 12, Simo, Boyolali", openingHours = "Setiap hari 07.00 - 17.00",
    whatsapp = "+6281234567890",
)

fun testDetail(stock: Int = 5, halal: Boolean = false, favorite: Boolean? = false) = ProductDetail(
    product = ProductSummary(
        id = 7, name = "Keripik Tempe", sellerId = 3, sellerName = "Bu Siti", price = 15_000, image = testImages,
        rating = 4.8, ratingCount = 120, stock = stock, halalVerified = halal,
    ),
    description = "Renyah dan gurih.",
    images = listOf(testImages, testImages, testImages),
    seller = testSeller(),
    isFavorite = favorite,
)
