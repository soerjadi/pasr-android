package id.oemah.nusantara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.asAppError

/** A paged two-column grid of products with skeleton, empty, error and load-more states. */
@Composable
fun ProductGrid(
    items: LazyPagingItems<ProductSummary>,
    emptyText: String,
    onOpen: (Long) -> Unit,
    onAdd: (ProductSummary) -> Unit,
    modifier: Modifier = Modifier,
    hiddenIds: Set<Long> = emptySet(),
    overlay: @Composable BoxScope.(ProductSummary) -> Unit = {},
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> Skeleton(modifier)
        items.itemCount == 0 && refresh is LoadState.Error ->
            ErrorPanel(refresh.error.asAppError().userMessage(), onRetry = items::retry)
        items.itemCount == 0 -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyText) }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.takeIf { it.id !in hiddenIds }?.let { product ->
                    Box {
                        ProductCard(product, onOpen = { onOpen(product.id) }, onAdd = { onAdd(product) })
                        overlay(product)
                    }
                }
            }
            when (val append = items.loadState.append) {
                LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = items::retry) { Text(append.error.asAppError().userMessage() + " " + stringResource(R.string.retry)) }
                    }
                }
                else -> Unit
            }
        }
    }
}
