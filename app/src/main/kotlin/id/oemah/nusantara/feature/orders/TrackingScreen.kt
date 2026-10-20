package id.oemah.nusantara.feature.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.DateFormat
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.feature.ads.AdSlot
import id.oemah.nusantara.feature.ads.rememberLinkOpener
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun TrackingRoute(onBack: () -> Unit, viewModel: TrackingViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val waitAds = viewModel.waitAds.collectAsStateWithLifecycle().value
    val refreshing = viewModel.refreshing.collectAsStateWithLifecycle().value
    val openLink = rememberLinkOpener()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it is UiEvent.Message) snackbar.showSnackbar(it.text) }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.tracking_title), onBack = onBack)
            StateHost(state = state, onRetry = viewModel::reload) { order ->
                TrackingScreen(order, waitAds, refreshing, onRefresh = viewModel::refresh, onOpenLink = openLink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    order: Order,
    waitAds: List<Ad>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column {
                    Text("#${order.number}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        DateFormat.date(order.createdAt) + " - " + stringResource(R.string.tracking_total, Rupiah(order.total).format()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item { Timeline(OrderTimeline.steps(order)) }
            if (waitAds.isNotEmpty()) item { AdSlot(waitAds, onOpen = onOpenLink) }
            items(order.sellerOrders, key = { it.id }) { SellerOrderCard(it, onOpenLink) }
        }
    }
}

@Composable
private fun Timeline(steps: List<TimelineStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEach { step ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val color = when (step.state) {
                    StepState.DONE -> MaterialTheme.colorScheme.primary
                    StepState.CURRENT -> MaterialTheme.colorScheme.secondary
                    StepState.PENDING -> MaterialTheme.colorScheme.outlineVariant
                    StepState.CANCELLED -> MaterialTheme.colorScheme.error
                }
                Box(Modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    Text(
                        when (step.state) {
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
                        step.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (step.state == StepState.CURRENT) FontWeight.Bold else FontWeight.Normal,
                    )
                    step.at?.let { Text(DateFormat.dateTime(it), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
