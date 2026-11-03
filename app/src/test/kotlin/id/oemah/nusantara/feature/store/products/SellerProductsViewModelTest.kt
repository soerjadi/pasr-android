package id.oemah.nusantara.feature.store.products

import androidx.paging.PagingData
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.SellerProductPage
import id.oemah.nusantara.api.model.SellerProductPageCounts
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import id.oemah.nusantara.ui.components.userMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SellerProductsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val sellerRepo = mockk<SellerRepository>()
    private val requestedStatuses = mutableListOf<MeApi.StatusListMyProducts?>()

    private fun sampleSellerProduct(
        id: Long = 1,
        name: String = "Product $id",
        status: SellerProduct.Status = SellerProduct.Status.AVAILABLE,
    ) = SellerProduct(
        id = id,
        name = name,
        price = 15_000,
        stock = 10,
        description = "Deskripsi",
        status = status,
        images = listOf(ImageSet("s", "m", "l")),
    )

    private fun sampleCounts() = SellerProductPageCounts(
        all = 10,
        available = 6,
        outOfStock = 3,
        hidden = 1,
    )

    private fun TestScope.createViewModel(): SellerProductsViewModel {
        every { sellerRepo.myProducts(any()) } answers {
            requestedStatuses += firstArg<MeApi.StatusListMyProducts?>()
            flowOf(PagingData.empty())
        }
        coEvery { sellerRepo.listMyProducts(any(), any(), any()) } returns Result.success(
            SellerProductPage(items = emptyList(), counts = sampleCounts(), nextCursor = null),
        )

        val vm = SellerProductsViewModel(sellerRepo)
        vm.products.onEach { }.launchIn(backgroundScope)
        runCurrent()
        return vm
    }

    @Test
    fun initial_state_defaults_to_all_null_and_loads_counts() = runTest {
        val vm = createViewModel()

        assertNull(vm.state.value.selectedStatus)
        assertEquals(sampleCounts(), vm.state.value.counts)
        assertEquals(listOf<MeApi.StatusListMyProducts?>(null), requestedStatuses)
    }

    @Test
    fun selecting_a_different_status_updates_state_and_requests_filtered_products() = runTest {
        val vm = createViewModel()

        vm.selectStatus(MeApi.StatusListMyProducts.AVAILABLE)
        runCurrent()

        assertEquals(MeApi.StatusListMyProducts.AVAILABLE, vm.state.value.selectedStatus)
        assertEquals(
            listOf(null, MeApi.StatusListMyProducts.AVAILABLE),
            requestedStatuses,
        )
    }

    @Test
    fun selecting_the_same_status_does_not_reload() = runTest {
        val vm = createViewModel()

        vm.selectStatus(null)
        runCurrent()

        assertEquals(1, requestedStatuses.size)
    }

    @Test
    fun refresh_reloads_paging_and_refreshes_counts() = runTest {
        val vm = createViewModel()

        vm.refresh()
        runCurrent()

        assertEquals(listOf<MeApi.StatusListMyProducts?>(null, null), requestedStatuses)
        coVerify(exactly = 2) { sellerRepo.listMyProducts(status = null, limit = 1) }
    }

    @Test
    fun updateProductStatus_calls_repository_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val updatedProduct = sampleSellerProduct(42, status = SellerProduct.Status.OUT_OF_STOCK)
        coEvery {
            sellerRepo.updateMyProduct(42L, UpdateProductRequest(status = UpdateProductRequest.Status.OUT_OF_STOCK))
        } returns Result.success(updatedProduct)

        vm.updateProductStatus(42L, UpdateProductRequest.Status.OUT_OF_STOCK)
        assertEquals(42L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify {
            sellerRepo.updateMyProduct(42L, UpdateProductRequest(status = UpdateProductRequest.Status.OUT_OF_STOCK))
        }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf<MeApi.StatusListMyProducts?>(null, null), requestedStatuses)
    }

    @Test
    fun updateProductStatus_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        coEvery {
            sellerRepo.updateMyProduct(42L, UpdateProductRequest(status = UpdateProductRequest.Status.HIDDEN))
        } returns Result.failure(AppError.Offline)

        vm.updateProductStatus(42L, UpdateProductRequest.Status.HIDDEN)
        runCurrent()

        coVerify {
            sellerRepo.updateMyProduct(42L, UpdateProductRequest(status = UpdateProductRequest.Status.HIDDEN))
        }
        assertNull(vm.state.value.actionInProgressId)
        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun openDeleteDialog_and_closeDeleteDialog_manage_productToDelete() = runTest {
        val vm = createViewModel()
        val product = sampleSellerProduct(99)

        assertNull(vm.state.value.productToDelete)

        vm.openDeleteDialog(product)
        assertEquals(product, vm.state.value.productToDelete)

        vm.closeDeleteDialog()
        assertNull(vm.state.value.productToDelete)
    }

    @Test
    fun deleteProduct_without_productToDelete_does_nothing() = runTest {
        val vm = createViewModel()

        vm.deleteProduct()
        runCurrent()

        coVerify(exactly = 0) { sellerRepo.deleteMyProduct(any()) }
    }

    @Test
    fun deleteProduct_calls_repository_closes_dialog_and_refreshes_on_success() = runTest {
        val vm = createViewModel()
        val product = sampleSellerProduct(101)
        vm.openDeleteDialog(product)

        coEvery { sellerRepo.deleteMyProduct(101L) } returns Result.success(Unit)

        vm.deleteProduct()
        assertEquals(101L, vm.state.value.actionInProgressId)

        runCurrent()

        coVerify { sellerRepo.deleteMyProduct(101L) }
        assertNull(vm.state.value.actionInProgressId)
        assertNull(vm.state.value.productToDelete)
        assertNull(vm.state.value.actionError)
        assertEquals(listOf<MeApi.StatusListMyProducts?>(null, null), requestedStatuses)
    }

    @Test
    fun deleteProduct_sets_actionError_on_failure() = runTest {
        val vm = createViewModel()
        val product = sampleSellerProduct(101)
        vm.openDeleteDialog(product)

        coEvery { sellerRepo.deleteMyProduct(101L) } returns Result.failure(AppError.Offline)

        vm.deleteProduct()
        runCurrent()

        coVerify { sellerRepo.deleteMyProduct(101L) }
        assertNull(vm.state.value.actionInProgressId)
        assertEquals(product, vm.state.value.productToDelete)
        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)
    }

    @Test
    fun dismissError_clears_actionError() = runTest {
        val vm = createViewModel()
        coEvery {
            sellerRepo.updateMyProduct(any(), any())
        } returns Result.failure(AppError.Offline)

        vm.updateProductStatus(1L, UpdateProductRequest.Status.OUT_OF_STOCK)
        runCurrent()

        assertEquals(AppError.Offline.userMessage(), vm.state.value.actionError)

        vm.dismissError()
        assertNull(vm.state.value.actionError)
    }
}
