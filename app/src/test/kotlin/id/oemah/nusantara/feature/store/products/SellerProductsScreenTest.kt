package id.oemah.nusantara.feature.store.products

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.SellerProductPageCounts
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.ui.theme.NusantaraTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerProductsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun states(refresh: LoadState = LoadState.NotLoading(true)) =
        LoadStates(refresh, LoadState.NotLoading(true), LoadState.NotLoading(true))

    private fun sampleProduct(
        id: Long = 101L,
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
    fun renders_four_tabs_with_titles_and_badges() {
        val state = SellerProductsState(
            selectedStatus = null,
            counts = SellerProductPageCounts(all = 12, available = 8, outOfStock = 3, hidden = 1),
        )

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = state,
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Semua").assertIsDisplayed()
        rule.onNodeWithText("12").assertIsDisplayed()
        rule.onNodeWithText("Tersedia").assertIsDisplayed()
        rule.onNodeWithText("8").assertIsDisplayed()
        rule.onNodeWithText("Habis").assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
        rule.onNodeWithText("Diarsipkan").assertIsDisplayed()
        rule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun tab_selection_invokes_onSelectStatus() {
        val selectedStatuses = mutableListOf<MeApi.StatusListMyProducts?>()

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(selectedStatus = null),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = { selectedStatuses.add(it) },
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tersedia").performClick()
        rule.onNodeWithText("Habis").performClick()
        rule.onNodeWithText("Diarsipkan").performClick()
        rule.onNodeWithText("Semua").performClick()

        assertEquals(
            listOf(
                MeApi.StatusListMyProducts.AVAILABLE,
                MeApi.StatusListMyProducts.OUT_OF_STOCK,
                MeApi.StatusListMyProducts.HIDDEN,
                null,
            ),
            selectedStatuses,
        )
    }

    @Test
    fun empty_state_displays_for_all_status() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(selectedStatus = null),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Belum ada produk yang terdaftar").assertIsDisplayed()
    }

    @Test
    fun empty_state_displays_for_available_status() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(selectedStatus = MeApi.StatusListMyProducts.AVAILABLE),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tidak ada produk yang sedang tersedia").assertIsDisplayed()
    }

    @Test
    fun empty_state_displays_for_out_of_stock_status() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(selectedStatus = MeApi.StatusListMyProducts.OUT_OF_STOCK),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tidak ada produk yang stoknya habis").assertIsDisplayed()
    }

    @Test
    fun empty_state_displays_for_hidden_status() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(selectedStatus = MeApi.StatusListMyProducts.HIDDEN),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Tidak ada produk yang diarsipkan").assertIsDisplayed()
    }

    @Test
    fun skeleton_renders_while_initial_page_loads() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states(LoadState.Loading))).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithTag("skeleton").assertIsDisplayed()
    }

    @Test
    fun initial_load_error_displays_error_panel() {
        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states(LoadState.Error(AppError.Conflict(code = "ERR", message = "Gagal memuat produk"))))).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Gagal memuat produk").assertIsDisplayed()
        rule.onNodeWithText("Coba lagi").assertIsDisplayed()
    }

    @Test
    fun fab_click_invokes_onAddProduct() {
        var addProductClicked = false

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = { addProductClicked = true },
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithTag("SellerProducts_AddFab").performClick()
        assertTrue(addProductClicked)
    }

    @Test
    fun products_list_renders_cards_and_wires_callbacks() {
        val product = sampleProduct(id = 202L, name = "Kopi Gayo Arabika", price = 65000L, stock = 20)
        val data = PagingData.from(listOf(product), sourceLoadStates = states())
        var editedProductId: Long? = null
        var updatedProductId: Long? = null
        var updatedStatus: UpdateProductRequest.Status? = null
        var deleteTarget: SellerProduct? = null

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(),
                    products = flowOf(data).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = { editedProductId = it },
                    onSelectStatus = {},
                    onUpdateStatus = { id, status ->
                        updatedProductId = id
                        updatedStatus = status
                    },
                    onOpenDeleteDialog = { deleteTarget = it },
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Kopi Gayo Arabika").assertIsDisplayed()
        rule.onNodeWithText("Rp 65.000").assertIsDisplayed()
        rule.onNodeWithText("Stok: 20").assertIsDisplayed()

        // Edit button
        rule.onNodeWithText("Ubah").performClick()
        assertEquals(202L, editedProductId)

        // Dropdown menu options
        rule.onNodeWithTag("SellerProductCard_MoreOptions").performClick()
        rule.onNodeWithText("Tandai Habis").performClick()
        assertEquals(202L, updatedProductId)
        assertEquals(UpdateProductRequest.Status.OUT_OF_STOCK, updatedStatus)

        // Delete option
        rule.onNodeWithTag("SellerProductCard_MoreOptions").performClick()
        rule.onNodeWithText("Hapus Produk").performClick()
        assertEquals(product, deleteTarget)
    }

    @Test
    fun delete_dialog_displayed_when_active_and_triggers_callbacks() {
        val product = sampleProduct(id = 303L, name = "Kue Nastar Spesial")
        var deleteConfirmed = false
        var dialogClosed = false

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(productToDelete = product),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = { dialogClosed = true },
                    onDeleteProduct = { deleteConfirmed = true },
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Hapus Produk?").assertIsDisplayed()
        rule.onNodeWithText("Kue Nastar Spesial").assertIsDisplayed()

        rule.onNodeWithText("Batal").performClick()
        assertTrue(dialogClosed)

        rule.onNodeWithText("Hapus").performClick()
        assertTrue(deleteConfirmed)
    }

    @Test
    fun action_error_snackbar_displays_and_dismisses() {
        var errorDismissed = false

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(actionError = "Gagal memperbarui status produk"),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = {},
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = { errorDismissed = true },
                )
            }
        }

        rule.onNodeWithText("Gagal memperbarui status produk").assertIsDisplayed()
        assertTrue(errorDismissed)
        rule.mainClock.advanceTimeBy(5000)
    }

    @Test
    fun screen_header_renders_title_and_triggers_back() {
        var backPressed = false

        rule.setContent {
            NusantaraTheme {
                SellerProductsContent(
                    state = SellerProductsState(),
                    products = flowOf(PagingData.empty<SellerProduct>(sourceLoadStates = states())).collectAsLazyPagingItems(),
                    onBack = { backPressed = true },
                    onAddProduct = {},
                    onEditProduct = {},
                    onSelectStatus = {},
                    onUpdateStatus = { _, _ -> },
                    onOpenDeleteDialog = {},
                    onCloseDeleteDialog = {},
                    onDeleteProduct = {},
                    onDismissError = {},
                )
            }
        }

        rule.onNodeWithText("Kelola Produk").assertIsDisplayed()
        rule.onNodeWithContentDescription("Kembali").performClick()
        assertTrue(backPressed)
    }
}
