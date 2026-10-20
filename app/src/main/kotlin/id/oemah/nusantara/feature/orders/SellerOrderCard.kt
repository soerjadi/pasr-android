package id.oemah.nusantara.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.OrderItem
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.SellerOrder
import id.oemah.nusantara.core.IndonesianPhone

/** One seller's part of an order: its own status, where and when to collect it, and a way to reach the seller. */
@Composable
fun SellerOrderCard(
    sellerOrder: SellerOrder,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReview: ((OrderItem) -> Unit)? = null,
) {
    val chatUrl = IndonesianPhone.toWhatsAppUrl(sellerOrder.whatsapp)
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sellerOrder.sellerName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(sellerOrder.status.label(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text(stringResource(R.string.checkout_pickup), style = MaterialTheme.typography.titleSmall)
            Text(sellerOrder.pickupAddress)
            Text(sellerOrder.openingHours, style = MaterialTheme.typography.bodySmall)
            sellerOrder.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            sellerOrder.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.checkout_line, item.quantity, item.name), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    if (onReview != null && sellerOrder.status == OrderStatus.COMPLETED && !item.reviewed) {
                        TextButton(onClick = { onReview(item) }) { Text(stringResource(R.string.review_action)) }
                    }
                }
            }
            if (chatUrl != null) {
                OutlinedButton(onClick = { onOpenLink(chatUrl) }) { Text(stringResource(R.string.tracking_chat)) }
            }
        }
    }
}
