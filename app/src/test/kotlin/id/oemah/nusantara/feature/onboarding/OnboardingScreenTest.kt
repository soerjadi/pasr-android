package id.oemah.nusantara.feature.onboarding

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {
    @get:Rule val rule = createComposeRule()

    private var finished = 0

    private fun screen() = rule.setContent { NusantaraTheme { OnboardingScreen(onFinish = { finished++ }) } }

    @Test fun the_first_page_explains_local_products() {
        screen()

        rule.onNodeWithText("Temukan Produk UMKM Lokal").assertExists()
        rule.onNodeWithText("Lanjut").assertExists()
    }

    @Test fun skip_finishes_straight_away() {
        screen()

        rule.onNodeWithText("Lewati").performClick()

        assertEquals(1, finished)
    }

    @Test fun next_shows_the_pickup_page_whose_button_finishes() {
        screen()

        rule.onNodeWithText("Lanjut").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Pesan, lalu Ambil di Toko").assertExists()
        assertEquals(0, finished)

        rule.onNodeWithText("Mulai").performClick()

        assertEquals(1, finished)
    }
}
