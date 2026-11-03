package id.oemah.nusantara.feature.orders.seller

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerOrderDetailScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun sampleOrder(
        id: Long = 42L,
        status: OrderStatus = OrderStatus.CREATED,
        note: String? = null,
        acceptBy: String? = "2026-11-03T12:00:00Z",
    ) = StoreOrder(
        id = id,
        number = "NM-20261103-042",
        status = status,
        createdAt = "2026-11-03T10:00:00Z",
        buyerName = "Siti Rahma",
        subtotal = 50_000L,
        items = listOf(
            OrderItem(
                id = 1L,
                productId = 10L,
                name = "Ayam Bakar Madu",
                image = ImageSet("s", "m", "l"),
                quantity = 2,
                unitPrice = 25_000L,
                lineTotal = 50_000L,
                reviewed = false,
            ),
        ),
        statusHistory = listOf(
            StatusChange(OrderStatus.CREATED, "2026-11-03T10:00:00Z"),
        ),
        acceptBy = acceptBy,
        note = note,
    )

    @Test
    fun renders_header_buyer_info_and_status_pill() {
        val order = sampleOrder()
        val state = SellerOrderDetailState(orderState = UiState.Content(order))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("NM-20261103-042").assertIsDisplayed()
        rule.onNodeWithText("Siti Rahma").assertIsDisplayed()
        rule.onAllNodesWithText("Pesanan dibuat").assertCountEquals(2)
    }

    @Test
    fun displays_deadline_warning_for_created_order_with_acceptBy() {
        val order = sampleOrder(status = OrderStatus.CREATED, acceptBy = "2026-11-03T12:00:00Z")
        val state = SellerOrderDetailState(orderState = UiState.Content(order))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Batas konfirmasi: 3 Nov 2026, 19:00", substring = true).assertIsDisplayed()
    }

    @Test
    fun displays_itemized_breakdown_and_total() {
        val order = sampleOrder()
        val state = SellerOrderDetailState(orderState = UiState.Content(order))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Ayam Bakar Madu", substring = true).assertIsDisplayed()
        rule.onNodeWithText("@ Rp 25.000", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Total Subtotal").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Rp 50.000", substring = true).assertCountEquals(2)
    }

    @Test
    fun displays_pickup_note_callout_when_present() {
        val order = sampleOrder(
            status = OrderStatus.READY_FOR_PICKUP,
            note = "Ambil di meja kasir ya!",
        )
        val state = SellerOrderDetailState(orderState = UiState.Content(order))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Ambil di meja kasir ya!", substring = true).assertIsDisplayed()
    }

    @Test
    fun displays_timeline_steps() {
        val order = sampleOrder(
            status = OrderStatus.SELLER_PROCESSING,
            note = null,
        )
        val state = SellerOrderDetailState(orderState = UiState.Content(order))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Pesanan dibuat", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Siap diambil", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Selesai", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun created_status_actions_tolak_opens_dialog_and_terima_calls_accept() {
        val order = sampleOrder(status = OrderStatus.CREATED)
        val state = SellerOrderDetailState(orderState = UiState.Content(order))
        var rejectOpened = false
        var accepted = false

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = { accepted = true },
                    onOpenRejectDialog = { rejectOpened = true },
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tolak").performClick()
        assertTrue(rejectOpened)

        rule.onNodeWithText("Terima Pesanan").performClick()
        assertTrue(accepted)
    }

    @Test
    fun seller_processing_status_action_siap_diambil_opens_ready_dialog() {
        val order = sampleOrder(status = OrderStatus.SELLER_PROCESSING)
        val state = SellerOrderDetailState(orderState = UiState.Content(order))
        var readyOpened = false

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = { readyOpened = true },
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Siap Diambil").performClick()
        assertTrue(readyOpened)
    }

    @Test
    fun ready_for_pickup_status_action_selesaikan_pesanan_calls_complete() {
        val order = sampleOrder(status = OrderStatus.READY_FOR_PICKUP)
        val state = SellerOrderDetailState(orderState = UiState.Content(order))
        var completed = false

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = { completed = true },
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Selesaikan Pesanan").performClick()
        assertTrue(completed)
    }

    @Test
    fun completed_and_cancelled_orders_have_no_action_buttons() {
        val completedOrder = sampleOrder(status = OrderStatus.COMPLETED)
        val state = SellerOrderDetailState(orderState = UiState.Content(completedOrder))

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tolak").assertDoesNotExist()
        rule.onNodeWithText("Terima Pesanan").assertDoesNotExist()
        rule.onNodeWithText("Siap Diambil").assertDoesNotExist()
        rule.onNodeWithText("Selesaikan Pesanan").assertDoesNotExist()
    }

    @Test
    fun action_error_displays_snackbar_and_calls_dismiss() {
        val order = sampleOrder()
        val state = SellerOrderDetailState(
            orderState = UiState.Content(order),
            actionError = "Gagal memproses pesanan",
        )
        var errorDismissed = false

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = { errorDismissed = true },
                )
            }
        }

        rule.onNodeWithText("Gagal memproses pesanan").assertIsDisplayed()
        assertTrue(errorDismissed)
        rule.mainClock.advanceTimeBy(5000)
    }

    @Test
    fun back_button_triggers_onBack() {
        val order = sampleOrder()
        val state = SellerOrderDetailState(orderState = UiState.Content(order))
        var backPressed = false

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = { backPressed = true },
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithContentDescription("Kembali").performClick()
        assertTrue(backPressed)
    }

    @Test
    fun renders_dialogs_when_requested() {
        val order = sampleOrder()
        val state = SellerOrderDetailState(
            orderState = UiState.Content(order),
            showRejectDialog = true,
        )

        rule.setContent {
            NusantaraTheme {
                SellerOrderDetailContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onAccept = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onReject = { _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReady = {},
                    onComplete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tolak Pesanan").assertIsDisplayed()
        rule.onNodeWithText("Konfirmasi Tolak").assertIsDisplayed()
    }
}

