package id.oemah.nusantara.feature.checkout

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.model.CartLineInput
import id.oemah.nusantara.api.model.CartRequest
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.CartLine
import id.oemah.nusantara.testing.testOrder
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

class CheckoutRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "checkout.preferences_pb") } }
    private val api = mockk<OrdersApi>()
    private fun repo() = DefaultCheckoutRepository(api, IdempotencyKeyStore(dataStore))

    @After fun stop() = scope.cancel()

    private val lines = listOf(
        CartLine(1, 11, 5, "S", "A", "s", Rupiah(15_000), 2),
        CartLine(2, 12, 6, "T", "B", "s", Rupiah(45_000), 1),
    )

    @Test fun validate_sends_the_cart_lines_as_product_and_quantity_only() = runBlocking {
        val sent = mutableListOf<CartRequest>()
        coEvery { api.validateCart(any()) } answers { sent += firstArg<CartRequest>(); Response.success(CartValidation(emptyList(), 0)) }

        repo().validate(lines).getOrThrow()

        assertEquals(listOf(CartLineInput(11, 2), CartLineInput(12, 1)), sent.single().lines)
    }

    @Test fun placing_an_order_sends_the_lines_and_an_idempotency_key() = runBlocking {
        val sent = mutableListOf<Pair<String, CartRequest>>()
        coEvery { api.createOrder(any(), any()) } answers { sent += firstArg<String>() to secondArg<CartRequest>(); Response.success(testOrder()) }

        val order = repo().placeOrder(lines).getOrThrow()

        assertEquals(1L, order.id)
        assertEquals(listOf(CartLineInput(11, 2), CartLineInput(12, 1)), sent.single().second.lines)
        assertNotEquals("", sent.single().first)
    }

    @Test fun retrying_after_a_network_failure_reuses_the_same_key() = runBlocking {
        val keys = mutableListOf<String>()
        coEvery { api.createOrder(any(), any()) } answers { keys += firstArg<String>(); throw IOException("timeout") }
        val repo = repo()

        val first = repo.placeOrder(lines)
        val second = repo.placeOrder(lines)

        assertEquals(AppError.Offline, first.exceptionOrNull())
        assertEquals(AppError.Offline, second.exceptionOrNull())
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
    }

    @Test fun the_key_survives_an_app_restart() = runBlocking {
        val keys = mutableListOf<String>()
        coEvery { api.createOrder(any(), any()) } answers { keys += firstArg<String>(); throw IOException("killed mid-request") }

        repo().placeOrder(lines)
        repo().placeOrder(lines) // a brand-new repository and key store over the same persisted data

        assertEquals(keys[0], keys[1])
    }

    @Test fun a_changed_cart_gets_a_new_key() = runBlocking {
        val keys = mutableListOf<String>()
        coEvery { api.createOrder(any(), any()) } answers { keys += firstArg<String>(); throw IOException("timeout") }
        val repo = repo()

        repo.placeOrder(lines)
        repo.placeOrder(lines.dropLast(1))

        assertNotEquals(keys[0], keys[1])
    }

    @Test fun success_clears_the_key_so_the_next_order_is_a_new_one() = runBlocking {
        val keys = mutableListOf<String>()
        coEvery { api.createOrder(any(), any()) } answers { keys += firstArg<String>(); Response.success(testOrder()) }
        val repo = repo()

        repo.placeOrder(lines).getOrThrow()
        repo.placeOrder(lines).getOrThrow()

        assertNotEquals(keys[0], keys[1])
    }
}
