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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader

/** S4 seller home: "Menunggu verifikasi" while the store is PENDING, otherwise the new-orders card. */
@Composable
fun SellerHomeRoute(onBack: () -> Unit, onAddProduct: () -> Unit, viewModel: SellerHomeViewModel = hiltViewModel()) {
    val store = viewModel.store.collectAsStateWithLifecycle().value ?: return
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(store.name, onBack, actions = { TextButton(onClick = onBack) { Text(stringResource(R.string.seller_home_buyer_mode)) } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            viewModel.ownerName?.let { Text(stringResource(R.string.seller_home_greeting, it), style = MaterialTheme.typography.bodyMedium) }
            if (store.status == StoreStatus.PENDING) PendingSection() else NewOrdersSection(store)
            Section(stringResource(if (store.status == StoreStatus.PENDING) R.string.seller_home_meanwhile else R.string.store_my_store)) {
                OutlinedButton(
                    onClick = onAddProduct,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("+ " + stringResource(R.string.seller_home_add_product)) }
            }
            if (store.newOrderCount == 0) {
                Section(stringResource(R.string.seller_home_no_orders)) {
                    Text(
                        stringResource(if (store.status == StoreStatus.PENDING) R.string.seller_home_no_orders_pending else R.string.seller_home_no_orders_verified),
                        style = MaterialTheme.typography.bodySmall,
                    )
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
private fun NewOrdersSection(store: StoreSummary) {
    if (store.newOrderCount == 0) return
    Section(stringResource(R.string.store_new_orders, store.newOrderCount)) { AcceptCountdown(store.acceptBy) }
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
