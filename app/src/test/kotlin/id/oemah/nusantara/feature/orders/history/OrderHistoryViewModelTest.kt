package id.oemah.nusantara.feature.orders.history

import androidx.paging.PagingData
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.orders.OrderTab
import id.oemah.nusantara.feature.orders.OrdersRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderHistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val orders = mockk<OrdersRepository>()
    private val requested = mutableListOf<OrderTab>()

    private fun TestScope.viewModel(sessions: FakeSessionStore): OrderHistoryViewModel {
        every { orders.orders(any()) } answers { requested += firstArg<OrderTab>(); flowOf(PagingData.empty<Order>()) }
        val vm = OrderHistoryViewModel(orders, sessions)
        vm.orders.onEach { }.launchIn(backgroundScope) // collected, like the screen does
        runCurrent()
        return vm
    }

    @Test fun the_default_tab_is_all_and_requests_all_orders() = runTest {
        val vm = viewModel(FakeSessionStore(session()))

        assertEquals(OrderTab.ALL, vm.selectedTab.value)
        assertEquals(listOf(OrderTab.ALL), requested)
    }

    @Test fun selecting_a_tab_requests_that_tab() = runTest {
        val vm = viewModel(FakeSessionStore(session()))

        vm.select(OrderTab.READY); runCurrent()

        assertEquals(OrderTab.READY, vm.selectedTab.value)
        assertEquals(listOf(OrderTab.ALL, OrderTab.READY), requested)
    }

    @Test fun selecting_the_same_tab_again_does_not_reload() = runTest {
        val vm = viewModel(FakeSessionStore(session()))

        vm.select(OrderTab.ALL); runCurrent()

        assertEquals(1, requested.size)
    }

    @Test fun logged_out_does_not_call_the_api_and_reports_it() = runTest {
        val vm = viewModel(FakeSessionStore(null))

        assertFalse(vm.loggedIn.value)
        verify(exactly = 0) { orders.orders(any()) }
    }

    @Test fun logging_in_starts_loading_the_history() = runTest {
        val sessions = FakeSessionStore(null)
        val vm = viewModel(sessions)
        verify(exactly = 0) { orders.orders(any()) }

        sessions.save(session()); runCurrent()

        assertTrue(vm.loggedIn.value)
        assertEquals(listOf(OrderTab.ALL), requested)
    }

    @Test fun logging_out_stops_showing_orders() = runTest {
        val sessions = FakeSessionStore(session())
        val vm = viewModel(sessions)

        sessions.clear(); runCurrent()

        assertFalse(vm.loggedIn.value)
    }
}
