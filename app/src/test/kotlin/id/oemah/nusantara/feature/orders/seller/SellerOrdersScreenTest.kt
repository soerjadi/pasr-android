package id.oemah.nusantara.feature.orders.seller

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreOrderPageCounts
import id.oemah.nusantara.ui.theme.NusantaraTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerOrdersScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun states(refresh: LoadState = LoadState.NotLoading(true)) =
        LoadStates(refresh, LoadState.NotLoading(true), LoadState.NotLoading(true))

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
    fun renders_three_tabs_with_titles_and_badges() {
        val state = SellerOrdersState(
            selectedTab = MeApi.TabListMyStoreOrders.NEW,
            counts = StoreOrderPageCounts(new = 3, processing = 1, done = 0),
        )

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = state,
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Baru").assertIsDisplayed()
        rule.onNodeWithText("Diproses").assertIsDisplayed()
        rule.onNodeWithText("Selesai").assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
        rule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun selecting_tab_triggers_callback() {
        var selectedTab: MeApi.TabListMyStoreOrders? = null
        val state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.NEW)

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = state,
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = { selectedTab = it },
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Diproses").performClick()
        assertEquals(MeApi.TabListMyStoreOrders.PROCESSING, selectedTab)

        rule.onNodeWithText("Selesai").performClick()
        assertEquals(MeApi.TabListMyStoreOrders.DONE, selectedTab)
    }

    @Test
    fun empty_state_renders_message_for_new_tab() {
        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.NEW),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Belum ada pesanan baru").assertIsDisplayed()
    }

    @Test
    fun empty_state_renders_message_for_processing_tab() {
        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.PROCESSING),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tidak ada pesanan yang sedang diproses").assertIsDisplayed()
    }

    @Test
    fun empty_state_renders_message_for_done_tab() {
        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.DONE),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Belum ada riwayat pesanan selesai").assertIsDisplayed()
    }

    @Test
    fun skeleton_renders_while_initial_page_loads() {
        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.NEW),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states(LoadState.Loading))).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithTag("skeleton").assertIsDisplayed()
    }

    @Test
    fun orders_list_renders_cards_and_wires_callbacks() {
        val order = sampleOrder(id = 202L, number = "NM-20261103-202")
        val data = PagingData.from(listOf(order), sourceLoadStates = states())
        var openedOrderId: Long? = null
        var acceptedOrderId: Long? = null
        var rejectDialogOrder: StoreOrder? = null

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(selectedTab = MeApi.TabListMyStoreOrders.NEW),
                    orders = flowOf(data).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = { openedOrderId = it },
                    onSelectTab = {},
                    onAcceptOrder = { acceptedOrderId = it },
                    onOpenRejectDialog = { rejectDialogOrder = it },
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("NM-20261103-202").assertIsDisplayed()
        rule.onNodeWithText("Budi Santoso").assertIsDisplayed()

        // Accept button
        rule.onNodeWithText("Terima Pesanan").performClick()
        assertEquals(202L, acceptedOrderId)

        // Reject button
        rule.onNodeWithText("Tolak").performClick()
        assertEquals(order, rejectDialogOrder)

        // Click card
        rule.onNodeWithText("NM-20261103-202").performClick()
        assertEquals(202L, openedOrderId)
    }

    @Test
    fun reject_dialog_displayed_when_active() {
        val order = sampleOrder(id = 303L)
        var rejectedOrderId: Long? = null
        var rejectedReason: RejectRequest.Reason? = null

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(activeRejectOrder = order),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { id, reason, _ ->
                        rejectedOrderId = id
                        rejectedReason = reason
                    },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tolak Pesanan").assertIsDisplayed()
        rule.onNodeWithText("Toko sedang sibuk / tutup").performClick()
        rule.onNodeWithText("Konfirmasi Tolak").performClick()

        assertEquals(303L, rejectedOrderId)
        assertEquals(RejectRequest.Reason.BUSY_OR_CLOSED, rejectedReason)
    }

    @Test
    fun ready_dialog_displayed_when_active() {
        val order = sampleOrder(id = 404L)
        var readiedOrderId: Long? = null

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(activeReadyOrder = order),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { id, _ -> readiedOrderId = id },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Pesanan Siap Diambil").assertIsDisplayed()
        rule.onNodeWithText("Tandai Siap").performClick()

        assertEquals(404L, readiedOrderId)
    }

    @Test
    fun action_error_snackbar_displays_and_dismisses() {
        var errorDismissed = false

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(actionError = "Gagal memproses pesanan"),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = { errorDismissed = true },
                )
            }
        }

        rule.onNodeWithText("Gagal memproses pesanan").assertIsDisplayed()
        assertTrue(errorDismissed)
        rule.mainClock.advanceTimeBy(5000)
    }

    @Test
    fun screen_header_renders_title_and_triggers_back() {
        var backPressed = false

        rule.setContent {
            NusantaraTheme {
                SellerOrdersContent(
                    state = SellerOrdersState(),
                    orders = flowOf(PagingData.empty<StoreOrder>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = { backPressed = true },
                    onOpenOrder = {},
                    onSelectTab = {},
                    onAcceptOrder = {},
                    onOpenRejectDialog = {},
                    onCloseRejectDialog = {},
                    onRejectOrder = { _, _, _ -> },
                    onOpenReadyDialog = {},
                    onCloseReadyDialog = {},
                    onReadyOrder = { _, _ -> },
                    onCompleteOrder = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Pesanan Masuk").assertIsDisplayed()
        rule.onNodeWithContentDescription("Kembali").performClick()
        assertTrue(backPressed)
    }
}
