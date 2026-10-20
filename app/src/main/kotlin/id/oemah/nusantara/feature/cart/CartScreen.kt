package id.oemah.nusantara.feature.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.LineIssue
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.QuantityStepper
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.userMessage

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onCheckout: () -> Unit,
    onLoginRequired: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                CartEvent.LoginRequired -> onLoginRequired()
                CartEvent.OpenCheckout -> onCheckout()
            }
        }
    }
    CartScreen(
        state = state,
        onBack = onBack,
        onBrowse = onBrowse,
        onSetQuantity = viewModel::setQuantity,
        onRemove = viewModel::remove,
        onAdjust = viewModel::adjustToAvailable,
        onDismissNotice = viewModel::dismissNotice,
        onCheckout = viewModel::checkout,
    )
}

@Composable
fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onSetQuantity: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onAdjust: (Long) -> Unit,
    onDismissNotice: () -> Unit,
    onCheckout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.cart_title, state.groups.sumOf { it.lines.size }), onBack = onBack)
        if (state.isEmpty) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.cart_empty))
                PrimaryButton(text = stringResource(R.string.cart_browse), onClick = onBrowse)
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.notice?.let { notice ->
                item {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(notice, Modifier.weight(1f))
                            TextButton(onClick = onDismissNotice) { Text(stringResource(R.string.cart_notice_dismiss)) }
                        }
                    }
                }
            }
            items(state.groups, key = { it.sellerId }) { group ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.sellerName, style = MaterialTheme.typography.titleMedium)
                        group.pickupAddress?.let { Text(stringResource(R.string.cart_pickup_at, it), style = MaterialTheme.typography.bodySmall) }
                        group.lines.forEach { LineRow(it, onSetQuantity, onRemove, onAdjust) }
                    }
                }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.error != null -> Text(state.error.userMessage(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    state.validating -> Text(stringResource(R.string.cart_validating), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.titleMedium)
                    Text(state.total.format(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                PrimaryButton(
                    text = stringResource(R.string.cart_checkout),
                    onClick = onCheckout,
                    enabled = state.canCheckout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LineRow(
    ui: CartLineUi,
    onSetQuantity: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onAdjust: (Long) -> Unit,
) {
    val line = ui.line
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(line.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Text(line.name, style = MaterialTheme.typography.titleSmall)
                Text(line.unitPrice.format(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                QuantityStepper(value = line.quantity, min = 1, max = 99, onChange = { onSetQuantity(line.id, it) })
            }
            IconButton(onClick = { onRemove(line.id) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cart_remove))
            }
        }
        when (ui.issue) {
            LineIssue.OUT_OF_STOCK -> IssueText(stringResource(R.string.cart_issue_out_of_stock))
            LineIssue.PRODUCT_UNAVAILABLE -> IssueText(stringResource(R.string.cart_issue_unavailable))
            LineIssue.INSUFFICIENT_STOCK -> {
                IssueText(stringResource(R.string.cart_issue_insufficient, ui.availableStock ?: 0))
                TextButton(onClick = { onAdjust(line.id) }) { Text(stringResource(R.string.cart_adjust)) }
            }
            null -> Unit
        }
    }
}

@Composable
private fun IssueText(text: String) = Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
