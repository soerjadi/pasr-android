package id.oemah.nusantara.feature.store.products

import androidx.lifecycle.SavedStateHandle
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ImageUpload
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductEditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val sellerRepo = mockk<SellerRepository>(relaxed = true)

    private fun sampleProduct(
        id: Long = 42L,
        name: String = "Gudeg Jogja",
        price: Long = 25_000L,
        stock: Int = 15,
        description: String = "Makanan khas Yogyakarta",
        status: SellerProduct.Status = SellerProduct.Status.AVAILABLE,
        images: List<ImageSet> = listOf(ImageSet("s.jpg", "m.jpg", "l.jpg")),
    ) = SellerProduct(
        id = id,
        name = name,
        price = price,
        stock = stock,
        description = description,
        status = status,
        images = images,
    )

    private fun TestScope.createViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
    ): ProductEditorViewModel {
        val vm = ProductEditorViewModel(handle, sellerRepo)
        runCurrent()
        return vm
    }

    @Test
    fun add_mode_default_state() = runTest {
        val vm = createViewModel(SavedStateHandle())

        val state = vm.state.value
        assertFalse(state.isEditMode)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals("", state.price)
        assertEquals("", state.stock)
        assertEquals("", state.description)
        assertNull(state.photoId)
        assertNull(state.imageUrl)
        assertEquals(SellerProduct.Status.AVAILABLE, state.status)
        assertFalse(state.isUploadingImage)
        assertFalse(state.isSubmitting)
        assertFalse(state.showDeleteDialog)
        assertNull(state.error)
        assertFalse(state.isFormValid)
    }

    @Test
    fun edit_mode_initial_loading_success() = runTest {
        val product = sampleProduct(id = 42L)
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.success(product)

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))

        coVerify(exactly = 1) { sellerRepo.getMyProduct(42L) }
        val state = vm.state.value
        assertTrue(state.isEditMode)
        assertFalse(state.isLoading)
        assertEquals("Gudeg Jogja", state.name)
        assertEquals("25000", state.price)
        assertEquals("15", state.stock)
        assertEquals("Makanan khas Yogyakarta", state.description)
        assertEquals("m.jpg", state.imageUrl)
        assertNull(state.photoId)
        assertEquals(SellerProduct.Status.AVAILABLE, state.status)
        assertTrue(state.isFormValid)
    }

    @Test
    fun edit_mode_initial_loading_failure() = runTest {
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.failure(AppError.Offline)

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))

        coVerify(exactly = 1) { sellerRepo.getMyProduct(42L) }
        val state = vm.state.value
        assertTrue(state.isEditMode)
        assertFalse(state.isLoading)
        assertEquals("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.", state.error)
    }

    @Test
    fun createProduct_success() = runTest {
        var savedEmitted = 0
        coEvery { sellerRepo.createProduct(any()) } returns Result.success(sampleProduct())

        val vm = createViewModel(SavedStateHandle())
        vm.saved.onEach { savedEmitted++ }.launchIn(backgroundScope)

        vm.setName("Bakpia Pathok")
        vm.setPrice("35000")
        vm.setStock("50")
        vm.setDescription("Bakpia isi kacang hijau")

        assertTrue(vm.state.value.isFormValid)

        vm.submit()
        runCurrent()

        coVerify(exactly = 1) {
            sellerRepo.createProduct(
                CreateProductRequest(
                    name = "Bakpia Pathok",
                    price = 35_000L,
                    stock = 50,
                    description = "Bakpia isi kacang hijau",
                    photoIds = null,
                )
            )
        }
        assertEquals(1, savedEmitted)
        assertFalse(vm.state.value.isSubmitting)
        assertNull(vm.state.value.error)
    }

    @Test
    fun createProduct_with_uploaded_photo() = runTest {
        coEvery { sellerRepo.uploadImage(any<ByteArray>(), any(), any()) } returns Result.success(
            ImageUpload(id = 88L, image = ImageSet("s.png", "m.png", "l.png"))
        )
        coEvery { sellerRepo.createProduct(any()) } returns Result.success(sampleProduct())

        val vm = createViewModel(SavedStateHandle())

        vm.uploadPhoto(byteArrayOf(1, 2, 3), "test.png", "image/png")
        runCurrent()

        assertEquals(88L, vm.state.value.photoId)
        assertEquals("m.png", vm.state.value.imageUrl)

        vm.setName("Keripik Tempe")
        vm.setPrice("12000")
        vm.setStock("20")

        vm.submit()
        runCurrent()

        coVerify(exactly = 1) {
            sellerRepo.createProduct(
                CreateProductRequest(
                    name = "Keripik Tempe",
                    price = 12_000L,
                    stock = 20,
                    description = null,
                    photoIds = listOf(88L),
                )
            )
        }
    }

    @Test
    fun createProduct_failure_sets_error() = runTest {
        var savedEmitted = 0
        coEvery { sellerRepo.createProduct(any()) } returns Result.failure(AppError.Conflict("ERR", "Nama produk sudah ada"))

        val vm = createViewModel(SavedStateHandle())
        vm.saved.onEach { savedEmitted++ }.launchIn(backgroundScope)

        vm.setName("Bakpia Pathok")
        vm.setPrice("35000")
        vm.setStock("50")

        vm.submit()
        runCurrent()

        assertEquals(0, savedEmitted)
        assertFalse(vm.state.value.isSubmitting)
        assertEquals("Nama produk sudah ada", vm.state.value.error)
    }

    @Test
    fun updateProduct_success() = runTest {
        var savedEmitted = 0
        val product = sampleProduct(id = 42L)
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.success(product)
        coEvery { sellerRepo.updateMyProduct(eq(42L), any()) } returns Result.success(product)

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))
        vm.saved.onEach { savedEmitted++ }.launchIn(backgroundScope)

        vm.setName("Gudeg Spesial")
        vm.setPrice("30000")
        vm.setStatus(SellerProduct.Status.OUT_OF_STOCK)

        vm.submit()
        runCurrent()

        coVerify(exactly = 1) {
            sellerRepo.updateMyProduct(
                42L,
                UpdateProductRequest(
                    name = "Gudeg Spesial",
                    price = 30_000L,
                    stock = 15,
                    description = "Makanan khas Yogyakarta",
                    photoIds = null,
                    status = UpdateProductRequest.Status.OUT_OF_STOCK,
                )
            )
        }
        assertEquals(1, savedEmitted)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun updateProduct_failure_sets_error() = runTest {
        val product = sampleProduct(id = 42L)
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.success(product)
        coEvery { sellerRepo.updateMyProduct(eq(42L), any()) } returns Result.failure(AppError.Conflict("ERR", "Gagal update"))

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))

        vm.submit()
        runCurrent()

        assertEquals("Gagal update", vm.state.value.error)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun photo_upload_and_remove() = runTest {
        coEvery { sellerRepo.uploadImage(any<ByteArray>(), any(), any()) } returns Result.success(
            ImageUpload(id = 101L, image = ImageSet("s.jpg", "medium_pic.jpg", "l.jpg"))
        )

        val vm = createViewModel(SavedStateHandle())

        vm.uploadPhoto(byteArrayOf(1, 2, 3))
        assertTrue(vm.state.value.isUploadingImage)

        runCurrent()

        assertFalse(vm.state.value.isUploadingImage)
        assertEquals(101L, vm.state.value.photoId)
        assertEquals("medium_pic.jpg", vm.state.value.imageUrl)

        vm.removePhoto()
        assertNull(vm.state.value.photoId)
        assertNull(vm.state.value.imageUrl)
    }

    @Test
    fun photo_upload_failure_sets_error() = runTest {
        coEvery { sellerRepo.uploadImage(any<ByteArray>(), any(), any()) } returns Result.failure(
            AppError.Conflict("ERR", "Unggah foto gagal")
        )

        val vm = createViewModel(SavedStateHandle())

        vm.uploadPhoto(byteArrayOf(1, 2, 3))
        runCurrent()

        assertFalse(vm.state.value.isUploadingImage)
        assertNull(vm.state.value.photoId)
        assertEquals("Unggah foto gagal", vm.state.value.error)
    }

    @Test
    fun stock_stepping_and_numeric_constraints() = runTest {
        val vm = createViewModel(SavedStateHandle())

        // Non-digits filtered
        vm.setStock("12a3")
        assertEquals("123", vm.state.value.stock)

        // Step up
        vm.stepStock(5)
        assertEquals("128", vm.state.value.stock)

        // Step down
        vm.stepStock(-28)
        assertEquals("100", vm.state.value.stock)

        // Clamp to 0
        vm.stepStock(-200)
        assertEquals("0", vm.state.value.stock)

        // Max 4 digits
        vm.setStock("99999")
        assertEquals("9999", vm.state.value.stock)

        // Clamp to 9999
        vm.stepStock(10)
        assertEquals("9999", vm.state.value.stock)

        // Empty stock steps from 0
        vm.setStock("")
        vm.stepStock(3)
        assertEquals("3", vm.state.value.stock)
    }

    @Test
    fun price_and_name_constraints() = runTest {
        val vm = createViewModel(SavedStateHandle())

        // Price filters non-digits
        vm.setPrice("Rp 50.000")
        assertEquals("50000", vm.state.value.price)

        // Max 9 digits
        vm.setPrice("1234567890123")
        assertEquals("123456789", vm.state.value.price)

        // Name max 80 characters
        vm.setName("A".repeat(100))
        assertEquals(80, vm.state.value.name.length)
    }

    @Test
    fun validation_edge_cases() = runTest {
        val vm = createViewModel(SavedStateHandle())

        // Blank name
        vm.setName("   ")
        vm.setPrice("10000")
        vm.setStock("1")
        assertFalse(vm.state.value.isFormValid)

        // Price 0
        vm.setName("Produk")
        vm.setPrice("0")
        assertFalse(vm.state.value.isFormValid)

        // Price > 100,000,000
        vm.setPrice("100000001")
        assertFalse(vm.state.value.isFormValid)

        // Stock empty
        vm.setPrice("10000")
        vm.setStock("")
        assertFalse(vm.state.value.isFormValid)

        // Stock valid 0
        vm.setStock("0")
        assertTrue(vm.state.value.isFormValid)

        // Submit does nothing if invalid
        vm.setName("")
        vm.submit()
        runCurrent()
        coVerify(exactly = 0) { sellerRepo.createProduct(any()) }
    }

    @Test
    fun delete_flow_in_edit_mode() = runTest {
        var deletedEmitted = 0
        val product = sampleProduct(id = 42L)
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.success(product)
        coEvery { sellerRepo.deleteMyProduct(42L) } returns Result.success(Unit)

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))
        vm.deleted.onEach { deletedEmitted++ }.launchIn(backgroundScope)

        assertFalse(vm.state.value.showDeleteDialog)

        vm.openDeleteDialog()
        assertTrue(vm.state.value.showDeleteDialog)

        vm.closeDeleteDialog()
        assertFalse(vm.state.value.showDeleteDialog)

        vm.openDeleteDialog()
        vm.delete()
        runCurrent()

        coVerify(exactly = 1) { sellerRepo.deleteMyProduct(42L) }
        assertFalse(vm.state.value.showDeleteDialog)
        assertFalse(vm.state.value.isSubmitting)
        assertEquals(1, deletedEmitted)
    }

    @Test
    fun delete_failure_sets_error() = runTest {
        val product = sampleProduct(id = 42L)
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.success(product)
        coEvery { sellerRepo.deleteMyProduct(42L) } returns Result.failure(AppError.Conflict("ERR", "Gagal hapus"))

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))

        vm.delete()
        runCurrent()

        assertFalse(vm.state.value.isSubmitting)
        assertEquals("Gagal hapus", vm.state.value.error)
    }

    @Test
    fun delete_ignored_in_add_mode() = runTest {
        val vm = createViewModel(SavedStateHandle())

        vm.delete()
        runCurrent()

        coVerify(exactly = 0) { sellerRepo.deleteMyProduct(any()) }
    }

    @Test
    fun dismissError_clears_error() = runTest {
        coEvery { sellerRepo.getMyProduct(42L) } returns Result.failure(AppError.Conflict("ERR", "Error"))

        val vm = createViewModel(SavedStateHandle(mapOf("productId" to 42L)))
        assertEquals("Error", vm.state.value.error)

        vm.dismissError()
        assertNull(vm.state.value.error)
    }
}
