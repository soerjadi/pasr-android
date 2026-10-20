package id.oemah.nusantara.feature.products.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.ProductDetail
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.pick
import id.oemah.nusantara.feature.cart.CartButton
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.QuantityStepper
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun ProductDetailRoute(
    onBack: () -> Unit,
    onOpenSeller: (Long) -> Unit,
    onLoginRequired: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val quantity = viewModel.quantity.collectAsStateWithLifecycle().value
    val favorite = viewModel.favorite.collectAsStateWithLifecycle().value
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                UiEvent.LoginRequired -> onLoginRequired()
                is UiEvent.Message -> snackbar.showSnackbar(it.text)
            }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        StateHost(state = state, onRetry = viewModel::reload, modifier = Modifier.padding(padding)) { detail ->
            ProductDetailScreen(
                detail = detail,
                favorite = favorite,
                quantity = quantity,
                onQuantityChange = viewModel::setQuantity,
                onAddToCart = viewModel::addToCart,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenSeller = onOpenSeller,
                onBack = onBack,
                cartAction = { CartButton(onOpenCart) },
            )
        }
    }
}

@Composable
fun ProductDetailScreen(
    detail: ProductDetail,
    favorite: Boolean,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSeller: (Long) -> Unit,
    onBack: () -> Unit,
    cartAction: @Composable () -> Unit = {},
) {
    val product = detail.product
    val soldOut = product.stock <= 0
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Gallery(detail, favorite, onToggleFavorite, onBack, cartAction)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.detail_rating, product.rating.toString(), product.ratingCount), style = MaterialTheme.typography.bodyMedium)
                Text(Rupiah(product.price).format(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.detail_stock, product.stock), style = MaterialTheme.typography.bodySmall)

                Card(onClick = { onOpenSeller(detail.seller.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp).sizeIn(minHeight = 48.dp)) {
                        Text(detail.seller.name, style = MaterialTheme.typography.titleMedium)
                        Text(detail.seller.location, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.detail_pickup_title), style = MaterialTheme.typography.titleSmall)
                        Text(detail.seller.pickupAddress)
                        Text(detail.seller.openingHours, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text(detail.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuantityStepper(value = quantity, min = 1, max = product.stock.coerceIn(1, 99), onChange = onQuantityChange)
                PrimaryButton(
                    text = stringResource(if (soldOut) R.string.detail_sold_out else R.string.detail_add_to_cart),
                    onClick = onAddToCart,
                    enabled = !soldOut,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Gallery(
    detail: ProductDetail,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    cartAction: @Composable () -> Unit,
) {
    val images = detail.images.ifEmpty { listOf(detail.product.image) }
    val pager = rememberPagerState { images.size }
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = images[page].pick(1080),
                contentDescription = detail.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            cartAction()
            IconButton(onClick = onToggleFavorite, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(
                    imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(if (favorite) R.string.detail_favorite_remove else R.string.detail_favorite_add),
                    tint = if (favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (detail.product.halalVerified) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            ) {
                Text(stringResource(R.string.detail_halal_badge), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            Text("${pager.currentPage + 1}/${images.size}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
