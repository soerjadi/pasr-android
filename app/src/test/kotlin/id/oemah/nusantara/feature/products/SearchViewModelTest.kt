package id.oemah.nusantara.feature.products

import androidx.paging.PagingData
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()

    private fun searchTest(block: suspend TestScope.(SearchViewModel) -> Unit) = runTest {
        every { catalog.products(any()) } returns flowOf(PagingData.empty())
        val vm = SearchViewModel(catalog, FakeCartRepository())
        vm.results.onEach { }.launchIn(backgroundScope) // keep the results flow collected like the screen does
        runCurrent()
        block(vm)
    }

    @Test fun typing_is_debounced_300ms_and_searches_once_for_the_final_text() = searchTest { vm ->
        vm.onQueryChange("t"); advanceTimeBy(100)
        vm.onQueryChange("te"); advanceTimeBy(100)
        vm.onQueryChange("tem"); advanceTimeBy(299)
        verify(exactly = 0) { catalog.products(any()) }

        advanceTimeBy(2); runCurrent()

        verify(exactly = 1) { catalog.products(any()) }
        verify(exactly = 1) { catalog.products(ProductQuery(query = "tem")) }
    }

    @Test fun blank_or_whitespace_input_makes_no_request_and_is_idle() = searchTest { vm ->
        vm.onQueryChange("   "); advanceTimeBy(1_000); runCurrent()

        verify(exactly = 0) { catalog.products(any()) }
        assertTrue(vm.isIdle.value)
    }

    @Test fun the_query_is_trimmed_before_searching() = searchTest { vm ->
        vm.onQueryChange("  tempe  "); advanceTimeBy(400); runCurrent()

        verify(exactly = 1) { catalog.products(ProductQuery(query = "tempe")) }
        assertFalse(vm.isIdle.value)
    }

    @Test fun the_typed_text_is_exposed_unchanged_for_the_text_field() = searchTest { vm ->
        vm.onQueryChange("  tem"); runCurrent()

        assertEquals("  tem", vm.queryText.value)
    }
}
