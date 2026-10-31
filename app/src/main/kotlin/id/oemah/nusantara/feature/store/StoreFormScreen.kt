package id.oemah.nusantara.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.userMessage

/** S2 "Data toko". */
@Composable
fun StoreFormRoute(onBack: () -> Unit, onCreated: () -> Unit, viewModel: StoreFormViewModel = hiltViewModel()) {
    val form = viewModel.form.collectAsStateWithLifecycle().value
    val categories = viewModel.categories.collectAsStateWithLifecycle().value
    val submitting = viewModel.submitting.collectAsStateWithLifecycle().value
    val error = viewModel.error.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) { viewModel.created.collect { onCreated() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        FormHeader(stringResource(R.string.store_form_title), stringResource(R.string.store_form_step), progress = 0.5f, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { viewModel.edit { copy(name = it) } },
                label = { Text(stringResource(R.string.store_form_name)) },
                placeholder = { Text(stringResource(R.string.store_form_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when (categories) {
                UiState.Loading -> Text(stringResource(R.string.store_form_category_hint), style = MaterialTheme.typography.bodySmall)
                is UiState.Error -> {
                    Text(categories.error.userMessage(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::loadCategories) { Text(stringResource(R.string.retry)) }
                }
                is UiState.Content -> SelectField(
                    label = stringResource(R.string.store_form_category),
                    placeholder = stringResource(R.string.store_form_category_hint),
                    selected = categories.value.firstOrNull { it.id == form.categoryId }?.name,
                    options = categories.value.map { it.name },
                    onSelect = { index -> viewModel.edit { copy(categoryId = categories.value[index].id) } },
                )
            }
            OutlinedTextField(
                value = form.ownerName,
                onValueChange = { viewModel.edit { copy(ownerName = it) } },
                label = { Text(stringResource(R.string.store_form_owner)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.phone,
                onValueChange = {},
                label = { Text(stringResource(R.string.store_form_phone)) },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SelectField(
                label = stringResource(R.string.store_form_kelurahan),
                placeholder = stringResource(R.string.store_form_kelurahan_hint),
                selected = form.kelurahan,
                options = KELURAHAN,
                onSelect = { index -> viewModel.edit { copy(kelurahan = KELURAHAN[index]) } },
            )
            OutlinedTextField(
                value = form.address,
                onValueChange = { viewModel.edit { copy(address = it) } },
                label = { Text(stringResource(R.string.store_form_address)) },
                placeholder = { Text(stringResource(R.string.store_form_address_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        PrimaryButton(
            stringResource(R.string.store_form_submit),
            onClick = viewModel::submit,
            enabled = form.toRequest() != null && !submitting,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
internal fun FormHeader(title: String, step: String, progress: Float, onBack: () -> Unit) {
    Column {
        ScreenHeader(title, onBack)
        Text(step, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 56.dp, bottom = 8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(label: String, placeholder: String, selected: String?, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}
