package id.oemah.nusantara.feature.cart

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R

@Composable
fun CartButton(onClick: () -> Unit, modifier: Modifier = Modifier, viewModel: CartBadgeViewModel = hiltViewModel()) {
    val count = viewModel.count.collectAsStateWithLifecycle().value
    BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }, modifier = modifier) {
        IconButton(onClick = onClick, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.cart_open))
        }
    }
}
