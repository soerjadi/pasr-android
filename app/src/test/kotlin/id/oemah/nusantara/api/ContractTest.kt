package id.oemah.nusantara.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ContractTest {
    private val apiClasses = listOf("AuthApi", "MeApi", "CatalogApi", "SocialApi", "OrdersApi", "AdsApi")

    private val expectedOperations = setOf(
        "requestOtp", "verifyOtp", "refreshSession", "logout",
        "getMe", "updateMe", "registerDevice",
        "listCategories", "listProducts", "getProduct", "getSeller", "listSellerReviews",
        "listFavorites", "addFavorite", "removeFavorite", "followSeller", "unfollowSeller", "submitReview",
        "validateCart", "createOrder", "listOrders", "getOrder",
        "listAds",
    )

    @Test fun generated_client_exposes_every_expected_operation() {
        val actual = apiClasses.flatMap { name ->
            Class.forName("id.oemah.nusantara.api.$name").declaredMethods
                .filterNot { it.isSynthetic || '$' in it.name } // Kotlin's `foo$default` bridges are not operations
                .map { it.name }
        }.toSet()

        assertEquals(23, expectedOperations.size)
        assertEquals(expectedOperations, actual)
    }
}
