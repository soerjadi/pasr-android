package id.oemah.nusantara.feature.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.userMessage
import kotlinx.coroutines.launch

/** S4 seller home: "Menunggu verifikasi" while the store is PENDING, otherwise the live dashboard. */
@Composable
fun SellerHomeRoute(
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    viewModel: SellerHomeViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val store = state.store ?: return

    SellerHomeContent(
        store = store,
        ownerName = viewModel.ownerName,
        summaryState = state.summaryState,
        statsState = state.statsState,
        selectedRange = state.selectedRange,
        isTogglingOpen = state.isTogglingOpen,
        actionError = state.actionError,
        onBack = onBack,
        onAddProduct = onAddProduct,
        onToggleOpen = viewModel::toggleStoreOpen,
        onSelectRange = viewModel::selectStatsRange,
        onDismissError = viewModel::dismissActionError,
    )
}

@Composable
fun SellerHomeContent(
    store: StoreSummary,
    ownerName: String?,
    summaryState: UiState<StoreSummaryResponse>,
    statsState: UiState<StoreStats>,
    selectedRange: MeApi.RangeGetMyStoreStats,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onToggleOpen: () -> Unit,
    onSelectRange: (MeApi.RangeGetMyStoreStats) -> Unit,
    actionError: String?,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    isTogglingOpen: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(actionError)
            }
            onDismissError()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ScreenHeader(
                title = stringResource(R.string.store_my_store),
                onBack = onBack,
                actions = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.seller_home_buyer_mode))
                    }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ownerName?.let {
                    Text(
                        stringResource(R.string.seller_home_greeting, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (store.status == StoreStatus.PENDING) {
                    PendingSection()
                    Section(stringResource(R.string.seller_home_meanwhile)) {
                        OutlinedButton(
                            onClick = onAddProduct,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("+ " + stringResource(R.string.seller_home_add_product))
                        }
                    }
                    Section(stringResource(R.string.seller_home_no_orders)) {
                        Text(
                            stringResource(R.string.seller_home_no_orders_pending),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    StoreStatusCard(
                        store = store,
                        summaryState = summaryState,
                        isTogglingOpen = isTogglingOpen,
                        onToggleOpen = onToggleOpen,
                    )

                    DailySummaryRow(summaryState = summaryState)

                    val newOrders = (summaryState as? UiState.Content)?.data?.newOrderCount ?: store.newOrderCount
                    val acceptBy = (summaryState as? UiState.Content)?.data?.acceptBy ?: store.acceptBy
                    if (newOrders > 0) {
                        Section(stringResource(R.string.store_new_orders, newOrders)) {
                            AcceptCountdown(acceptBy)
                        }
                    }

                    PerformanceStatsSection(
                        statsState = statsState,
                        selectedRange = selectedRange,
                        onSelectRange = onSelectRange,
                    )

                    OutlinedButton(
                        onClick = onAddProduct,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("+ " + stringResource(R.string.seller_home_add_product))
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun StoreStatusCard(
    store: StoreSummary,
    summaryState: UiState<StoreSummaryResponse>,
    isTogglingOpen: Boolean,
    onToggleOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (summaryState) {
                is UiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(store.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.store_verified),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Text(store.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        summaryState.error.userMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is UiState.Content -> {
                    val summary = summaryState.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(store.name, style = MaterialTheme.typography.titleMedium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(if (summary.isOpen) R.string.seller_home_status_open else R.string.seller_home_status_closed),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (summary.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                )
                                if (summary.isOpen && !summary.closesAt.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.seller_home_closes_at, summary.closesAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = summary.isOpen,
                            onCheckedChange = { onToggleOpen() },
                            enabled = !isTogglingOpen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryRow(
    summaryState: UiState<StoreSummaryResponse>,
    modifier: Modifier = Modifier,
) {
    when (summaryState) {
        is UiState.Loading -> Unit
        is UiState.Error -> Unit
        is UiState.Content -> {
            val summary = summaryState.data
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.seller_home_orders_today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = summary.ordersToday.toString(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.seller_home_sales_today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Rupiah(summary.salesToday).format(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceStatsSection(
    statsState: UiState<StoreStats>,
    selectedRange: MeApi.RangeGetMyStoreStats,
    onSelectRange: (MeApi.RangeGetMyStoreStats) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.seller_home_stats_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRange == MeApi.RangeGetMyStoreStats.TODAY,
                    onClick = { onSelectRange(MeApi.RangeGetMyStoreStats.TODAY) },
                    label = { Text(stringResource(R.string.seller_home_stats_today)) },
                )
                FilterChip(
                    selected = selectedRange == MeApi.RangeGetMyStoreStats.WEEK,
                    onClick = { onSelectRange(MeApi.RangeGetMyStoreStats.WEEK) },
                    label = { Text(stringResource(R.string.seller_home_stats_week)) },
                )
            }

            when (statsState) {
                is UiState.Loading -> Unit
                is UiState.Error -> {
                    Text(
                        text = statsState.error.userMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is UiState.Content -> {
                    val stats = statsState.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.seller_home_completed_orders_count, stats.completedOrders),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Total " + Rupiah(stats.salesValue).format(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (stats.topProducts.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.seller_home_top_products_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            stats.topProducts.take(3).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = stringResource(R.string.seller_home_units_sold, item.unitsSold),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSection() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.seller_home_pending_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(stringResource(R.string.seller_home_pending_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Section(stringResource(R.string.seller_home_status)) {
        StatusStep(done = true, active = false, R.string.seller_home_step_sent, R.string.seller_home_step_done)
        StatusStep(done = false, active = true, R.string.seller_home_step_review, R.string.seller_home_step_running)
        StatusStep(done = false, active = false, R.string.seller_home_step_listed, R.string.seller_home_step_after)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun StatusStep(done: Boolean, active: Boolean, title: Int, subtitle: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val color = if (done || active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (done) color else Color.Transparent), contentAlignment = Alignment.Center) {
            Text(if (done) "✓" else if (active) "●" else "○", color = if (done) MaterialTheme.colorScheme.onPrimary else color, style = MaterialTheme.typography.labelMedium)
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall)
        }
    }
}
