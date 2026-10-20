package id.oemah.nusantara.feature.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.ui.components.ErrorPanel
import id.oemah.nusantara.ui.components.Skeleton
import id.oemah.nusantara.ui.components.userMessage

@Composable
fun ReviewList(items: LazyPagingItems<Review>, modifier: Modifier = Modifier) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> Skeleton(modifier)
        items.itemCount == 0 && refresh is LoadState.Error ->
            ErrorPanel(refresh.error.asAppError().userMessage(), onRetry = items::retry)
        items.itemCount == 0 -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.seller_reviews_empty))
        }
        else -> LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { review ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(review.authorName, style = MaterialTheme.typography.titleSmall)
                        Text("★".repeat(review.rating), color = MaterialTheme.colorScheme.secondary)
                        review.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        HorizontalDivider(Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
