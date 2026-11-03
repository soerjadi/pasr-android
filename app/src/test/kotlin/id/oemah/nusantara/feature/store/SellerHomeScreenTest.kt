package id.oemah.nusantara.feature.store

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreStatsTopProductsInner
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerHomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun verified_store_renders_status_card_and_daily_summary() {
        val summary = StoreSummaryResponse(
            isOpen = true,
            closesAt = "17:00",
            newOrderCount = 0,
            acceptBy = null,
            ordersToday = 8,
            salesToday = 150_000,
            latestOrders = emptyList(),
        )
        val stats = StoreStats(
            range = StoreStats.Range.TODAY,
            salesValue = 150_000,
            completedOrders = 8,
            daily = emptyList(),
            topProducts = listOf(
                StoreStatsTopProductsInner(productId = 1, name = "Kripik Tempe", unitsSold = 10),
            ),
        )

        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.VERIFIED, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Content(summary),
                    statsState = UiState.Content(stats),
                    selectedRange = id.oemah.nusantara.api.MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = {},
                    onSelectRange = {},
                    actionError = null,
                    onDismissError = {},
                )
            }
        }

        compose.onNodeWithText("Warung Bu Siti").assertIsDisplayed()
        compose.onNodeWithText("Buka").assertIsDisplayed()
        compose.onNodeWithText("Tutup pukul 17:00 WIB").assertIsDisplayed()
        compose.onNodeWithText("8").assertIsDisplayed()
        compose.onNodeWithText("Rp 150.000").assertIsDisplayed()
        compose.onNodeWithText("Kripik Tempe").assertIsDisplayed()
        compose.onNodeWithText("10 terjual").assertIsDisplayed()
    }

    @Test fun closed_store_renders_tutup_label() {
        val summary = StoreSummaryResponse(
            isOpen = false,
            closesAt = "17:00",
            newOrderCount = 0,
            acceptBy = null,
            ordersToday = 2,
            salesToday = 30_000,
            latestOrders = emptyList(),
        )
        val stats = StoreStats(
            range = StoreStats.Range.TODAY,
            salesValue = 30_000,
            completedOrders = 2,
            daily = emptyList(),
            topProducts = emptyList(),
        )

        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.VERIFIED, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Content(summary),
                    statsState = UiState.Content(stats),
                    selectedRange = id.oemah.nusantara.api.MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = {},
                    onSelectRange = {},
                    actionError = null,
                    onDismissError = {},
                )
            }
        }

        compose.onNodeWithText("Tutup").assertIsDisplayed()
        compose.onNodeWithText("Tutup pukul 17:00 WIB").assertDoesNotExist()
    }

    @Test fun pending_store_renders_pending_section() {
        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.PENDING, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Loading,
                    statsState = UiState.Loading,
                    selectedRange = id.oemah.nusantara.api.MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = {},
                    onSelectRange = {},
                    actionError = null,
                    onDismissError = {},
                )
            }
        }

        compose.onNodeWithText("Menunggu verifikasi").assertIsDisplayed()
    }

    @Test fun switching_range_and_toggling_open_trigger_callbacks() {
        val summary = StoreSummaryResponse(
            isOpen = true,
            closesAt = "17:00",
            newOrderCount = 0,
            acceptBy = null,
            ordersToday = 0,
            salesToday = 0,
            latestOrders = emptyList(),
        )
        val stats = StoreStats(
            range = StoreStats.Range.TODAY,
            salesValue = 0,
            completedOrders = 0,
            daily = emptyList(),
            topProducts = emptyList(),
        )

        var selectedRange: MeApi.RangeGetMyStoreStats? = null

        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.VERIFIED, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Content(summary),
                    statsState = UiState.Content(stats),
                    selectedRange = MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = {},
                    onSelectRange = { range: MeApi.RangeGetMyStoreStats -> selectedRange = range },
                    actionError = null,
                    onDismissError = {},
                )
            }
        }

        compose.onNodeWithText("7 Hari Terakhir").performClick()
        assertEquals(MeApi.RangeGetMyStoreStats.WEEK, selectedRange)
    }

    @Test fun toggling_switch_calls_onToggleOpen() {
        val summary = StoreSummaryResponse(
            isOpen = true,
            closesAt = "17:00",
            newOrderCount = 0,
            acceptBy = null,
            ordersToday = 8,
            salesToday = 150_000,
            latestOrders = emptyList(),
        )
        val stats = StoreStats(
            range = StoreStats.Range.TODAY,
            salesValue = 150_000,
            completedOrders = 8,
            daily = emptyList(),
            topProducts = emptyList(),
        )

        var toggled = false
        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.VERIFIED, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Content(summary),
                    statsState = UiState.Content(stats),
                    selectedRange = MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = { toggled = true },
                    onSelectRange = {},
                    actionError = null,
                    onDismissError = {},
                )
            }
        }

        compose.onNode(isToggleable()).performClick()
        assertTrue(toggled)
    }

    @Test fun action_error_triggers_onDismissError() {
        var dismissed = false
        compose.setContent {
            NusantaraTheme {
                SellerHomeContent(
                    store = StoreSummary(1, "Warung Bu Siti", StoreStatus.VERIFIED, 0),
                    ownerName = "Siti",
                    summaryState = UiState.Loading,
                    statsState = UiState.Loading,
                    selectedRange = MeApi.RangeGetMyStoreStats.TODAY,
                    onBack = {},
                    onAddProduct = {},
                    onToggleOpen = {},
                    onSelectRange = {},
                    actionError = "Gagal mengubah status toko. Silakan coba lagi.",
                    onDismissError = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Gagal mengubah status toko. Silakan coba lagi.").assertIsDisplayed()
        assertTrue(dismissed)
        compose.mainClock.advanceTimeBy(5000)
    }
}
