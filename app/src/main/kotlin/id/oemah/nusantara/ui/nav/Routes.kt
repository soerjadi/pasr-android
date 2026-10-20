package id.oemah.nusantara.ui.nav

import kotlinx.serialization.Serializable

/** Every navigation destination. Declared up front so features can link to each other without forward references. */
@Serializable
sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Categories : Route
    @Serializable data object Orders : Route
    @Serializable data object Favorites : Route
    @Serializable data object Account : Route

    @Serializable data object Onboarding : Route
    @Serializable data object Login : Route
    @Serializable data object Search : Route
    @Serializable data class ProductList(val categoryId: Long? = null, val title: String, val featured: Boolean = false) : Route
    @Serializable data class ProductDetail(val productId: Long) : Route
    @Serializable data class Seller(val sellerId: Long) : Route
    @Serializable data object Cart : Route
    @Serializable data object Checkout : Route
    @Serializable data class OrderConfirmation(val orderId: Long) : Route
    @Serializable data class OrderTracking(val orderId: Long) : Route
    @Serializable data class WriteReview(val orderItemId: Long, val productName: String) : Route
}
