package id.oemah.nusantara.feature.store.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.pick

@Composable
fun SellerProductCard(
    product: SellerProduct,
    onEdit: () -> Unit,
    onUpdateStatus: (UpdateProductRequest.Status) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Card(
        onClick = onEdit,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val imageUrl = product.images.firstOrNull()?.pick(320)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(status = product.status)
                    }

                    Text(
                        text = Rupiah(product.price).format(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = stringResource(R.string.detail_stock, product.stock),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("SellerProductCard_Loading"),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                }

                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(stringResource(R.string.seller_products_edit))
                }

                Spacer(Modifier.width(4.dp))

                Box {
                    var menuExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { menuExpanded = true },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("SellerProductCard_MoreOptions"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi lainnya",
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (product.status != SellerProduct.Status.AVAILABLE) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.seller_products_mark_available)) },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateStatus(UpdateProductRequest.Status.AVAILABLE)
                                },
                            )
                        }

                        if (product.status != SellerProduct.Status.OUT_OF_STOCK) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.seller_products_mark_out_of_stock)) },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateStatus(UpdateProductRequest.Status.OUT_OF_STOCK)
                                },
                            )
                        }

                        if (product.status != SellerProduct.Status.HIDDEN) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.seller_products_mark_hidden)) },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateStatus(UpdateProductRequest.Status.HIDDEN)
                                },
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.seller_products_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: SellerProduct.Status,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        SellerProduct.Status.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
        SellerProduct.Status.OUT_OF_STOCK -> MaterialTheme.colorScheme.errorContainer
        SellerProduct.Status.HIDDEN -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        SellerProduct.Status.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer
        SellerProduct.Status.OUT_OF_STOCK -> MaterialTheme.colorScheme.onErrorContainer
        SellerProduct.Status.HIDDEN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textRes = when (status) {
        SellerProduct.Status.AVAILABLE -> R.string.seller_products_tab_available
        SellerProduct.Status.OUT_OF_STOCK -> R.string.seller_products_tab_out_of_stock
        SellerProduct.Status.HIDDEN -> R.string.seller_products_tab_hidden
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
