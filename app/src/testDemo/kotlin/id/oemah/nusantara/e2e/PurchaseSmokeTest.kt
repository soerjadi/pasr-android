package id.oemah.nusantara.e2e

import android.Manifest
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.test.FakeImageLoaderEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import id.oemah.nusantara.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Browse → add to cart → checkout (logging in on the way) → order placed → tracking, through the
 * real screens, view models, repositories and generated client, with the demo fixtures as server.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// A phone-sized window (the default 320x470 would push the product grid below the fold).
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class PurchaseSmokeTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() {
        hilt.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        // No real image requests, and no permission dialog after ordering.
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context)
                .components { add(FakeImageLoaderEngine.Builder().default(ColorImage(Color.Gray.toArgb())).build()) }
                .build(),
        )
        shadowOf(context as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Waits for [condition]; on timeout, fails with what the screen actually showed. */
    private fun await(what: String, timeoutMs: Long = 10_000, condition: () -> Boolean) {
        try {
            rule.waitUntil(timeoutMs, condition)
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Timed out waiting for $what. Screen was:\n" + rule.onRoot().printToString(60), e)
        }
    }

    private fun awaitText(text: String) = await("\"$text\"") { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitEnabled(text: String) =
        await("enabled \"$text\"") { rule.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }

    @Test fun purchase_flow_from_browse_to_tracking() {
        // First launch: skip the intro.
        awaitText("Lewati")
        rule.onNodeWithText("Lewati").performClick()

        // Home: open the first featured product.
        awaitText("Keripik Tempe")
        rule.onNodeWithText("Keripik Tempe").performClick()

        // Detail: add to cart, then open the cart.
        awaitText("Tambah ke Keranjang")
        rule.onNodeWithText("Tambah ke Keranjang").performClick()
        awaitText("Ditambahkan ke keranjang")
        rule.onNodeWithContentDescription("Buka keranjang").performClick()

        // Cart: validated by the server, then checkout. Logged out, so it asks for login first.
        awaitEnabled("Checkout")
        rule.onNodeWithText("Checkout").performClick()

        // Login: phone, then code. Any values are accepted by the fixtures.
        awaitText("Kirim Kode")
        rule.onNode(hasSetTextAction()).performTextInput("0812-3456-7890")
        rule.onNodeWithText("Kirim Kode").performClick()
        awaitText("Kirim ulang dalam 60 detik")
        rule.onNode(hasSetTextAction()).performTextInput("1234")
        rule.onNode(hasText("Masuk") and hasClickAction()).performClick() // the title is also "Masuk"

        // Back in the cart, now logged in.
        awaitEnabled("Checkout")
        rule.onNodeWithText("Checkout").performClick()

        // Checkout: pickup, cash at the store, and nothing about shipping.
        awaitText("Buat Pesanan")
        check(rule.onAllNodesWithText("Ambil di Toko").fetchSemanticsNodes().isNotEmpty()) { "no pickup block on checkout" }
        rule.onNodeWithText("Bayar di toko saat mengambil").assertExists()
        check(rule.onAllNodesWithText("Ongkir", substring = true).fetchSemanticsNodes().isEmpty()) { "shipping text on checkout" }
        rule.onNodeWithText("Buat Pesanan").performClick()

        // Confirmation.
        awaitText("Pesanan Berhasil!")
        rule.onNodeWithText("#NM-20261015-001").assertExists()
        rule.onNodeWithText("Lihat Pesanan").performClick()

        // Tracking: the order is ready for pickup.
        awaitText("Tracking Pesanan")
        awaitText("Siap diambil")
    }
}
