package id.oemah.nusantara.feature.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.core.pick

/** Renders nothing for an empty list. A lone ad is a full-width card, several scroll as a carousel. */
@Composable
fun AdSlot(ads: List<Ad>, modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    when {
        ads.isEmpty() -> Unit
        ads.size == 1 -> AdCard(ads.single(), onOpen, modifier.testTag("ad_slot").fillMaxWidth())
        else -> LazyRow(
            modifier = modifier.testTag("ad_slot"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ads, key = { it.id }) { AdCard(it, onOpen, Modifier.width(280.dp)) }
        }
    }
}

@Composable
private fun AdCard(ad: Ad, onOpen: (String) -> Unit, modifier: Modifier) {
    Card(
        onClick = { if (SafeLinks.isOpenable(ad.targetUrl)) onOpen(ad.targetUrl) },
        modifier = modifier,
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ad.image.pick(320),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Sponsored content is always visibly labelled, even if the server sent a blank label.
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = ad.label.ifBlank { stringResource(R.string.ad_label_fallback) },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(ad.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ad.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
