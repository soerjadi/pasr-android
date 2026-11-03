package id.oemah.nusantara.feature.orders.seller

import androidx.lifecycle.SavedStateHandle
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SellerOrderDetailViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val sellerRepo = mockk<SellerRepository>()

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
        subtotal = 35_000L,
        items = listOf(
            OrderItem(
                id = 1L,
                productId = 10L,
                name = "Nasi Liwet Solo",
                image = ImageSet("s", "m", "l"),
                quantity = 1,
                unitPrice = 35_000L,
                lineTotal = 35_000L,
                reviewed = false,
            ),
        ),
        statusHistory = listOf(StatusChange(status, "2026-11-03T10:00:00Z")),
        acceptBy = acceptBy,
        note = note,
    )

    private fun TestScope.createViewModel(
        handle: SavedStateHandle = SavedStateHandle(mapOf("orderId" to 42L)),
    ): SellerOrderDetailViewModel {
        val vm = SellerOrderDetailViewModel(handle, sellerRepo)
        runCurrent()
        return vm
    }

    @Test
    fun initialization_loads_order_by_orderId_from_savedStateHandle() = runTest {
        val expected = sampleOrder(id = 42L)
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(expected)

        val vm = createViewModel()

        coVerify(exactly = 1) { sellerRepo.getMyStoreOrder(42L) }
        assertTrue(vm.state.value.orderState is UiState.Content)
        assertEquals(expected, (vm.state.value.orderState as UiState.Content).value)
        assertFalse(vm.state.value.isActionInProgress)
        assertNull(vm.state.value.actionError)
    }

    @Test
    fun initialization_failure_sets_orderState_to_error() = runTest {
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.failure(AppError.Offline)

        val vm = createViewModel()

        assertTrue(vm.state.value.orderState is UiState.Error)
    }

    @Test
    fun acceptOrder_success_updates_orderState_and_clears_progress() = runTest {
        val initial = sampleOrder(id = 42L, status = OrderStatus.CREATED)
        val updated = initial.copy(status = OrderStatus.SELLER_PROCESSING)
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(initial)
        coEvery { sellerRepo.acceptMyStoreOrder(42L) } returns Result.success(updated)

        val vm = createViewModel()
        vm.acceptOrder()
        runCurrent()

        coVerify(exactly = 1) { sellerRepo.acceptMyStoreOrder(42L) }
        assertEquals(UiState.Content(updated), vm.state.value.orderState)
        assertFalse(vm.state.value.isActionInProgress)
        assertNull(vm.state.value.actionError)
    }

    @Test
    fun acceptOrder_failure_sets_actionError_and_resets_progress() = runTest {
        val initial = sampleOrder(id = 42L)
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(initial)
        coEvery { sellerRepo.acceptMyStoreOrder(42L) } returns Result.failure(AppError.Server)

        val vm = createViewModel()
        vm.acceptOrder()
        runCurrent()

        assertEquals(UiState.Content(initial), vm.state.value.orderState)
        assertFalse(vm.state.value.isActionInProgress)
        assertEquals(AppError.Server.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun rejectOrder_success_updates_orderState_closes_dialog_and_clears_progress() = runTest {
        val initial = sampleOrder(id = 42L, status = OrderStatus.CREATED)
        val updated = initial.copy(status = OrderStatus.CANCELLED)
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(initial)
        coEvery {
            sellerRepo.rejectMyStoreOrder(42L, RejectRequest.Reason.OUT_OF_STOCK, "Stok habis")
        } returns Result.success(updated)

        val vm = createViewModel()
        vm.openRejectDialog()
        assertTrue(vm.state.value.showRejectDialog)

        vm.rejectOrder(RejectRequest.Reason.OUT_OF_STOCK, "Stok habis")
        runCurrent()

        coVerify(exactly = 1) {
            sellerRepo.rejectMyStoreOrder(42L, RejectRequest.Reason.OUT_OF_STOCK, "Stok habis")
        }
        assertEquals(UiState.Content(updated), vm.state.value.orderState)
        assertFalse(vm.state.value.showRejectDialog)
        assertFalse(vm.state.value.isActionInProgress)
    }

    @Test
    fun readyOrder_success_updates_orderState_closes_dialog_and_clears_progress() = runTest {
        val initial = sampleOrder(id = 42L, status = OrderStatus.SELLER_PROCESSING)
        val updated = initial.copy(status = OrderStatus.READY_FOR_PICKUP, note = "Siap di meja")
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(initial)
        coEvery { sellerRepo.readyMyStoreOrder(42L, "Siap di meja") } returns Result.success(updated)

        val vm = createViewModel()
        vm.openReadyDialog()
        assertTrue(vm.state.value.showReadyDialog)

        vm.readyOrder("Siap di meja")
        runCurrent()

        coVerify(exactly = 1) { sellerRepo.readyMyStoreOrder(42L, "Siap di meja") }
        assertEquals(UiState.Content(updated), vm.state.value.orderState)
        assertFalse(vm.state.value.showReadyDialog)
        assertFalse(vm.state.value.isActionInProgress)
    }

    @Test
    fun completeOrder_success_updates_orderState_and_clears_progress() = runTest {
        val initial = sampleOrder(id = 42L, status = OrderStatus.READY_FOR_PICKUP)
        val updated = initial.copy(status = OrderStatus.COMPLETED)
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(initial)
        coEvery { sellerRepo.completeMyStoreOrder(42L) } returns Result.success(updated)

        val vm = createViewModel()
        vm.completeOrder()
        runCurrent()

        coVerify(exactly = 1) { sellerRepo.completeMyStoreOrder(42L) }
        assertEquals(UiState.Content(updated), vm.state.value.orderState)
        assertFalse(vm.state.value.isActionInProgress)
    }

    @Test
    fun dialog_controls_and_error_dismissal() = runTest {
        coEvery { sellerRepo.getMyStoreOrder(42L) } returns Result.success(sampleOrder(42L))

        val vm = createViewModel()

        vm.openRejectDialog()
        assertTrue(vm.state.value.showRejectDialog)
        vm.closeRejectDialog()
        assertFalse(vm.state.value.showRejectDialog)

        vm.openReadyDialog()
        assertTrue(vm.state.value.showReadyDialog)
        vm.closeReadyDialog()
        assertFalse(vm.state.value.showReadyDialog)

        coEvery { sellerRepo.acceptMyStoreOrder(42L) } returns Result.failure(AppError.Offline)
        vm.acceptOrder()
        runCurrent()
        assertTrue(vm.state.value.actionError != null)

        vm.dismissError()
        assertNull(vm.state.value.actionError)
    }
}
