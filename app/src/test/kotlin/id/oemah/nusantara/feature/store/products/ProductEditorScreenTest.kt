package id.oemah.nusantara.feature.store.products

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.ui.theme.NusantaraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductEditorScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun header_shows_add_title_and_triggers_back() {
        var backClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(isEditMode = false),
                    onBack = { backClicked = true },
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tambah Produk").assertIsDisplayed()
        rule.onNodeWithContentDescription("Kembali").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun header_shows_edit_title_in_edit_mode() {
        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(isEditMode = true),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Ubah Produk").assertIsDisplayed()
    }

    @Test
    fun photo_container_triggers_onPickPhoto_when_no_image() {
        var pickPhotoClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(imageUrl = null, isUploadingImage = false),
                    onBack = {},
                    onPickPhoto = { pickPhotoClicked = true },
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("+ Tambah Foto").performClick()
        assertTrue(pickPhotoClicked)
    }

    @Test
    fun photo_section_shows_uploading_state() {
        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(isUploadingImage = true),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Mengunggah foto...").assertIsDisplayed()
    }

    @Test
    fun photo_section_shows_preview_and_buttons_when_image_present() {
        var changePhotoClicked = false
        var removePhotoClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        imageUrl = "https://example.com/product.jpg",
                        isUploadingImage = false,
                    ),
                    onBack = {},
                    onPickPhoto = { changePhotoClicked = true },
                    onRemovePhoto = { removePhotoClicked = true },
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Ganti Foto").performClick()
        assertTrue(changePhotoClicked)

        rule.onNodeWithContentDescription("Hapus Foto").performClick()
        assertTrue(removePhotoClicked)
    }

    @Test
    fun form_inputs_wire_changes_correctly() {
        var nameInput = ""
        var priceInput = ""
        var stockInput = ""
        var stockStep = 0
        var descInput = ""

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        name = "",
                        price = "",
                        stock = "10",
                        description = "",
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = { nameInput = it },
                    onPriceChange = { priceInput = it },
                    onStockChange = { stockInput = it },
                    onStepStock = { stockStep = it },
                    onDescriptionChange = { descInput = it },
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithTag("ProductEditor_NameField").performTextInput("Sambal Teri")
        assertEquals("Sambal Teri", nameInput)

        rule.onNodeWithTag("ProductEditor_PriceField").performTextInput("25000")
        assertEquals("25000", priceInput)

        rule.onNodeWithTag("ProductEditor_StockField").performTextReplacement("15")
        assertEquals("15", stockInput)

        rule.onNodeWithText("−").performClick()
        assertEquals(-1, stockStep)

        rule.onNodeWithText("+").performClick()
        assertEquals(1, stockStep)

        rule.onNodeWithTag("ProductEditor_DescriptionField").performTextInput("Sambal pedas lezat")
        assertEquals("Sambal pedas lezat", descInput)
    }

    @Test
    fun status_selector_is_hidden_in_add_mode() {
        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(isEditMode = false),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Status Produk").assertDoesNotExist()
    }

    @Test
    fun status_selector_displays_in_edit_mode_and_triggers_callback() {
        var selectedStatus: SellerProduct.Status? = null

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        isEditMode = true,
                        status = SellerProduct.Status.AVAILABLE,
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = { selectedStatus = it },
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Status Produk").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Tersedia").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Habis").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Diarsipkan").performScrollTo().assertIsDisplayed()

        rule.onNodeWithText("Habis").performClick()
        assertEquals(SellerProduct.Status.OUT_OF_STOCK, selectedStatus)

        rule.onNodeWithText("Diarsipkan").performClick()
        assertEquals(SellerProduct.Status.HIDDEN, selectedStatus)
    }

    @Test
    fun submit_button_enabled_only_when_form_is_valid() {
        var submitClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        isEditMode = false,
                        name = "Kue Nastar",
                        price = "50000",
                        stock = "10",
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = { submitClicked = true },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Simpan Produk").performClick()
        assertTrue(submitClicked)
    }

    @Test
    fun submit_button_disabled_when_form_invalid() {
        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        isEditMode = false,
                        name = "",
                        price = "",
                        stock = "",
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithTag("ProductEditor_SubmitButton").assertIsNotEnabled()
    }

    @Test
    fun edit_mode_shows_update_title_and_delete_button_which_opens_dialog() {
        var openDeleteClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        isEditMode = true,
                        name = "Kripik Singkong",
                        price = "15000",
                        stock = "50",
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = { openDeleteClicked = true },
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Perbarui Produk").assertIsDisplayed()
        rule.onNodeWithTag("ProductEditor_DeleteButton").performClick()
        assertTrue(openDeleteClicked)
    }

    @Test
    fun delete_dialog_triggers_confirm_and_dismiss() {
        var confirmDeleteClicked = false
        var closeDeleteClicked = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        isEditMode = true,
                        name = "Kripik Singkong",
                        showDeleteDialog = true,
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = { closeDeleteClicked = true },
                    onConfirmDelete = { confirmDeleteClicked = true },
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Hapus Produk?").assertIsDisplayed()
        rule.onNodeWithText("Batal").performClick()
        assertTrue(closeDeleteClicked)

        rule.onNodeWithText("Hapus").performClick()
        assertTrue(confirmDeleteClicked)
    }

    @Test
    fun error_snackbar_displays_and_dismisses() {
        var errorDismissed = false

        rule.setContent {
            NusantaraTheme {
                ProductEditorContent(
                    state = ProductEditorState(
                        error = "Gagal menyimpan produk",
                    ),
                    onBack = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onNameChange = {},
                    onPriceChange = {},
                    onStockChange = {},
                    onStepStock = {},
                    onDescriptionChange = {},
                    onStatusChange = {},
                    onSubmit = {},
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onConfirmDelete = {},
                    onDismissError = { errorDismissed = true },
                )
            }
        }

        rule.onNodeWithText("Gagal menyimpan produk").assertIsDisplayed()
        assertTrue(errorDismissed)
        rule.mainClock.advanceTimeBy(5000)
    }
}
