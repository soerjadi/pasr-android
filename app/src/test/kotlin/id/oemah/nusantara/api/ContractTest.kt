package id.oemah.nusantara.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ContractTest {
    private val apiClasses = listOf("AuthApi", "MeApi", "CatalogApi", "SocialApi", "OrdersApi", "AdsApi", "AdminApi")

    private val expectedOperations = setOf(
        "requestOtp", "verifyOtp", "refreshSession", "logout",
        "getMe", "updateMe", "registerDevice", "createMyStore", "createMyProduct",
        "getMyStore", "updateMyStore", "setMyStoreOpen", "uploadImage",
        "listMyProducts", "getMyProduct", "updateMyProduct", "deleteMyProduct",
        "getMyStoreSummary", "getMyStoreStats",
        "listMyStoreOrders", "getMyStoreOrder", "acceptMyStoreOrder", "rejectMyStoreOrder", "readyMyStoreOrder", "completeMyStoreOrder",
        "listCategories", "listProducts", "getProduct", "getSeller", "listSellerReviews",
        "listFavorites", "addFavorite", "removeFavorite", "followSeller", "unfollowSeller", "submitReview",
        "validateCart", "createOrder", "listOrders", "getOrder",
        "listAds",
        "listAdminStores", "verifyAdminStore", "suspendAdminStore", "reinstateAdminStore",
        "updateAdminProduct",
        "createAdminCategory", "updateAdminCategory", "deleteAdminCategory",
        "listAdCampaigns", "createAdCampaign", "updateAdCampaign", "deleteAdCampaign",
        "listAdminOrders", "acceptAdminSellerOrder", "rejectAdminSellerOrder", "readyAdminSellerOrder", "completeAdminSellerOrder", "cancelAdminSellerOrder",
        "deleteAdminReview",
    )

    @Test fun generated_client_exposes_every_expected_operation() {
        val actual = apiClasses.flatMap { name ->
            Class.forName("id.oemah.nusantara.api.$name").declaredMethods
                .filterNot { it.isSynthetic || '$' in it.name } // Kotlin's `foo$default` bridges are not operations
                .map { it.name }
        }.toSet()

        assertEquals(60, expectedOperations.size)
        assertEquals(expectedOperations, actual)
    }
}
