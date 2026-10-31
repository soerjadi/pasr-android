package id.oemah.nusantara.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.ads.AdSlot
import id.oemah.nusantara.feature.ads.rememberLinkOpener
import id.oemah.nusantara.feature.cart.CartButton
import id.oemah.nusantara.feature.categories.emoji
import id.oemah.nusantara.feature.store.StoreWidget
import id.oemah.nusantara.ui.components.ProductCard
import id.oemah.nusantara.ui.components.userMessage

@Composable
fun HomeRoute(
    onSearch: () -> Unit,
    onOpenFeatured: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenCart: () -> Unit,
    onOpenSellerMode: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    HomeScreen(
        state = state,
        store = viewModel.store.collectAsStateWithLifecycle().value,
        onOpenSellerMode = onOpenSellerMode,
        onSearch = onSearch,
        onOpenFeatured = onOpenFeatured,
        onOpenCategory = onOpenCategory,
        onOpenAllCategories = onOpenAllCategories,
        onOpenProduct = onOpenProduct,
        onAddToCart = viewModel::addToCart,
        onOpenLink = rememberLinkOpener(),
        onRetry = viewModel::reload,
        cartAction = { CartButton(onOpenCart) },
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    store: StoreSummary?,
    onOpenSellerMode: () -> Unit,
    onSearch: () -> Unit,
    onOpenFeatured: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onAddToCart: (ProductSummary) -> Unit,
    onOpenLink: (String) -> Unit,
    onRetry: () -> Unit,
    cartAction: @Composable () -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth { Header(onSearch, cartAction) }
        store?.let { fullWidth { StoreWidget(it, onOpenSellerMode) } }
        fullWidth { Hero(onOpenFeatured) }
        fullWidth { CategoryShortcuts(state, onOpenCategory, onOpenAllCategories, onRetry) }

        if (state.partnerAds.isNotEmpty()) {
            fullWidth { SectionTitle(stringResource(R.string.home_partners)) }
            fullWidth { AdSlot(state.partnerAds, onOpen = onOpenLink) }
        }

        fullWidth { SectionTitle(stringResource(R.string.home_featured)) }
        when (val featured = state.featured) {
            UiState.Loading -> fullWidth { PlaceholderBlock() }
            is UiState.Error -> fullWidth { InlineError(featured.error.userMessage(), onRetry) }
            is UiState.Content ->
                if (featured.value.isEmpty()) {
                    fullWidth { Text(stringResource(R.string.home_empty_featured), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                } else {
                    items(featured.value, key = { it.id }) { product ->
                        ProductCard(product, onOpen = { onOpenProduct(product.id) }, onAdd = { onAddToCart(product) })
                    }
                }
        }

        if (state.sponsoredAds.isNotEmpty()) {
            fullWidth { AdSlot(state.sponsoredAds, onOpen = onOpenLink) }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Header(onSearch: () -> Unit, cartAction: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.home_location), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp).weight(1f))
            cartAction()
        }
        OutlinedCard(onClick = onSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Row(Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null)
                Text(stringResource(R.string.home_search_hint), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Hero(onOpenFeatured: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary).padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_hero_kicker), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.home_hero_title), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            Button(onClick = onOpenFeatured, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.home_hero_action)) }
        }
    }
}

@Composable
private fun CategoryShortcuts(
    state: HomeUiState,
    onOpenCategory: (Category) -> Unit,
    onOpenAll: () -> Unit,
    onRetry: () -> Unit,
) {
    when (val categories = state.categories) {
        UiState.Loading -> PlaceholderBlock(height = 72.dp)
        is UiState.Error -> InlineError(categories.error.userMessage(), onRetry)
        is UiState.Content -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.categoryShortcuts.forEach { category -> Shortcut(category.emoji(), category.name) { onOpenCategory(category) } }
            Shortcut("➕", stringResource(R.string.home_more_categories), onOpenAll)
        }
    }
}

@Composable
private fun Shortcut(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).clickable(onClick = onClick).heightIn(min = 48.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium)

@Composable
private fun PlaceholderBlock(height: androidx.compose.ui.unit.Dp = 160.dp) {
    Box(Modifier.fillMaxWidth().heightIn(min = height).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
