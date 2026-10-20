package id.oemah.nusantara.feature.products.detail

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import id.oemah.nusantara.api.model.ProductDetail
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDetailScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun screen(detail: ProductDetail) = rule.setContent {
        NusantaraTheme {
            ProductDetailScreen(
                detail = detail, favorite = false, quantity = 1,
                onQuantityChange = {}, onAddToCart = {}, onToggleFavorite = {}, onOpenSeller = {}, onBack = {},
            )
        }
    }

    @Test fun the_halal_badge_is_hidden_unless_the_server_verified_it() {
        screen(testDetail(halal = false))
        rule.onNodeWithText("Halal ID").assertDoesNotExist()
    }

    @Test fun the_halal_badge_is_shown_when_verified() {
        screen(testDetail(halal = true))
        rule.onNodeWithText("Halal ID").assertExists()
    }

    @Test fun a_sold_out_product_has_a_disabled_stok_habis_button() {
        screen(testDetail(stock = 0))
        rule.onNodeWithText("Stok habis").assertIsNotEnabled()
    }

    @Test fun the_pickup_address_and_hours_are_shown() {
        screen(testDetail())
        rule.onNodeWithText("Jl. Merdeka No. 12, Simo, Boyolali").assertExists()
        rule.onNodeWithText("Setiap hari 07.00 - 17.00").assertExists()
    }

    @Test fun price_name_and_seller_are_shown() {
        screen(testDetail())
        rule.onNodeWithText("Rp 15.000").assertExists()
        rule.onNodeWithText("Keripik Tempe").assertExists()
        rule.onNodeWithText("Bu Siti").assertExists()
    }
}
