package id.oemah.nusantara.feature.cart

import app.cash.turbine.test
import id.oemah.nusantara.api.model.CartGroup
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.LineIssue
import id.oemah.nusantara.api.model.ValidatedLine
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.checkout.CheckoutRepository
import id.oemah.nusantara.feature.products.detail.testSeller
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val checkout = mockk<CheckoutRepository>()
    private val issues = mutableMapOf<Long, Pair<LineIssue, Int>>() // productId -> (issue, available stock)
    private val serverPrices = mutableMapOf<Long, Long>() // productId -> server unit price

    private val images = ImageSet("s", "m", "l")

    private fun line(id: Long, productId: Long, sellerId: Long, quantity: Int = 1, price: Long = 15_000) = CartLine(
        id = id, productId = productId, sellerId = sellerId, sellerName = "Penjual $sellerId", name = "Produk $productId",
        imageUrl = "s", unitPrice = Rupiah(price), quantity = quantity,
    )

    private fun build(lines: List<CartLine>): CartValidation {
        val groups = lines.groupBy { it.sellerId }.map { (sellerId, sellerLines) ->
            CartGroup(
                seller = testSeller().copy(id = sellerId, name = "Penjual $sellerId", pickupAddress = "Alamat $sellerId", openingHours = "Jam $sellerId"),
                lines = sellerLines.map { l ->
                    val unit = serverPrices[l.productId] ?: l.unitPrice.amount
                    val (issue, available) = issues[l.productId] ?: (null to 99)
                    ValidatedLine(
                        productId = l.productId, name = l.name, image = images, quantity = l.quantity,
                        unitPrice = unit, lineTotal = unit * l.quantity, availableStock = available, issue = issue,
                    )
                },
            )
        }
        return CartValidation(groups, groups.sumOf { g -> g.lines.sumOf { it.lineTotal } })
    }

    private fun setup(lines: List<CartLine>, loggedIn: Boolean = true): Pair<CartViewModel, FakeCartRepository> {
        val cart = FakeCartRepository(lines)
        coEvery { checkout.validate(any()) } answers { Result.success(build(firstArg())) }
        return CartViewModel(cart, checkout, FakeSessionStore(if (loggedIn) session() else null)) to cart
    }

    private suspend fun TestScope.settle() { advanceTimeBy(600); runCurrent() }

    @Test fun lines_are_grouped_by_seller_with_the_servers_pickup_details() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10), line(2, 2, 20), line(3, 3, 10)))

        settle()

        val groups = vm.state.value.groups
        assertEquals(listOf(10L, 20L), groups.map { it.sellerId })
        assertEquals(listOf(1L, 3L), groups.first().lines.map { it.line.productId })
        assertEquals("Alamat 10", groups.first().pickupAddress)
        assertEquals("Jam 20", groups.last().openingHours)
    }

    @Test fun an_empty_cart_is_the_empty_state_and_is_never_validated() = runTest {
        val (vm, _) = setup(emptyList())

        settle()

        assertTrue(vm.state.value.isEmpty)
        assertFalse(vm.state.value.canCheckout)
        coVerify(exactly = 0) { checkout.validate(any()) }
    }

    @Test fun the_cart_is_revalidated_after_changes_but_debounced() = runTest {
        val (vm, cart) = setup(listOf(line(1, 1, 10)))
        settle()
        coVerify(exactly = 1) { checkout.validate(any()) }

        vm.setQuantity(1, 2); advanceTimeBy(200)
        vm.setQuantity(1, 3); advanceTimeBy(400)
        coVerify(exactly = 1) { checkout.validate(any()) }

        advanceTimeBy(150); runCurrent()
        coVerify(exactly = 2) { checkout.validate(any()) }
        assertEquals(3, cart.current.single().quantity)
    }

    @Test fun a_changed_server_price_updates_the_snapshot_and_shows_a_notice() = runTest {
        serverPrices[1] = 16_000
        val (vm, cart) = setup(listOf(line(1, 1, 10, quantity = 2, price = 15_000)))

        settle()

        assertEquals(listOf(Triple(1L, "Produk 1", Rupiah(16_000))), cart.updatedSnapshots)
        assertEquals("Harga diperbarui", vm.state.value.notice)
        assertEquals(Rupiah(32_000), vm.state.value.total)

        vm.dismissNotice(); runCurrent()
        assertEquals(null, vm.state.value.notice)
    }

    @Test fun the_total_is_the_sum_of_the_validated_line_totals() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10, quantity = 2, price = 15_000), line(2, 2, 20, quantity = 1, price = 45_000)))

        settle()

        assertEquals(Rupiah(75_000), vm.state.value.total)
        assertTrue(vm.state.value.canCheckout)
    }

    @Test fun sold_out_and_unavailable_lines_block_checkout_until_removed() = runTest {
        issues[1] = LineIssue.OUT_OF_STOCK to 0
        issues[2] = LineIssue.PRODUCT_UNAVAILABLE to 0
        val (vm, _) = setup(listOf(line(1, 1, 10), line(2, 2, 20), line(3, 3, 20)))
        settle()
        assertFalse(vm.state.value.canCheckout)

        vm.remove(1); vm.remove(2); settle()
        issues.clear(); settle()

        assertTrue(vm.state.value.canCheckout)
    }

    @Test fun insufficient_stock_offers_to_adjust_to_the_available_quantity() = runTest {
        issues[1] = LineIssue.INSUFFICIENT_STOCK to 2
        val (vm, cart) = setup(listOf(line(1, 1, 10, quantity = 5)))
        settle()
        assertEquals(LineIssue.INSUFFICIENT_STOCK, vm.state.value.groups.single().lines.single().issue)
        assertFalse(vm.state.value.canCheckout)

        vm.adjustToAvailable(1); issues.clear(); settle()

        assertEquals(2, cart.current.single().quantity)
        assertTrue(vm.state.value.canCheckout)
    }

    @Test fun checkout_is_blocked_while_the_validation_is_stale() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10)))
        settle()
        assertTrue(vm.state.value.canCheckout)

        vm.setQuantity(1, 4); runCurrent()

        assertFalse(vm.state.value.canCheckout)
        settle()
        assertTrue(vm.state.value.canCheckout)
    }

    @Test fun offline_blocks_checkout_and_reports_the_error() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10)))
        coEvery { checkout.validate(any()) } returns Result.failure(AppError.Offline)

        vm.setQuantity(1, 2); settle()

        assertFalse(vm.state.value.canCheckout)
        assertEquals(AppError.Offline, vm.state.value.error)
        assertEquals(1, vm.state.value.groups.size) // the local cart is still shown
    }

    @Test fun checkout_while_logged_out_asks_for_login() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10)), loggedIn = false)
        settle()

        vm.events.test {
            vm.checkout(); runCurrent()
            assertEquals(CartEvent.LoginRequired, awaitItem())
        }
    }

    @Test fun checkout_when_valid_opens_the_checkout_screen() = runTest {
        val (vm, _) = setup(listOf(line(1, 1, 10)))
        settle()

        vm.events.test {
            vm.checkout(); runCurrent()
            assertEquals(CartEvent.OpenCheckout, awaitItem())
        }
    }

    @Test fun checkout_with_an_unresolved_issue_does_nothing() = runTest {
        issues[1] = LineIssue.OUT_OF_STOCK to 0
        val (vm, _) = setup(listOf(line(1, 1, 10)))
        settle()

        vm.events.test {
            vm.checkout(); runCurrent()
            expectNoEvents()
        }
    }
}
