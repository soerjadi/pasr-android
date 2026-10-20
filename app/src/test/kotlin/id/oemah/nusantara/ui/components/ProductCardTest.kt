package id.oemah.nusantara.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductCardTest {
    @get:Rule val rule = createComposeRule()

    private var opened = 0
    private var added = 0

    private fun product(stock: Int) = ProductSummary(
        id = 1, name = "Keripik Tempe", sellerId = 2, sellerName = "Bu Siti", price = 15_000,
        image = ImageSet("s", "m", "l"), rating = 4.8, ratingCount = 120, stock = stock, halalVerified = true,
    )

    private fun card(stock: Int) = rule.setContent {
        // 160 dp is a real 2-column grid cell; a full-width card would not fit the 470 dp test screen.
        NusantaraTheme { ProductCard(product(stock), onOpen = { opened++ }, onAdd = { added++ }, modifier = Modifier.width(160.dp)) }
    }

    @Test fun shows_name_seller_price_and_rating() {
        card(stock = 5)
        rule.onNodeWithText("Keripik Tempe").assertExists()
        rule.onNodeWithText("Bu Siti").assertExists()
        rule.onNodeWithText("Rp 15.000").assertExists()
        rule.onNodeWithText("4.8").assertExists()
    }

    @Test fun tapping_the_card_opens_it_and_the_add_button_adds() {
        card(stock = 5)

        rule.onNodeWithText("Keripik Tempe").performClick()
        rule.onNodeWithContentDescription("Tambah ke keranjang").performClick()

        assertEquals(1, opened)
        assertEquals(1, added)
    }

    @Test fun a_sold_out_product_shows_habis_and_cannot_be_added() {
        card(stock = 0)

        rule.onNodeWithText("Habis").assertExists()
        rule.onNodeWithContentDescription("Tambah ke keranjang").assertIsNotEnabled()
    }

    @Test fun an_in_stock_product_has_an_enabled_add_button_that_meets_the_touch_target() {
        card(stock = 5)

        rule.onNodeWithContentDescription("Tambah ke keranjang")
            .assertIsEnabled().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }
}
