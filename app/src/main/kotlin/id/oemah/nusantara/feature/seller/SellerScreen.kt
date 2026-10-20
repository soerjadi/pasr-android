package id.oemah.nusantara.feature.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.api.model.SellerDetail
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.pick
import id.oemah.nusantara.feature.ads.rememberLinkOpener
import id.oemah.nusantara.feature.cart.CartButton
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ProductGrid
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun SellerRoute(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onLoginRequired: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: SellerViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val following = viewModel.following.collectAsStateWithLifecycle().value
    val followerCount = viewModel.followerCount.collectAsStateWithLifecycle().value
    val whatsappUrl = viewModel.whatsappUrl.collectAsStateWithLifecycle().value
    val products = viewModel.products.collectAsLazyPagingItems()
    val reviews = viewModel.reviews.collectAsLazyPagingItems()
    val openLink = rememberLinkOpener()
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
            SellerScreen(
                detail = detail,
                following = following,
                followerCount = followerCount,
                chatAvailable = whatsappUrl != null,
                products = products,
                reviews = reviews,
                onToggleFollow = viewModel::toggleFollow,
                onChat = { whatsappUrl?.let(openLink) },
                onOpenProduct = onOpenProduct,
                onAddToCart = viewModel::addToCart,
                onBack = onBack,
                headerActions = { CartButton(onOpenCart) },
            )
        }
    }
}

@Composable
fun SellerScreen(
    detail: SellerDetail,
    following: Boolean,
    followerCount: Int,
    chatAvailable: Boolean,
    products: LazyPagingItems<ProductSummary>,
    reviews: LazyPagingItems<Review>,
    onToggleFollow: () -> Unit,
    onChat: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onAddToCart: (ProductSummary) -> Unit,
    onBack: () -> Unit,
    headerActions: @Composable RowScope.() -> Unit = {},
) {
    val seller = detail.seller
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth()) {
        ScreenHeader(title = seller.name, onBack = onBack, actions = headerActions)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                seller.avatar?.let {
                    AsyncImage(it.pick(320), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(CircleShape))
                }
                Column(Modifier.weight(1f)) {
                    Text(seller.location, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.detail_rating, seller.rating.toString(), seller.ratingCount), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.seller_followers, followerCount), style = MaterialTheme.typography.bodySmall)
                }
                if (following) {
                    OutlinedButton(onClick = onToggleFollow, modifier = Modifier.padding(start = 4.dp)) { Text(stringResource(R.string.seller_following)) }
                } else {
                    PrimaryButton(text = stringResource(R.string.seller_follow), onClick = onToggleFollow)
                }
            }
            Text(detail.description, style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.detail_pickup_title), style = MaterialTheme.typography.titleSmall)
                    Text(seller.pickupAddress)
                    Text(seller.openingHours, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (chatAvailable) {
                OutlinedButton(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.seller_chat)) }
            }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.seller_tab_products)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.seller_tab_reviews)) })
        }
        if (tab == 0) {
            ProductGrid(products, emptyText = stringResource(R.string.seller_products_empty), onOpen = onOpenProduct, onAdd = onAddToCart)
        } else {
            ReviewList(reviews)
        }
    }
}
