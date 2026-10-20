package id.oemah.nusantara.feature.orders.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.core.DateFormat
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.pick
import id.oemah.nusantara.feature.orders.OrderTab
import id.oemah.nusantara.feature.orders.emptyMessage
import id.oemah.nusantara.feature.orders.historyChip
import id.oemah.nusantara.feature.orders.thumbnails
import id.oemah.nusantara.ui.components.ErrorPanel
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.Skeleton
import id.oemah.nusantara.ui.components.userMessage

@Composable
fun OrderHistoryRoute(
    onOpenOrder: (Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel(),
) {
    val tab = viewModel.selectedTab.collectAsStateWithLifecycle().value
    val loggedIn = viewModel.loggedIn.collectAsStateWithLifecycle().value
    val items = viewModel.orders.collectAsLazyPagingItems()

    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.orders_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        if (!loggedIn) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.orders_login_prompt))
                PrimaryButton(text = stringResource(R.string.orders_login_action), onClick = onLogin)
            }
            return@Column
        }
        TabRow(selectedTabIndex = tab.ordinal) {
            OrderTab.entries.forEach { entry ->
                Tab(selected = entry == tab, onClick = { viewModel.select(entry) }, text = { Text(entry.label) })
            }
        }
        OrderList(items, tab, onOpenOrder)
    }
}

@Composable
private fun OrderList(items: LazyPagingItems<Order>, tab: OrderTab, onOpenOrder: (Long) -> Unit) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> Skeleton()
        items.itemCount == 0 && refresh is LoadState.Error -> ErrorPanel(refresh.error.asAppError().userMessage(), onRetry = items::retry)
        items.itemCount == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tab.emptyMessage()) }
        else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { OrderRow(it) { onOpenOrder(it.id) } }
            }
            if (items.loadState.append is LoadState.Loading) {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            (items.loadState.append as? LoadState.Error)?.let { error ->
                item {
                    TextButton(onClick = items::retry, modifier = Modifier.fillMaxWidth()) {
                        Text(error.error.asAppError().userMessage() + " " + stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${order.number}", style = MaterialTheme.typography.titleSmall)
                StatusChip(order.status)
            }
            Text(DateFormat.date(order.createdAt), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                order.thumbnails().forEach {
                    AsyncImage(it.pick(320), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
                }
            }
            Text(Rupiah(order.total).format(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(status.historyChip(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
