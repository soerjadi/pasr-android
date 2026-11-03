package id.oemah.nusantara.feature.orders.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.DateFormat
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.feature.orders.label

@Composable
fun StoreOrderCard(
    order: StoreOrder,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onReady: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: Number, Date/Time, and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.number,
                        style = MaterialTheme.typography.titleMedium,
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

            // Buyer Name
            Text(
                text = order.buyerName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Items Summary
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                order.items.forEach { item ->
                    Text(
                        text = stringResource(R.string.checkout_line, item.quantity, item.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Rupiah(order.subtotal).format(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Status CREATED: Countdown / AcceptBy Banner
            if (order.status == OrderStatus.CREATED && order.acceptBy != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
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

            // Status READY_FOR_PICKUP: Pickup note callout
            if (order.status == OrderStatus.READY_FOR_PICKUP && !order.note.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = order.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            // Action Buttons
            when (order.status) {
                OrderStatus.CREATED -> {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.seller_orders_reject))
                        }

                        Button(
                            onClick = onAccept,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isLoading) {
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
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onReady,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
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
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onComplete,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
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

                OrderStatus.COMPLETED, OrderStatus.CANCELLED -> {
                    // Informational status pill only
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
