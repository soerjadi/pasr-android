package id.oemah.nusantara.feature.orders.seller

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoreOrderCardTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleOrder(
        id: Long = 101L,
        number: String = "NM-20261103-001",
        status: OrderStatus = OrderStatus.CREATED,
        buyerName: String = "Budi Santoso",
        subtotal: Long = 45000L,
        items: List<OrderItem> = listOf(
            OrderItem(
                id = 1L,
                productId = 10L,
                name = "Ayam Geprek",
                image = ImageSet("s", "m", "l"),
                quantity = 2,
                unitPrice = 20000L,
                lineTotal = 40000L,
                reviewed = false,
            ),
            OrderItem(
                id = 2L,
                productId = 11L,
                name = "Es Teh Manis",
                image = ImageSet("s", "m", "l"),
                quantity = 1,
                unitPrice = 5000L,
                lineTotal = 5000L,
                reviewed = false,
            ),
        ),
        acceptBy: String? = "2026-11-03T12:00:00Z",
        note: String? = null,
    ) = StoreOrder(
        id = id,
        number = number,
        status = status,
        createdAt = "2026-11-03T10:00:00Z",
        buyerName = buyerName,
        subtotal = subtotal,
        items = items,
        statusHistory = emptyList(),
        acceptBy = acceptBy,
        note = note,
    )

    @Test
    fun shows_order_details_and_items_summary() {
        val order = sampleOrder()
        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("NM-20261103-001").assertIsDisplayed()
        rule.onNodeWithText("Budi Santoso").assertIsDisplayed()
        rule.onNodeWithText("Pesanan dibuat").assertIsDisplayed()
        rule.onNodeWithText("2 × Ayam Geprek").assertIsDisplayed()
        rule.onNodeWithText("1 × Es Teh Manis").assertIsDisplayed()
        rule.onNodeWithText("Rp 45.000").assertIsDisplayed()
    }

    @Test
    fun created_status_shows_accept_and_reject_buttons_and_triggers_callbacks() {
        val order = sampleOrder(status = OrderStatus.CREATED)
        var accepted = false
        var rejected = false

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = { accepted = true },
                    onReject = { rejected = true },
                    onReady = {},
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("Terima Pesanan").assertIsDisplayed().performClick()
        assertTrue(accepted)

        rule.onNodeWithText("Tolak").assertIsDisplayed().performClick()
        assertTrue(rejected)
    }

    @Test
    fun seller_processing_status_shows_ready_button() {
        val order = sampleOrder(status = OrderStatus.SELLER_PROCESSING)
        var ready = false

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = { ready = true },
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("Siap Diambil").assertIsDisplayed().performClick()
        assertTrue(ready)

        rule.onNodeWithText("Terima Pesanan").assertDoesNotExist()
        rule.onNodeWithText("Tolak").assertDoesNotExist()
    }

    @Test
    fun ready_for_pickup_shows_pickup_note_and_complete_button() {
        val order = sampleOrder(
            status = OrderStatus.READY_FOR_PICKUP,
            note = "Ambil di meja kasir depan",
        )
        var completed = false

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = { completed = true },
                )
            }
        }

        rule.onNodeWithText("Ambil di meja kasir depan").assertIsDisplayed()
        rule.onNodeWithText("Selesaikan Pesanan").assertIsDisplayed().performClick()
        assertTrue(completed)
    }

    @Test
    fun completed_status_shows_no_action_buttons() {
        val order = sampleOrder(status = OrderStatus.COMPLETED)

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("Selesai").assertIsDisplayed()
        rule.onNodeWithText("Terima Pesanan").assertDoesNotExist()
        rule.onNodeWithText("Tolak").assertDoesNotExist()
        rule.onNodeWithText("Siap Diambil").assertDoesNotExist()
        rule.onNodeWithText("Selesaikan Pesanan").assertDoesNotExist()
    }

    @Test
    fun loading_state_disables_action_buttons() {
        val order = sampleOrder(status = OrderStatus.CREATED)

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = {},
                    isLoading = true,
                )
            }
        }

        rule.onNodeWithText("Terima Pesanan").assertIsNotEnabled()
        rule.onNodeWithText("Tolak").assertIsNotEnabled()
    }

    @Test
    fun clicking_card_triggers_onClick() {
        val order = sampleOrder()
        var clicked = false

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = { clicked = true },
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("NM-20261103-001").performClick()
        assertTrue(clicked)
    }

    @Test
    fun cancelled_status_shows_no_action_buttons() {
        val order = sampleOrder(status = OrderStatus.CANCELLED)

        rule.setContent {
            NusantaraTheme {
                StoreOrderCard(
                    order = order,
                    onClick = {},
                    onAccept = {},
                    onReject = {},
                    onReady = {},
                    onComplete = {},
                )
            }
        }

        rule.onNodeWithText("Dibatalkan").assertIsDisplayed()
        rule.onNodeWithText("Terima Pesanan").assertDoesNotExist()
        rule.onNodeWithText("Tolak").assertDoesNotExist()
    }

    @Test
    fun reject_order_dialog_default_reason_confirms_without_note() {
        val order = sampleOrder()
        var confirmedReason: RejectRequest.Reason? = null
        var confirmedNote: String? = null

        rule.setContent {
            NusantaraTheme {
                RejectOrderDialog(
                    order = order,
                    onConfirm = { reason, note ->
                        confirmedReason = reason
                        confirmedNote = note
                    },
                    onDismiss = {},
                )
            }
        }

        rule.onNodeWithText("Konfirmasi Tolak").assertIsEnabled().performClick()
        assertEquals(RejectRequest.Reason.OUT_OF_STOCK, confirmedReason)
        assertNull(confirmedNote)
    }

    @Test
    fun reject_order_dialog_flow() {
        val order = sampleOrder()
        var confirmedReason: RejectRequest.Reason? = null
        var confirmedNote: String? = null
        var dismissed = false

        rule.setContent {
            NusantaraTheme {
                RejectOrderDialog(
                    order = order,
                    onConfirm = { reason, note ->
                        confirmedReason = reason
                        confirmedNote = note
                    },
                    onDismiss = { dismissed = true },
                )
            }
        }

        rule.onNodeWithText("Tolak Pesanan").assertIsDisplayed()
        rule.onNodeWithText("Stok produk habis").assertIsDisplayed()

        // Select OTHER reason
        rule.onNodeWithText("Alasan lainnya").performClick()

        // Confirm button disabled when note is blank
        rule.onNodeWithText("Konfirmasi Tolak").assertIsNotEnabled()

        // Type note
        rule.onNodeWithText("Tulis alasan penolakan").performTextInput("Warung tutup mendadak")
        rule.onNodeWithText("Konfirmasi Tolak").assertIsEnabled().performClick()

        assertEquals(RejectRequest.Reason.OTHER, confirmedReason)
        assertEquals("Warung tutup mendadak", confirmedNote)

        rule.onNodeWithText("Batal").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun ready_order_dialog_flow() {
        val order = sampleOrder()
        var confirmedNote: String? = null
        var dismissed = false

        rule.setContent {
            NusantaraTheme {
                ReadyOrderDialog(
                    order = order,
                    onConfirm = { note -> confirmedNote = note },
                    onDismiss = { dismissed = true },
                )
            }
        }

        rule.onNodeWithText("Pesanan Siap Diambil").assertIsDisplayed()
        rule.onNodeWithText("Catatan atau instruksi pengambilan (opsional)")
            .performTextInput("Siap di etalase nomor 3")

        rule.onNodeWithText("Tandai Siap").performClick()
        assertEquals("Siap di etalase nomor 3", confirmedNote)

        rule.onNodeWithText("Batal").performClick()
        assertTrue(dismissed)
    }
}
