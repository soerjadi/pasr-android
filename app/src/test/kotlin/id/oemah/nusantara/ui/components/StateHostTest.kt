package id.oemah.nusantara.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StateHostTest {
    @get:Rule
    val rule = createComposeRule()

    private var retries = 0

    private fun host(state: UiState<String>) = rule.setContent {
        NusantaraTheme { StateHost(state = state, onRetry = { retries++ }) { Text("isi: $it") } }
    }

    @Test fun loading_shows_a_skeleton() {
        host(UiState.Loading)
        rule.onNodeWithTag("skeleton").assertExists()
    }

    @Test fun content_is_rendered() {
        host(UiState.Content("x"))
        rule.onNodeWithText("isi: x").assertExists()
    }

    @Test fun offline_error_shows_message_and_retry_button() {
        host(UiState.Error(AppError.Offline))
        rule.onNodeWithText("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.").assertExists()

        rule.onNodeWithText("Coba lagi").performClick()

        assertEquals(1, retries)
    }
}
