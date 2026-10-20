package id.oemah.nusantara.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.ui.theme.NusantaraTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductGridTest {
    @get:Rule val rule = createComposeRule()

    private fun product(id: Long) = ProductSummary(
        id, "Produk $id", 1, "Bu Siti", 10_000, ImageSet("s", "m", "l"), 4.5, 1, 5, false,
    )

    private fun states(refresh: LoadState) = LoadStates(refresh, LoadState.NotLoading(true), LoadState.NotLoading(true))

    private fun grid(data: PagingData<ProductSummary>) = rule.setContent {
        NusantaraTheme {
            ProductGrid(
                items = flowOf(data).collectAsLazyPagingItems(),
                emptyText = "Produk tidak ditemukan",
                onOpen = {},
                onAdd = {},
            )
        }
    }

    @Test fun shows_the_products() {
        grid(PagingData.from(listOf(product(1), product(2)), sourceLoadStates = states(LoadState.NotLoading(true))))

        rule.onNodeWithText("Produk 1").assertExists()
        rule.onNodeWithText("Produk 2").assertExists()
    }

    @Test fun shows_the_empty_text_when_there_is_nothing() {
        grid(PagingData.from(emptyList(), sourceLoadStates = states(LoadState.NotLoading(true))))

        rule.onNodeWithText("Produk tidak ditemukan").assertExists()
    }

    @Test fun shows_a_skeleton_while_the_first_page_loads() {
        grid(PagingData.from(emptyList(), sourceLoadStates = states(LoadState.Loading)))

        rule.onNodeWithTag("skeleton").assertExists()
    }

    @Test fun shows_the_error_message_and_a_retry_button() {
        grid(PagingData.from(emptyList(), sourceLoadStates = states(LoadState.Error(AppError.Offline))))

        rule.onNodeWithText("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.").assertExists()
        rule.onNodeWithText("Coba lagi").performClick()
    }
}
