package id.oemah.nusantara.feature.orders.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.DateFormat
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.feature.orders.OrderTimeline
import id.oemah.nusantara.feature.orders.StepState
import id.oemah.nusantara.feature.orders.TimelineStep
import id.oemah.nusantara.feature.orders.label
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.StateHost
import kotlinx.coroutines.launch

@Composable
fun SellerOrderDetailRoute(
    onBack: () -> Unit,
    viewModel: SellerOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SellerOrderDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::loadOrder,
        onAccept = viewModel::acceptOrder,
        onOpenRejectDialog = viewModel::openRejectDialog,
        onCloseRejectDialog = viewModel::closeRejectDialog,
        onReject = viewModel::rejectOrder,
        onOpenReadyDialog = viewModel::openReadyDialog,
        onCloseReadyDialog = viewModel::closeReadyDialog,
        onReady = viewModel::readyOrder,
        onComplete = viewModel::completeOrder,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
fun SellerOrderDetailContent(
    state: SellerOrderDetailState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAccept: () -> Unit,
    onOpenRejectDialog: () -> Unit,
    onCloseRejectDialog: () -> Unit,
    onReject: (RejectRequest.Reason, String?) -> Unit,
    onOpenReadyDialog: () -> Unit,
    onCloseReadyDialog: () -> Unit,
    onReady: (String?) -> Unit,
    onComplete: () -> Unit,
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

    val currentOrder = (state.orderState as? UiState.Content)?.value
    val title = currentOrder?.number ?: stringResource(R.string.orders_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = title,
                onBack = onBack,
            )
        },
        bottomBar = {
            if (currentOrder != null && currentOrder.status !in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED)) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        when (currentOrder.status) {
                            OrderStatus.CREATED -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenRejectDialog,
                                        enabled = !state.isActionInProgress,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(R.string.seller_orders_reject))
                                    }

                                    Button(
                                        onClick = onAccept,
                                        enabled = !state.isActionInProgress,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (state.isActionInProgress) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(stringResource(R.string.seller_orders_accept))
                                    }
                                }
                            }

                            OrderStatus.SELLER_PROCESSING -> {
                                Button(
                                    onClick = onOpenReadyDialog,
                                    enabled = !state.isActionInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (state.isActionInProgress) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(stringResource(R.string.seller_orders_ready))
                                }
                            }

                            OrderStatus.READY_FOR_PICKUP -> {
                                Button(
                                    onClick = onComplete,
                                    enabled = !state.isActionInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (state.isActionInProgress) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(stringResource(R.string.seller_orders_complete))
                                }
                            }

                            OrderStatus.COMPLETED, OrderStatus.CANCELLED -> Unit
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        StateHost(
            state = state.orderState,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { order ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Buyer Info and Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.buyerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = DateFormat.dateTime(order.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Surface(
                                color = statusContainerColor(order.status),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = order.status.label(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusContentColor(order.status),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                // Acceptance Deadline Warning
                if (order.status == OrderStatus.CREATED && order.acceptBy != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Batas konfirmasi: " + DateFormat.dateTime(order.acceptBy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // Customer Pickup Note Callout
                if (!order.note.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Catatan Pengambilan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = order.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Itemized Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Rincian Pesanan",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        HorizontalDivider()
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.quantity} × ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "@ ${Rupiah(item.unitPrice).format()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = Rupiah(item.lineTotal).format(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Total Subtotal",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = Rupiah(order.subtotal).format(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Order Status History Timeline
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Status Pesanan",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        HorizontalDivider()
                        SellerOrderTimeline(OrderTimeline.steps(order))
                    }
                }
            }
        }
    }

    if (currentOrder != null) {
        if (state.showRejectDialog) {
            RejectOrderDialog(
                order = currentOrder,
                onConfirm = onReject,
                onDismiss = onCloseRejectDialog,
            )
        }

        if (state.showReadyDialog) {
            ReadyOrderDialog(
                order = currentOrder,
                onConfirm = onReady,
                onDismiss = onCloseReadyDialog,
            )
        }
    }
}

@Composable
private fun SellerOrderTimeline(steps: List<TimelineStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEach { step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val color = when (step.state) {
                    StepState.DONE -> MaterialTheme.colorScheme.primary
                    StepState.CURRENT -> MaterialTheme.colorScheme.secondary
                    StepState.PENDING -> MaterialTheme.colorScheme.outlineVariant
                    StepState.CANCELLED -> MaterialTheme.colorScheme.error
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (step.state) {
                            StepState.DONE -> "✓"
                            StepState.CANCELLED -> "✕"
                            else -> ""
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (step.state == StepState.CURRENT) FontWeight.Bold else FontWeight.Normal,
                    )
                    step.at?.let {
                        Text(
                            text = DateFormat.dateTime(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusContainerColor(status: OrderStatus) = when (status) {
    OrderStatus.CREATED -> MaterialTheme.colorScheme.tertiaryContainer
    OrderStatus.SELLER_PROCESSING -> MaterialTheme.colorScheme.primaryContainer
    OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.secondaryContainer
    OrderStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun statusContentColor(status: OrderStatus) = when (status) {
    OrderStatus.CREATED -> MaterialTheme.colorScheme.onTertiaryContainer
    OrderStatus.SELLER_PROCESSING -> MaterialTheme.colorScheme.onPrimaryContainer
    OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.onSecondaryContainer
    OrderStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
}
