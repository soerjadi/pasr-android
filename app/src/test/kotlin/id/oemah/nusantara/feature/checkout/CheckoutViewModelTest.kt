package id.oemah.nusantara.feature.checkout

import app.cash.turbine.test
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.cart.CartLine
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.testing.testOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val checkout = mockk<CheckoutRepository>()
    private fun line(id: Long, productId: Long = id) = CartLine(id, productId, 5, "Bu Siti", "P$productId", "s", Rupiah(15_000), 1)

    private fun viewModel(lines: List<CartLine> = listOf(line(1), line(2)), loggedIn: Boolean = true): Pair<CheckoutViewModel, FakeCartRepository> {
        val cart = FakeCartRepository(lines)
        coEvery { checkout.validate(any()) } returns Result.success(CartValidation(emptyList(), 30_000))
        return CheckoutViewModel(cart, checkout, FakeSessionStore(if (loggedIn) session() else null)) to cart
    }

    @Test fun the_review_loads_the_servers_validation() = runTest {
        val (vm, _) = viewModel(); runCurrent()

        assertEquals(30_000L, (vm.review.value as UiState.Content).value.total)
    }

    @Test fun a_validation_failure_is_an_error_state() = runTest {
        val (vm, _) = viewModel()
        coEvery { checkout.validate(any()) } returns Result.failure(AppError.Offline)

        vm.reload(); runCurrent()

        assertEquals(UiState.Error(AppError.Offline), vm.review.value)
    }

    @Test fun placing_an_order_succeeds_and_removes_only_the_ordered_lines() = runTest {
        val (vm, cart) = viewModel(); runCurrent()
        coEvery { checkout.placeOrder(any()) } coAnswers { cart.add(id.oemah.nusantara.feature.cart.CartProductSnapshot(99, 5, "Bu Siti", "Baru", "s", Rupiah(1_000))); Result.success(testOrder(id = 7)) }

        vm.placeOrder(); runCurrent()

        assertEquals(Submission.Placed(testOrder(id = 7)), vm.submission.value)
        assertEquals(listOf(99L), cart.current.map { it.productId }) // the line added meanwhile stays
    }

    @Test fun a_double_tap_sends_one_request() = runTest {
        val (vm, _) = viewModel(); runCurrent()
        val gate = CompletableDeferred<Unit>()
        coEvery { checkout.placeOrder(any()) } coAnswers { gate.await(); Result.success(testOrder()) }

        vm.placeOrder(); vm.placeOrder(); vm.placeOrder(); runCurrent()
        gate.complete(Unit); runCurrent()

        coVerify(exactly = 1) { checkout.placeOrder(any()) }
    }

    @Test fun a_network_failure_is_a_failed_state_and_retrying_sends_again() = runTest {
        val (vm, cart) = viewModel(); runCurrent()
        coEvery { checkout.placeOrder(any()) } returns Result.failure(AppError.Offline)

        vm.placeOrder(); runCurrent()
        assertEquals(Submission.Failed(AppError.Offline), vm.submission.value)
        assertEquals(2, cart.current.size) // nothing is removed on failure

        coEvery { checkout.placeOrder(any()) } returns Result.success(testOrder())
        vm.placeOrder(); runCurrent()

        assertTrue(vm.submission.value is Submission.Placed)
        coVerify(exactly = 2) { checkout.placeOrder(any()) }
    }

    @Test fun a_stock_conflict_returns_to_the_cart_and_keeps_the_cart_intact() = runTest {
        val (vm, cart) = viewModel(); runCurrent()
        coEvery { checkout.placeOrder(any()) } returns Result.failure(AppError.Conflict("INSUFFICIENT_STOCK", "x"))

        vm.events.test {
            vm.placeOrder(); runCurrent()
            assertEquals(CheckoutEvent.BackToCart("Stok berubah. Periksa keranjang Anda."), awaitItem())
        }
        assertEquals(Submission.Idle, vm.submission.value)
        assertEquals(2, cart.current.size)
    }

    @Test fun an_unavailable_product_is_treated_like_a_stock_conflict() = runTest {
        val (vm, _) = viewModel(); runCurrent()
        coEvery { checkout.placeOrder(any()) } returns Result.failure(AppError.Conflict("PRODUCT_UNAVAILABLE", "x"))

        vm.events.test {
            vm.placeOrder(); runCurrent()
            assertTrue(awaitItem() is CheckoutEvent.BackToCart)
        }
    }

    @Test fun placing_an_order_while_logged_out_asks_for_login_and_sends_nothing() = runTest {
        val (vm, _) = viewModel(loggedIn = false); runCurrent()

        vm.events.test {
            vm.placeOrder(); runCurrent()
            assertEquals(CheckoutEvent.LoginRequired, awaitItem())
        }
        coVerify(exactly = 0) { checkout.placeOrder(any()) }
    }

    @Test fun an_empty_cart_places_nothing() = runTest {
        val (vm, _) = viewModel(lines = emptyList()); runCurrent()

        vm.placeOrder(); runCurrent()

        coVerify(exactly = 0) { checkout.placeOrder(any()) }
        assertEquals(Submission.Idle, vm.submission.value)
    }
}
