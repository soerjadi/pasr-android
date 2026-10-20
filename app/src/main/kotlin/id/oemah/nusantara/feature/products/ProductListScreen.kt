package id.oemah.nusantara.feature.products

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.ProductGrid
import id.oemah.nusantara.ui.components.ScreenHeader

@Composable
fun ProductListRoute(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val items = viewModel.products.collectAsLazyPagingItems()
    Column {
        ScreenHeader(title = viewModel.title, onBack = onBack)
        ProductGrid(
            items = items,
            emptyText = stringResource(R.string.products_empty),
            onOpen = onOpenProduct,
            onAdd = viewModel::addToCart,
        )
    }
}
