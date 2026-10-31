package id.oemah.nusantara.feature.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import kotlinx.coroutines.delay

/** Home (B3): the seller's pending orders at a glance, with a short path into seller mode. */
@Composable
fun StoreWidget(store: StoreSummary, onOpenSellerMode: () -> Unit, modifier: Modifier = Modifier) {
    StoreFrame(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreIcon()
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.store_widget_kicker, store.name), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (store.newOrderCount > 0) stringResource(R.string.store_new_orders, store.newOrderCount) else stringResource(R.string.store_no_new_orders),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (store.newOrderCount > 0) CountBadge(store.newOrderCount)
        }
        if (store.newOrderCount > 0) AcceptCountdown(store.acceptBy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenSellerMode, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.store_view_orders))
            }
            OutlinedButton(
                onClick = onOpenSellerMode,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            ) { Text(stringResource(R.string.store_seller_mode)) }
        }
    }
}

/** Account (B14), user has a store. */
@Composable
fun MyStoreCard(store: StoreSummary, onOpenSellerMode: () -> Unit, modifier: Modifier = Modifier) {
    StoreFrame(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreIcon()
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.store_my_store), style = MaterialTheme.typography.labelMedium)
                Text(store.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (store.status == StoreStatus.VERIFIED) R.string.store_verified else R.string.store_pending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (store.newOrderCount > 0) AcceptBanner(stringResource(R.string.store_new_orders, store.newOrderCount))
        Button(onClick = onOpenSellerMode, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.store_open_seller_mode))
        }
        Text(stringResource(R.string.store_mode_note), style = MaterialTheme.typography.bodySmall)
    }
}

/** Account (B14), user has no store yet. */
@Composable
fun OpenStoreCard(onOpenStore: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StoreIcon(background = MaterialTheme.colorScheme.surface)
                Column {
                    Text(stringResource(R.string.store_cta_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.store_cta_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = onOpenStore,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary),
            ) { Text(stringResource(R.string.store_cta_action)) }
            Text(stringResource(R.string.store_cta_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StoreFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun StoreIcon(background: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(background), contentAlignment = Alignment.Center) {
        Text("🏪", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Text(count.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun AcceptBanner(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Ticks once a second; disappears at zero because the server cancels the order then and the next refresh drops the count. */
@Composable
internal fun AcceptCountdown(acceptBy: String?) {
    val left by produceState(secondsUntil(acceptBy), acceptBy) {
        while (acceptBy != null) {
            value = secondsUntil(acceptBy)
            delay(1_000)
        }
    }
    left?.takeIf { it > 0 }?.let { AcceptBanner(stringResource(R.string.store_accept_within, formatCountdown(it))) }
}
