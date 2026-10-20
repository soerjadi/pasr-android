package id.oemah.nusantara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.pick

@Composable
fun ProductCard(
    product: ProductSummary,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val addDescription = stringResource(R.string.product_add_to_cart_description)
    Card(onClick = onOpen, modifier = modifier) {
        AsyncImage(
            model = product.image.pick(320),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.sellerName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.rating.toString(), style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(Rupiah(product.price).format(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (product.stock <= 0) {
                        Text(stringResource(R.string.product_sold_out), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                FilledIconButton(
                    onClick = onAdd,
                    enabled = product.stock > 0,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = addDescription },
                ) {
                    Text("+")
                }
            }
        }
    }
}
