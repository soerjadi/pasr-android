package id.oemah.nusantara.feature.orders.seller

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.ui.components.ErrorPanel
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.Skeleton
import id.oemah.nusantara.ui.components.userMessage
import kotlinx.coroutines.launch

private enum class SellerOrderTabItem(
    val tab: MeApi.TabListMyStoreOrders,
    @StringRes val titleRes: Int,
) {
    NEW(MeApi.TabListMyStoreOrders.NEW, R.string.seller_orders_tab_new),
    PROCESSING(MeApi.TabListMyStoreOrders.PROCESSING, R.string.seller_orders_tab_processing),
    DONE(MeApi.TabListMyStoreOrders.DONE, R.string.seller_orders_tab_done),
}

@Composable
fun SellerOrdersRoute(
    onBack: () -> Unit,
    onOpenOrder: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SellerOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val orders = viewModel.orders.collectAsLazyPagingItems()

    SellerOrdersContent(
        state = state,
        orders = orders,
        onBack = onBack,
        onOpenOrder = onOpenOrder,
        onSelectTab = viewModel::selectTab,
        onAcceptOrder = viewModel::acceptOrder,
        onOpenRejectDialog = viewModel::openRejectDialog,
        onCloseRejectDialog = viewModel::closeRejectDialog,
        onRejectOrder = viewModel::rejectOrder,
        onOpenReadyDialog = viewModel::openReadyDialog,
        onCloseReadyDialog = viewModel::closeReadyDialog,
        onReadyOrder = viewModel::readyOrder,
        onCompleteOrder = viewModel::completeOrder,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}

@Composable
fun SellerOrdersContent(
    state: SellerOrdersState,
    orders: LazyPagingItems<StoreOrder>,
    onBack: () -> Unit,
    onOpenOrder: (Long) -> Unit,
    onSelectTab: (MeApi.TabListMyStoreOrders) -> Unit,
    onAcceptOrder: (Long) -> Unit,
    onOpenRejectDialog: (StoreOrder) -> Unit,
    onCloseRejectDialog: () -> Unit,
    onRejectOrder: (Long, RejectRequest.Reason, String?) -> Unit,
    onOpenReadyDialog: (StoreOrder) -> Unit,
    onCloseReadyDialog: () -> Unit,
    onReadyOrder: (Long, String?) -> Unit,
    onCompleteOrder: (Long) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionError) {
        if (state.actionError != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(state.actionError)
            }
            onDismissError()
        }
    }

    val selectedTabIndex = SellerOrderTabItem.entries
        .indexOfFirst { it.tab == state.selectedTab }
        .coerceAtLeast(0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.seller_orders_title),
                onBack = onBack,
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
                SellerOrderTabItem.entries.forEach { tabItem ->
                    val isSelected = tabItem.tab == state.selectedTab
                    val count = when (tabItem.tab) {
                        MeApi.TabListMyStoreOrders.NEW -> state.counts?.new ?: 0
                        MeApi.TabListMyStoreOrders.PROCESSING -> state.counts?.processing ?: 0
                        MeApi.TabListMyStoreOrders.DONE -> state.counts?.done ?: 0
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { onSelectTab(tabItem.tab) },
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

            val refresh = orders.loadState.refresh
            when {
                orders.itemCount == 0 && refresh is LoadState.Loading -> Skeleton()
                orders.itemCount == 0 && refresh is LoadState.Error -> {
                    ErrorPanel(
                        message = refresh.error.asAppError().userMessage(),
                        onRetry = orders::retry,
                    )
                }
                orders.itemCount == 0 -> {
                    val emptyMessage = when (state.selectedTab) {
                        MeApi.TabListMyStoreOrders.NEW -> stringResource(R.string.seller_orders_empty_new)
                        MeApi.TabListMyStoreOrders.PROCESSING -> stringResource(R.string.seller_orders_empty_processing)
                        MeApi.TabListMyStoreOrders.DONE -> stringResource(R.string.seller_orders_empty_done)
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
                        items(count = orders.itemCount, key = orders.itemKey { it.id }) { index ->
                            orders[index]?.let { order ->
                                StoreOrderCard(
                                    order = order,
                                    onClick = { onOpenOrder(order.id) },
                                    onAccept = { onAcceptOrder(order.id) },
                                    onReject = { onOpenRejectDialog(order) },
                                    onReady = { onOpenReadyDialog(order) },
                                    onComplete = { onCompleteOrder(order.id) },
                                    isLoading = (state.actionInProgressId == order.id),
                                )
                            }
                        }

                        if (orders.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        (orders.loadState.append as? LoadState.Error)?.let { error ->
                            item {
                                TextButton(
                                    onClick = orders::retry,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "${error.error.asAppError().userMessage()} ${stringResource(R.string.retry)}",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.activeRejectOrder != null) {
        RejectOrderDialog(
            order = state.activeRejectOrder,
            onConfirm = { reason, note ->
                onRejectOrder(state.activeRejectOrder.id, reason, note)
            },
            onDismiss = onCloseRejectDialog,
        )
    }

    if (state.activeReadyOrder != null) {
        ReadyOrderDialog(
            order = state.activeReadyOrder,
            onConfirm = { note ->
                onReadyOrder(state.activeReadyOrder.id, note)
            },
            onDismiss = onCloseReadyDialog,
        )
    }
}
