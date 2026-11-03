package id.oemah.nusantara.feature.store.products

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import id.oemah.nusantara.R
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.ui.components.ErrorPanel
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.Skeleton
import id.oemah.nusantara.ui.components.userMessage
import kotlinx.coroutines.launch

enum class SellerProductTabItem(
    val status: MeApi.StatusListMyProducts?,
    @StringRes val titleRes: Int,
) {
    ALL(null, R.string.seller_products_tab_all),
    AVAILABLE(MeApi.StatusListMyProducts.AVAILABLE, R.string.seller_products_tab_available),
    OUT_OF_STOCK(MeApi.StatusListMyProducts.OUT_OF_STOCK, R.string.seller_products_tab_out_of_stock),
    HIDDEN(MeApi.StatusListMyProducts.HIDDEN, R.string.seller_products_tab_hidden),
}

@Composable
fun SellerProductsRoute(
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SellerProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()

    SellerProductsContent(
        state = state,
        products = products,
        onBack = onBack,
        onAddProduct = onAddProduct,
        onEditProduct = onEditProduct,
        onSelectStatus = viewModel::selectStatus,
        onUpdateStatus = viewModel::updateProductStatus,
        onOpenDeleteDialog = viewModel::openDeleteDialog,
        onCloseDeleteDialog = viewModel::closeDeleteDialog,
        onDeleteProduct = viewModel::deleteProduct,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}

@Composable
fun SellerProductsContent(
    state: SellerProductsState,
    products: LazyPagingItems<SellerProduct>,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onSelectStatus: (MeApi.StatusListMyProducts?) -> Unit,
    onUpdateStatus: (Long, UpdateProductRequest.Status) -> Unit,
    onOpenDeleteDialog: (SellerProduct) -> Unit,
    onCloseDeleteDialog: () -> Unit,
    onDeleteProduct: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionError) {
        val error = state.actionError
        if (error != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(error)
            }
            onDismissError()
        }
    }

    val selectedTabIndex = SellerProductTabItem.entries
        .indexOfFirst { it.status == state.selectedStatus }
        .coerceAtLeast(0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.seller_products_title),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.seller_product_form_add_title)) },
                modifier = Modifier.testTag("SellerProducts_AddFab"),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                SellerProductTabItem.entries.forEach { tabItem ->
                    val isSelected = tabItem.status == state.selectedStatus
                    val count = when (tabItem.status) {
                        null -> state.counts?.all ?: 0
                        MeApi.StatusListMyProducts.AVAILABLE -> state.counts?.available ?: 0
                        MeApi.StatusListMyProducts.OUT_OF_STOCK -> state.counts?.outOfStock ?: 0
                        MeApi.StatusListMyProducts.HIDDEN -> state.counts?.hidden ?: 0
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { onSelectStatus(tabItem.status) },
                        text = {
                            if (count > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(count.toString())
                                        }
                                    },
                                ) {
                                    Text(stringResource(tabItem.titleRes))
                                }
                            } else {
                                Text(stringResource(tabItem.titleRes))
                            }
                        },
                    )
                }
            }

            val refresh = products.loadState.refresh
            when {
                products.itemCount == 0 && refresh is LoadState.Loading -> Skeleton()
                products.itemCount == 0 && refresh is LoadState.Error -> {
                    ErrorPanel(
                        message = refresh.error.asAppError().userMessage(),
                        onRetry = products::retry,
                    )
                }
                products.itemCount == 0 -> {
                    val emptyMessage = when (state.selectedStatus) {
                        null -> stringResource(R.string.seller_products_empty_all)
                        MeApi.StatusListMyProducts.AVAILABLE -> stringResource(R.string.seller_products_empty_available)
                        MeApi.StatusListMyProducts.OUT_OF_STOCK -> stringResource(R.string.seller_products_empty_out_of_stock)
                        MeApi.StatusListMyProducts.HIDDEN -> stringResource(R.string.seller_products_empty_hidden)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            count = products.itemCount,
                            key = products.itemKey { it.id },
                        ) { index ->
                            products[index]?.let { product ->
                                SellerProductCard(
                                    product = product,
                                    onEdit = { onEditProduct(product.id) },
                                    onUpdateStatus = { newStatus -> onUpdateStatus(product.id, newStatus) },
                                    onDelete = { onOpenDeleteDialog(product) },
                                    isLoading = (state.actionInProgressId == product.id),
                                )
                            }
                        }

                        if (products.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.productToDelete != null) {
        DeleteProductDialog(
            productName = state.productToDelete.name,
            onConfirm = onDeleteProduct,
            onDismiss = onCloseDeleteDialog,
        )
    }
}
