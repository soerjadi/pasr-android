package id.oemah.nusantara.feature.store

import app.cash.turbine.test
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class AddProductViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val myStore = mockk<MyStoreRepository>()
    private val vm = AddProductViewModel(myStore)

    private fun fill() {
        vm.setName("Nasi pecel"); vm.setPrice("15.000"); vm.setStock("10")
    }

    @Test fun price_keeps_only_digits() {
        vm.setPrice("Rp 15.000")
        assertEquals("15000", vm.form.value.price)
    }

    @Test fun the_stepper_stays_between_zero_and_the_maximum() {
        vm.stepStock(-1)
        assertEquals("0", vm.form.value.stock)
        vm.setStock("9999"); vm.stepStock(1)
        assertEquals("9999", vm.form.value.stock)
        vm.stepStock(-1)
        assertEquals("9998", vm.form.value.stock)
    }

    @Test fun saving_sends_the_request_and_signals_done() = runTest {
        coEvery { myStore.createProduct(any()) } returns Result.success(SellerProduct(31, "Nasi pecel", 15_000, 10))
        fill()

        vm.saved.test {
            vm.submit(); runCurrent()
            assertNotNull(awaitItem())
        }
        coVerify { myStore.createProduct(CreateProductRequest("Nasi pecel", 15_000, 10)) }
    }

    @Test fun an_incomplete_form_sends_nothing() = runTest {
        vm.setName("Nasi pecel")

        vm.submit(); runCurrent()

        coVerify(exactly = 0) { myStore.createProduct(any()) }
    }

    @Test fun a_failed_save_shows_the_error_and_allows_retry() = runTest {
        coEvery { myStore.createProduct(any()) } returns Result.failure(AppError.Offline)
        fill()

        vm.submit(); runCurrent()

        assertNotNull(vm.error.value)
        assertEquals(false, vm.submitting.value)
    }
}
