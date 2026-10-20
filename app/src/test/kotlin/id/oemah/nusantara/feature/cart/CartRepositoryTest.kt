package id.oemah.nusantara.feature.cart

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.cart.data.CartDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: CartDatabase
    private lateinit var repo: CartRepository

    private fun tempeh(price: Long = 15_000) = CartProductSnapshot(
        productId = 1, sellerId = 10, sellerName = "Bu Siti", name = "Keripik Tempe",
        imageUrl = "https://img/1_320.png", unitPrice = Rupiah(price),
    )

    private fun honey() = CartProductSnapshot(
        productId = 2, sellerId = 20, sellerName = "Pak Slamet", name = "Madu Hutan",
        imageUrl = "https://img/2_320.png", unitPrice = Rupiah(45_000),
    )

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(context, CartDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomCartRepository(db.cartDao())
    }

    @After fun close() = db.close()

    private suspend fun lines() = repo.lines.first()

    @Test fun adding_the_same_product_twice_yields_one_line_with_quantity_two() = runBlocking {
        repo.add(tempeh())
        repo.add(tempeh())

        val line = lines().single()
        assertEquals(2, line.quantity)
        assertEquals(Rupiah(15_000), line.unitPrice)
    }

    @Test fun lines_keep_the_order_they_were_added_in() = runBlocking {
        repo.add(tempeh()); repo.add(honey())

        assertEquals(listOf(1L, 2L), lines().map { it.productId })
    }

    @Test fun set_quantity_to_zero_or_less_removes_the_line() = runBlocking {
        repo.add(tempeh()); repo.add(honey())
        val (first, second) = lines()

        repo.setQuantity(first.id, 0)
        repo.setQuantity(second.id, -3)

        assertEquals(emptyList<CartLine>(), lines())
    }

    @Test fun set_quantity_clamps_to_99() = runBlocking {
        repo.add(tempeh())

        repo.setQuantity(lines().single().id, 500)

        assertEquals(99, lines().single().quantity)
    }

    @Test fun adding_beyond_99_clamps() = runBlocking {
        repo.add(tempeh(), quantity = 98)
        repo.add(tempeh(), quantity = 5)

        assertEquals(99, lines().single().quantity)
    }

    @Test fun add_requires_a_positive_quantity() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.add(tempeh(), quantity = 0) } }
    }

    @Test fun remove_all_removes_only_the_given_lines() = runBlocking {
        repo.add(tempeh()); repo.add(honey())
        val (first, _) = lines()

        repo.removeAll(listOf(first.id))

        assertEquals(listOf(2L), lines().map { it.productId })
    }

    @Test fun update_snapshot_changes_name_and_price_of_that_product_only() = runBlocking {
        repo.add(tempeh()); repo.add(honey())

        repo.updateSnapshot(productId = 1, name = "Keripik Tempe Pedas", unitPrice = Rupiah(16_000))

        val (first, second) = lines()
        assertEquals("Keripik Tempe Pedas", first.name)
        assertEquals(Rupiah(16_000), first.unitPrice)
        assertEquals("Madu Hutan", second.name)
        assertEquals(Rupiah(45_000), second.unitPrice)
    }

    @Test fun lines_survive_reopening_the_database() = runBlocking {
        val file = Room.databaseBuilder(context, CartDatabase::class.java, "reopen-test.db").allowMainThreadQueries().build()
        RoomCartRepository(file.cartDao()).add(tempeh(), quantity = 3)
        file.close()

        val reopened = Room.databaseBuilder(context, CartDatabase::class.java, "reopen-test.db").allowMainThreadQueries().build()
        val line = RoomCartRepository(reopened.cartDao()).lines.first().single()
        reopened.close()
        context.deleteDatabase("reopen-test.db")

        assertEquals(3, line.quantity)
        assertEquals("Keripik Tempe", line.name)
    }
}
