package id.oemah.nusantara.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuantityStepperTest {
    @get:Rule
    val rule = createComposeRule()

    private val changes = mutableListOf<Int>()

    private fun stepper(value: Int, min: Int = 1, max: Int = 5) = rule.setContent {
        NusantaraTheme { QuantityStepper(value = value, min = min, max = max, onChange = { changes += it }) }
    }

    @Test fun at_min_the_decrease_button_is_disabled() {
        stepper(value = 1)
        rule.onNodeWithContentDescription("Kurangi jumlah").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Tambah jumlah").assertIsEnabled()
    }

    @Test fun at_max_the_increase_button_is_disabled() {
        stepper(value = 5)
        rule.onNodeWithContentDescription("Tambah jumlah").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Kurangi jumlah").assertIsEnabled()
    }

    @Test fun both_buttons_meet_the_48dp_touch_target() {
        stepper(value = 3)
        listOf("Kurangi jumlah", "Tambah jumlah").forEach {
            rule.onNodeWithContentDescription(it).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    @Test fun increase_reports_value_plus_one() {
        stepper(value = 2)
        rule.onNodeWithContentDescription("Tambah jumlah").performClick()
        assertEquals(listOf(3), changes)
    }

    @Test fun decrease_reports_value_minus_one() {
        stepper(value = 2)
        rule.onNodeWithContentDescription("Kurangi jumlah").performClick()
        assertEquals(listOf(1), changes)
    }
}
