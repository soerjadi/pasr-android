package id.oemah.nusantara.feature.checkout

import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.model.CartLineInput
import id.oemah.nusantara.api.model.CartRequest
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.CartLine
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class CheckoutRepositoryTest {
    private val api = mockk<OrdersApi>()
    private val repo = DefaultCheckoutRepository(api)

    @Test fun validate_sends_the_cart_lines_as_product_and_quantity_only() = runTest {
        val request = slot<CartRequest>()
        coEvery { api.validateCart(capture(request)) } returns Response.success(CartValidation(emptyList(), 0))
        val lines = listOf(
            CartLine(1, 11, 5, "S", "A", "s", Rupiah(15_000), 2),
            CartLine(2, 12, 6, "T", "B", "s", Rupiah(45_000), 1),
        )

        repo.validate(lines).getOrThrow()

        assertEquals(listOf(CartLineInput(11, 2), CartLineInput(12, 1)), request.captured.lines)
    }
}
