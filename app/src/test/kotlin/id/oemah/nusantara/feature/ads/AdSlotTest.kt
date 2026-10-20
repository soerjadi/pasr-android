package id.oemah.nusantara.feature.ads

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdSlotTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String>()

    private fun ad(title: String = "KUR Bank BRI", label: String = "Kemitraan", url: String = "https://bri.co.id", id: Long = 1) = Ad(
        id = id, placement = AdPlacementKey.HOME_PARTNER_CAROUSEL, title = title,
        image = ImageSet("s", "m", "l"), targetUrl = url, label = label,
    )

    private fun slot(vararg ads: Ad) = rule.setContent {
        NusantaraTheme { AdSlot(ads = ads.toList(), onOpen = { opened += it }) }
    }

    @Test fun an_empty_list_renders_nothing() {
        slot()
        rule.onNodeWithTag("ad_slot").assertDoesNotExist()
    }

    @Test fun a_shown_ad_displays_its_label() {
        slot(ad(label = "Kemitraan"))
        rule.onNodeWithText("Kemitraan").assertExists()
    }

    @Test fun a_blank_label_falls_back_to_iklan() {
        slot(ad(label = "  "))
        rule.onNodeWithText("Iklan").assertExists()
    }

    @Test fun a_safe_link_is_opened_on_click() {
        slot(ad(url = "https://bri.co.id"))
        rule.onNodeWithText("KUR Bank BRI").performClick()
        assertEquals(listOf("https://bri.co.id"), opened)
    }

    @Test fun an_unsafe_link_is_never_opened() {
        slot(ad(url = "javascript:alert(1)"))
        rule.onNodeWithText("KUR Bank BRI").performClick()
        assertEquals(emptyList<String>(), opened)
    }

    @Test fun several_ads_are_all_shown() {
        slot(ad(title = "Satu", id = 1), ad(title = "Dua", id = 2))
        rule.onNodeWithText("Satu").assertExists()
        rule.onNodeWithText("Dua").assertExists()
    }
}
