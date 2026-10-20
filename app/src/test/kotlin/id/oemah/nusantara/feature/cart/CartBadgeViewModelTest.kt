package id.oemah.nusantara.feature.cart

import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CartBadgeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun the_badge_counts_cart_lines_and_follows_changes() = runTest {
        val cart = FakeCartRepository()
        val vm = CartBadgeViewModel(cart)
        runCurrent()
        assertEquals(0, vm.count.value)

        cart.add(CartProductSnapshot(1, 1, "S", "A", "s", Rupiah(1)), quantity = 5)
        cart.add(CartProductSnapshot(2, 1, "S", "B", "s", Rupiah(1)))
        runCurrent()

        assertEquals(2, vm.count.value)
    }
}
