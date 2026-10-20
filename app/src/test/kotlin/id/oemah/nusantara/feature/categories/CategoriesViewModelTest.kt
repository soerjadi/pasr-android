package id.oemah.nusantara.feature.categories

import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoriesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()

    @Test fun loads_the_categories() = runTest {
        coEvery { catalog.categories() } returns Result.success(listOf(Category(1, "Makanan & Minuman", "food")))

        val vm = CategoriesViewModel(catalog); advanceUntilIdle()

        assertEquals(UiState.Content(listOf(Category(1, "Makanan & Minuman", "food"))), vm.state.value)
    }

    @Test fun a_failure_is_an_error_state_and_retry_reloads() = runTest {
        coEvery { catalog.categories() } returns Result.failure(AppError.Offline)
        val vm = CategoriesViewModel(catalog); advanceUntilIdle()
        assertEquals(UiState.Error(AppError.Offline), vm.state.value)

        coEvery { catalog.categories() } returns Result.success(emptyList())
        vm.reload(); advanceUntilIdle()

        assertEquals(UiState.Content(emptyList<Category>()), vm.state.value)
        coVerify(exactly = 2) { catalog.categories() }
    }

    @Test fun known_icon_keys_have_an_emoji_and_unknown_ones_fall_back() {
        assertEquals("🍜", Category(1, "x", "food").emoji())
        assertEquals("🛍️", Category(2, "x", "something-new").emoji())
    }
}
