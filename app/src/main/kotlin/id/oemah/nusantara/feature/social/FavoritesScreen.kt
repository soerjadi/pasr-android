package id.oemah.nusantara.feature.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.R
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ProductGrid

@Composable
fun FavoritesRoute(
    onOpenProduct: (Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val loggedIn = viewModel.loggedIn.collectAsStateWithLifecycle().value
    val removed = viewModel.removedIds.collectAsStateWithLifecycle().value
    val items = viewModel.favorites.collectAsLazyPagingItems()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it is UiEvent.Message) snackbar.showSnackbar(it.text) }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(stringResource(R.string.favorites_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            if (!loggedIn) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.favorites_login_prompt))
                    PrimaryButton(text = stringResource(R.string.orders_login_action), onClick = onLogin)
                }
            } else {
                ProductGrid(
                    items = items,
                    emptyText = stringResource(R.string.favorites_empty),
                    onOpen = onOpenProduct,
                    onAdd = viewModel::addToCart,
                    hiddenIds = removed,
                    overlay = { product ->
                        IconButton(
                            onClick = { viewModel.remove(product.id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.detail_favorite_remove), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }
        }
    }
}
