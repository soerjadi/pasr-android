package id.oemah.nusantara.feature.checkout

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.CartLine
import java.io.File
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

class IdempotencyKeyStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "checkout.preferences_pb") } }

    @After fun stop() = scope.cancel()

    private fun line(productId: Long, quantity: Int, id: Long = productId) =
        CartLine(id, productId, 1, "S", "P$productId", "s", Rupiah(1_000), quantity)

    @Test fun the_same_cart_in_any_order_gets_the_same_key() = runBlocking {
        val store = IdempotencyKeyStore(dataStore)

        val a = store.keyFor(listOf(line(1, 2), line(2, 1)))
        val b = store.keyFor(listOf(line(2, 1), line(1, 2)))

        assertEquals(a, b)
    }

    @Test fun a_changed_quantity_or_product_gets_a_new_key() = runBlocking {
        val store = IdempotencyKeyStore(dataStore)
        val original = store.keyFor(listOf(line(1, 2)))

        val changedQuantity = store.keyFor(listOf(line(1, 3)))
        val otherProduct = store.keyFor(listOf(line(2, 3)))

        assertNotEquals(original, changedQuantity)
        assertNotEquals(changedQuantity, otherProduct)
    }

    @Test fun the_key_survives_a_restart() = runBlocking {
        val before = IdempotencyKeyStore(dataStore).keyFor(listOf(line(1, 2)))

        val afterRestart = IdempotencyKeyStore(dataStore).keyFor(listOf(line(1, 2)))

        assertEquals(before, afterRestart)
    }

    @Test fun clearing_forgets_the_key() = runBlocking {
        val store = IdempotencyKeyStore(dataStore)
        val before = store.keyFor(listOf(line(1, 2)))

        store.clear()

        assertNotEquals(before, store.keyFor(listOf(line(1, 2))))
    }

    @Test fun keys_are_not_blank() = runBlocking {
        assertNotEquals("", IdempotencyKeyStore(dataStore).keyFor(listOf(line(1, 1))))
    }
}
