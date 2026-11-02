package id.oemah.nusantara.feature.store

import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SellerHomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val myStore = mockk<MyStoreRepository>(relaxed = true)
    private val sellerRepo = mockk<SellerRepository>(relaxed = true)
    private val sessions = mockk<id.oemah.nusantara.core.session.SessionStore>(relaxed = true)

    private val storeFlow = MutableStateFlow<StoreSummary?>(null)

    private fun sampleSummary(isOpen: Boolean = true) = StoreSummaryResponse(
        isOpen = isOpen,
        closesAt = "17:00",
        newOrderCount = 2,
        acceptBy = "2026-10-15T03:30:00Z",
        ordersToday = 5,
        salesToday = 120_000,
        latestOrders = emptyList(),
    )

    private fun sampleStats(range: StoreStats.Range = StoreStats.Range.TODAY) = StoreStats(
        range = range,
        salesValue = 120_000,
        completedOrders = 5,
        daily = emptyList(),
        topProducts = emptyList(),
    )

    private fun createVm(): SellerHomeViewModel {
        coEvery { myStore.store } returns storeFlow
        return SellerHomeViewModel(myStore, sellerRepo, sessions)
    }

    @Test fun verified_store_loads_summary_and_stats_on_init() = runTest {
        coEvery { sellerRepo.getMyStoreSummary() } returns Result.success(sampleSummary())
        coEvery { sellerRepo.getMyStoreStats(MeApi.RangeGetMyStoreStats.TODAY) } returns Result.success(sampleStats())

        storeFlow.value = StoreSummary(1, "Warung Bu Sari", StoreStatus.VERIFIED, 0)
        val vm = createVm()
        runCurrent()

        val state = vm.state.value
        assertEquals(StoreStatus.VERIFIED, state.store?.status)
        assertTrue(state.summaryState is UiState.Content)
        assertEquals(5, (state.summaryState as UiState.Content<StoreSummaryResponse>).data.ordersToday)
        assertTrue(state.statsState is UiState.Content)
    }

    @Test fun pending_store_does_not_call_dashboard_apis() = runTest {
        storeFlow.value = StoreSummary(1, "Warung Bu Sari", StoreStatus.PENDING, 0)
        val vm = createVm()
        runCurrent()

        coVerify(exactly = 0) { sellerRepo.getMyStoreSummary() }
        coVerify(exactly = 0) { sellerRepo.getMyStoreStats(any()) }
    }

    @Test fun toggle_store_open_optimistically_flips_state_and_calls_api() = runTest {
        coEvery { sellerRepo.getMyStoreSummary() } returns Result.success(sampleSummary(isOpen = true))
        coEvery { sellerRepo.getMyStoreStats(any()) } returns Result.success(sampleStats())
        coEvery { sellerRepo.setMyStoreOpen(false) } returns Result.success(Unit)

        storeFlow.value = StoreSummary(1, "Warung Bu Sari", StoreStatus.VERIFIED, 0)
        val vm = createVm()
        runCurrent()

        vm.toggleStoreOpen()
        runCurrent()

        val content = vm.state.value.summaryState as UiState.Content<StoreSummaryResponse>
        assertFalse(content.data.isOpen)
        coVerify(exactly = 1) { sellerRepo.setMyStoreOpen(false) }
    }

    @Test fun toggle_store_open_reverts_on_failure() = runTest {
        coEvery { sellerRepo.getMyStoreSummary() } returns Result.success(sampleSummary(isOpen = true))
        coEvery { sellerRepo.getMyStoreStats(any()) } returns Result.success(sampleStats())
        coEvery { sellerRepo.setMyStoreOpen(false) } returns Result.failure(RuntimeException("Network error"))

        storeFlow.value = StoreSummary(1, "Warung Bu Sari", StoreStatus.VERIFIED, 0)
        val vm = createVm()
        runCurrent()

        vm.toggleStoreOpen()
        runCurrent()

        val content = vm.state.value.summaryState as UiState.Content<StoreSummaryResponse>
        assertTrue(content.data.isOpen) // reverted
        assertEquals("Gagal mengubah status toko. Silakan coba lagi.", vm.state.value.actionError)

        vm.dismissActionError()
        assertNull(vm.state.value.actionError)
    }

    @Test fun changing_range_fetches_corresponding_stats() = runTest {
        coEvery { sellerRepo.getMyStoreSummary() } returns Result.success(sampleSummary())
        coEvery { sellerRepo.getMyStoreStats(MeApi.RangeGetMyStoreStats.TODAY) } returns Result.success(sampleStats(StoreStats.Range.TODAY))
        coEvery { sellerRepo.getMyStoreStats(MeApi.RangeGetMyStoreStats.WEEK) } returns Result.success(sampleStats(StoreStats.Range.WEEK))

        storeFlow.value = StoreSummary(1, "Warung Bu Sari", StoreStatus.VERIFIED, 0)
        val vm = createVm()
        runCurrent()

        vm.selectStatsRange(MeApi.RangeGetMyStoreStats.WEEK)
        runCurrent()

        assertEquals(MeApi.RangeGetMyStoreStats.WEEK, vm.state.value.selectedRange)
        coVerify(exactly = 1) { sellerRepo.getMyStoreStats(MeApi.RangeGetMyStoreStats.WEEK) }
    }
}
