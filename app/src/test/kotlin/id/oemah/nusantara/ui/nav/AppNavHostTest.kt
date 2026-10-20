package id.oemah.nusantara.ui.nav

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        NusantaraTheme { AppNavHost(navController = rememberNavController()) }
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
}
