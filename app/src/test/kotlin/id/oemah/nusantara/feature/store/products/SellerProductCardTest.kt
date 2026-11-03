package id.oemah.nusantara.feature.store.products

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerProductCardTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleProduct(
        id: Long = 1L,
        name: String = "Sambal Bawang Mbok Galak",
        price: Long = 25000L,
        stock: Int = 15,
        description: String = "Sambal bawang pedas nikmat",
        status: SellerProduct.Status = SellerProduct.Status.AVAILABLE,
        images: List<ImageSet> = listOf(ImageSet("s", "m", "l")),
    ) = SellerProduct(
        id = id,
        name = name,
        price = price,
        stock = stock,
        description = description,
        status = status,
        images = images,
    )

    @Test
    fun displays_product_details_and_available_status_badge() {
        val product = sampleProduct(status = SellerProduct.Status.AVAILABLE)

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = {},
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithText("Sambal Bawang Mbok Galak").assertIsDisplayed()
        rule.onNodeWithText("Rp 25.000").assertIsDisplayed()
        rule.onNodeWithText("Stok: 15").assertIsDisplayed()
        rule.onNodeWithText("Tersedia").assertIsDisplayed()
        rule.onNodeWithText("Ubah").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun displays_out_of_stock_status_badge() {
        val product = sampleProduct(stock = 0, status = SellerProduct.Status.OUT_OF_STOCK)

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = {},
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithText("Habis").assertIsDisplayed()
    }

    @Test
    fun displays_hidden_status_badge() {
        val product = sampleProduct(status = SellerProduct.Status.HIDDEN)

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = {},
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithText("Diarsipkan").assertIsDisplayed()
    }

    @Test
    fun clicking_card_triggers_onEdit() {
        val product = sampleProduct()
        var editCalled = false

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = { editCalled = true },
                    onUpdateStatus = {},
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithText("Sambal Bawang Mbok Galak").performClick()
        assertTrue(editCalled)
    }

    @Test
    fun clicking_ubah_button_triggers_onEdit() {
        val product = sampleProduct()
        var editCalled = false

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = { editCalled = true },
                    onUpdateStatus = {},
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithText("Ubah").performClick()
        assertTrue(editCalled)
    }

    @Test
    fun available_product_overflow_menu_shows_mark_out_of_stock_and_hidden_and_delete() {
        val product = sampleProduct(status = SellerProduct.Status.AVAILABLE)
        var updatedStatus: UpdateProductRequest.Status? = null

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = { updatedStatus = it },
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithContentDescription("Opsi lainnya").performClick()

        rule.onNodeWithText("Tandai Tersedia").assertDoesNotExist()
        rule.onNodeWithText("Tandai Habis").assertIsDisplayed()
        rule.onNodeWithText("Arsipkan Produk").assertIsDisplayed()
        rule.onNodeWithText("Hapus Produk").assertIsDisplayed()

        rule.onNodeWithText("Tandai Habis").performClick()
        assertEquals(UpdateProductRequest.Status.OUT_OF_STOCK, updatedStatus)
    }

    @Test
    fun out_of_stock_product_overflow_menu_shows_mark_available() {
        val product = sampleProduct(status = SellerProduct.Status.OUT_OF_STOCK)
        var updatedStatus: UpdateProductRequest.Status? = null

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = { updatedStatus = it },
                    onDelete = {},
                )
            }
        }

        rule.onNodeWithContentDescription("Opsi lainnya").performClick()

        rule.onNodeWithText("Tandai Habis").assertDoesNotExist()
        rule.onNodeWithText("Tandai Tersedia").assertIsDisplayed().performClick()
        assertEquals(UpdateProductRequest.Status.AVAILABLE, updatedStatus)
    }

    @Test
    fun hidden_product_overflow_menu_shows_delete_and_triggers_onDelete() {
        val product = sampleProduct(status = SellerProduct.Status.HIDDEN)
        var deleteCalled = false

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = {},
                    onDelete = { deleteCalled = true },
                )
            }
        }

        rule.onNodeWithContentDescription("Opsi lainnya").performClick()

        rule.onNodeWithText("Arsipkan Produk").assertDoesNotExist()
        rule.onNodeWithText("Hapus Produk").assertIsDisplayed().performClick()
        assertTrue(deleteCalled)
    }

    @Test
    fun loading_state_disables_buttons_and_shows_loading_indicator() {
        val product = sampleProduct()

        rule.setContent {
            NusantaraTheme {
                SellerProductCard(
                    product = product,
                    onEdit = {},
                    onUpdateStatus = {},
                    onDelete = {},
                    isLoading = true,
                )
            }
        }

        rule.onNodeWithText("Ubah").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Opsi lainnya").assertIsNotEnabled()
        rule.onNodeWithTag("SellerProductCard_Loading").assertIsDisplayed()
    }

    @Test
    fun delete_product_dialog_displays_info_and_triggers_confirm_and_dismiss() {
        var confirmed = false
        var dismissed = false

        rule.setContent {
            NusantaraTheme {
                DeleteProductDialog(
                    productName = "Sambal Bawang Mbok Galak",
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        rule.onNodeWithText("Hapus Produk?").assertIsDisplayed()
        rule.onNodeWithText("Sambal Bawang Mbok Galak").assertIsDisplayed()
        rule.onNodeWithText("Produk yang dihapus tidak dapat dilihat atau dibeli oleh pelanggan.").assertIsDisplayed()

        rule.onNodeWithText("Hapus").assertIsEnabled().performClick()
        assertTrue(confirmed)

        rule.onNodeWithText("Batal").assertIsEnabled().performClick()
        assertTrue(dismissed)
    }
}
