package id.oemah.nusantara.feature.orders.seller

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreOrderPage
import id.oemah.nusantara.api.model.StoreOrderPageCounts
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.testing.testOrderItem
import id.oemah.nusantara.ui.components.userMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SellerOrdersViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val sellerRepo = mockk<SellerRepository>()
    private val requestedTabs = mutableListOf<MeApi.TabListMyStoreOrders>()

    private fun sampleOrder(id: Long = 1, status: OrderStatus = OrderStatus.CREATED) = StoreOrder(
        id = id,
        number = "NM-20261015-%03d".format(id),
        status = status,
        createdAt = "2026-10-15T03:02:00Z",
        buyerName = "Budi",
        subtotal = 15_000,
        items = listOf(testOrderItem()),
        statusHistory = listOf(StatusChange(status, "2026-10-15T03:02:00Z")),
    )

    private fun sampleCounts() = StoreOrderPageCounts(new = 2, processing = 1, done = 5)

    private fun TestScope.createViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
    ): SellerOrdersViewModel {
        every { sellerRepo.myStoreOrders(any()) } answers {
            requestedTabs += firstArg<MeApi.TabListMyStoreOrders>()
            flowOf(PagingData.empty())
        }
        coEvery { sellerRepo.listMyStoreOrders(any(), any(), any()) } returns Result.success(
            StoreOrderPage(items = emptyList(), counts = sampleCounts(), nextCursor = null),
        )

        val vm = SellerOrdersViewModel(handle, sellerRepo)
        vm.orders.onEach { }.launchIn(backgroundScope)
        runCurrent()
        return vm
    }

    @Test
    fun initial_tab_defaults_to_NEW_and_loads_orders_and_counts() = runTest {
        val vm = createViewModel()

        assertEquals(MeApi.TabListMyStoreOrders.NEW, vm.state.value.selectedTab)
        assertEquals(sampleCounts(), vm.state.value.counts)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW), requestedTabs)
    }

    @Test
    fun initial_tab_can_be_configured_via_saved_state_handle() = runTest {
        val vm = createViewModel(SavedStateHandle(mapOf("initialTab" to "PROCESSING")))

        assertEquals(MeApi.TabListMyStoreOrders.PROCESSING, vm.state.value.selectedTab)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.PROCESSING), requestedTabs)
    }

    @Test
    fun selecting_a_different_tab_updates_state_and_requests_new_orders() = runTest {
        val vm = createViewModel()

        vm.selectTab(MeApi.TabListMyStoreOrders.DONE)
        runCurrent()

        assertEquals(MeApi.TabListMyStoreOrders.DONE, vm.state.value.selectedTab)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW, MeApi.TabListMyStoreOrders.DONE), requestedTabs)
    }

    @Test
    fun selecting_the_same_tab_does_not_reload() = runTest {
        val vm = createViewModel()

        vm.selectTab(MeApi.TabListMyStoreOrders.NEW)
        runCurrent()

        assertEquals(1, requestedTabs.size)
    }

    @Test
    fun acceptOrder_calls_repository_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val order = sampleOrder(10, OrderStatus.SELLER_PROCESSING)
        coEvery { sellerRepo.acceptMyStoreOrder(10L) } returns Result.success(order)

        vm.acceptOrder(10L)
        assertEquals(10L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify { sellerRepo.acceptMyStoreOrder(10L) }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW, MeApi.TabListMyStoreOrders.NEW), requestedTabs)
    }

    @Test
    fun acceptOrder_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        coEvery { sellerRepo.acceptMyStoreOrder(10L) } returns Result.failure(AppError.Offline)

        vm.acceptOrder(10L)
        runCurrent()

        coVerify { sellerRepo.acceptMyStoreOrder(10L) }
        assertNull(vm.state.value.actionInProgressId)
        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun rejectOrder_calls_repository_closes_dialog_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val order = sampleOrder(15)
        vm.openRejectDialog(order)
        assertEquals(order, vm.state.value.activeRejectOrder)

        coEvery {
            sellerRepo.rejectMyStoreOrder(15L, RejectRequest.Reason.OUT_OF_STOCK, "Habis")
        } returns Result.success(order.copy(status = OrderStatus.CANCELLED))

        vm.rejectOrder(15L, RejectRequest.Reason.OUT_OF_STOCK, "Habis")
        assertEquals(15L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify { sellerRepo.rejectMyStoreOrder(15L, RejectRequest.Reason.OUT_OF_STOCK, "Habis") }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.activeRejectOrder)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW, MeApi.TabListMyStoreOrders.NEW), requestedTabs)
    }

    @Test
    fun rejectOrder_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        coEvery {
            sellerRepo.rejectMyStoreOrder(15L, any(), any())
        } returns Result.failure(AppError.Server)

        vm.rejectOrder(15L, RejectRequest.Reason.OTHER, "Alasan lain")
        runCurrent()

        assertNull(vm.state.value.actionInProgressId)
        assertEquals(AppError.Server.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun readyOrder_calls_repository_closes_dialog_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val order = sampleOrder(20)
        vm.openReadyDialog(order)
        assertEquals(order, vm.state.value.activeReadyOrder)

        coEvery {
            sellerRepo.readyMyStoreOrder(20L, "Siap di meja depan")
        } returns Result.success(order.copy(status = OrderStatus.READY_FOR_PICKUP, note = "Siap di meja depan"))

        vm.readyOrder(20L, "Siap di meja depan")
        assertEquals(20L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify { sellerRepo.readyMyStoreOrder(20L, "Siap di meja depan") }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.activeReadyOrder)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW, MeApi.TabListMyStoreOrders.NEW), requestedTabs)
    }

    @Test
    fun readyOrder_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        coEvery {
            sellerRepo.readyMyStoreOrder(20L, any())
        } returns Result.failure(AppError.Offline)

        vm.readyOrder(20L, null)
        runCurrent()

        assertNull(vm.state.value.actionInProgressId)
        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun completeOrder_calls_repository_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val order = sampleOrder(25)
        coEvery { sellerRepo.completeMyStoreOrder(25L) } returns Result.success(order.copy(status = OrderStatus.COMPLETED))

        vm.completeOrder(25L)
        assertEquals(25L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify { sellerRepo.completeMyStoreOrder(25L) }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf(MeApi.TabListMyStoreOrders.NEW, MeApi.TabListMyStoreOrders.NEW), requestedTabs)
    }

    @Test
    fun completeOrder_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        coEvery { sellerRepo.completeMyStoreOrder(25L) } returns Result.failure(AppError.Server)

        vm.completeOrder(25L)
        runCurrent()

        assertNull(vm.state.value.actionInProgressId)
        assertEquals(AppError.Server.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun dialog_and_error_dismissal() = runTest {
        val vm = createViewModel()
        val order = sampleOrder(30)

        vm.openRejectDialog(order)
        assertEquals(order, vm.state.value.activeRejectOrder)
        vm.closeRejectDialog()
        assertNull(vm.state.value.activeRejectOrder)

        vm.openReadyDialog(order)
        assertEquals(order, vm.state.value.activeReadyOrder)
        vm.closeReadyDialog()
        assertNull(vm.state.value.activeReadyOrder)

        coEvery { sellerRepo.acceptMyStoreOrder(30L) } returns Result.failure(AppError.Offline)
        vm.acceptOrder(30L)
        runCurrent()
        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)

        vm.dismissError()
        assertNull(vm.state.value.actionError)
    }
}
