package id.oemah.nusantara.feature.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.ProductGrid

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val text = viewModel.queryText.collectAsStateWithLifecycle().value
    val idle = viewModel.isIdle.collectAsStateWithLifecycle().value
    val items = viewModel.results.collectAsLazyPagingItems()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            OutlinedTextField(
                value = text,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp).focusRequester(focus),
            )
        }
        if (idle) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.search_idle)) }
        } else {
            ProductGrid(
                items = items,
                emptyText = stringResource(R.string.products_empty),
                onOpen = onOpenProduct,
                onAdd = viewModel::addToCart,
            )
        }
    }
}
