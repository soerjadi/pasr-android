package id.oemah.nusantara.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.feature.ads.AdSlot
import id.oemah.nusantara.feature.ads.rememberLinkOpener
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun ConfirmationRoute(
    onViewOrder: (Long) -> Unit,
    onHome: () -> Unit,
    viewModel: ConfirmationViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val ads = viewModel.postPurchaseAds.collectAsStateWithLifecycle().value
    val openLink = rememberLinkOpener()
    StateHost(state = state, onRetry = viewModel::reload) { order ->
        ConfirmationScreen(order, ads, onViewOrder = { onViewOrder(order.id) }, onHome = onHome, onOpenLink = openLink)
    }
}

@Composable
fun ConfirmationScreen(
    order: Order,
    postPurchaseAds: List<Ad>,
    onViewOrder: () -> Unit,
    onHome: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("✓", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
        }
        Text(stringResource(R.string.confirm_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.confirm_number_label), style = MaterialTheme.typography.bodySmall)
        Text("#${order.number}", style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.confirm_message), textAlign = TextAlign.Center)
        Text(stringResource(R.string.checkout_cash), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        order.sellerOrders.forEach { SellerOrderCard(it, onOpenLink) }

        AdSlot(postPurchaseAds, onOpen = onOpenLink)

        PrimaryButton(text = stringResource(R.string.confirm_view_order), onClick = onViewOrder, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.confirm_home)) }
    }
}
