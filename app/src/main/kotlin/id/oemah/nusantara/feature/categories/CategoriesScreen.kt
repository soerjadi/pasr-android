package id.oemah.nusantara.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun CategoriesRoute(onOpenCategory: (Category) -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Column {
        Text(
            stringResource(R.string.categories_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        StateHost(state = state, onRetry = viewModel::reload) { categories ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    Card(onClick = { onOpenCategory(category) }, modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category.emoji(), style = MaterialTheme.typography.headlineMedium)
                            Text(category.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
