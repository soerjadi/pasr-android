package id.oemah.nusantara.ui.nav

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavHostTest {
    @get:Rule
    val rule = createComposeRule()

    private fun launch() = rule.setContent {
        NusantaraTheme {
            AppNavHost(navController = rememberNavController()) {
                composable<Route.Home> { Text("Halaman Beranda") }
                composable<Route.Categories> { Text("Halaman Kategori") }
                composable<Route.Orders> { Text("Halaman Pesanan") }
                composable<Route.Favorites> { Text("Halaman Favorit") }
                composable<Route.Account> { Text("Halaman Akun") }
            }
        }
    }

    @Test
    fun bottom_bar_shows_five_tabs_in_order() {
        launch()

        val labels = listOf("Beranda", "Kategori", "Pesanan", "Favorit", "Akun")
        labels.forEach { rule.onNodeWithText(it).assertExists() }

        val lefts = labels.map { rule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.left }
        assert(lefts == lefts.sorted()) { "tabs are not laid out left to right in order: $lefts" }
    }

    @Test
    fun tapping_kategori_selects_it() {
        launch()
        rule.onNodeWithText("Kategori").assertIsNotSelected()

        rule.onNodeWithText("Kategori").performClick()

        rule.onNodeWithText("Kategori").assertIsSelected()
        rule.onNodeWithText("Beranda").assertIsNotSelected()
    }

    @Test
    fun can_navigate_to_seller_orders_and_detail_routes() {
        lateinit var navController: androidx.navigation.NavHostController
        rule.setContent {
            NusantaraTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController) {
                    composable<Route.Home> { Text("Halaman Beranda") }
                    composable<Route.SellerOrders> { Text("Halaman Pesanan Masuk") }
                    composable<Route.SellerOrderDetail> { Text("Halaman Detail Pesanan") }
                }
            }
        }

        rule.runOnUiThread {
            navController.navigate(Route.SellerOrders(initialTab = "NEW"))
        }
        rule.onNodeWithText("Halaman Pesanan Masuk").assertExists()

        rule.runOnUiThread {
            navController.navigate(Route.SellerOrderDetail(orderId = 42L))
        }
        rule.onNodeWithText("Halaman Detail Pesanan").assertExists()
    }

    @Test
    fun can_navigate_to_seller_products_and_edit_routes() {
        lateinit var navController: androidx.navigation.NavHostController
        rule.setContent {
            NusantaraTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController) {
                    composable<Route.Home> { Text("Halaman Beranda") }
                    composable<Route.SellerProducts> { Text("Halaman Kelola Produk") }
                    composable<Route.EditProduct> { Text("Halaman Ubah Produk") }
                }
            }
        }

        rule.runOnUiThread {
            navController.navigate(Route.SellerProducts)
        }
        rule.onNodeWithText("Halaman Kelola Produk").assertExists()

        rule.runOnUiThread {
            navController.navigate(Route.EditProduct(productId = 101L))
        }
        rule.onNodeWithText("Halaman Ubah Produk").assertExists()
    }
}
