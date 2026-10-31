package id.oemah.nusantara.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.PrimaryButton

/**
 * S3 "Tambah produk". [onboarding] marks it as step 2 of opening the store, where the product can also be skipped;
 * [onDone] runs after a save or a skip.
 */
@Composable
fun AddProductRoute(onboarding: Boolean, onBack: () -> Unit, onDone: () -> Unit, viewModel: AddProductViewModel = hiltViewModel()) {
    val form = viewModel.form.collectAsStateWithLifecycle().value
    val submitting = viewModel.submitting.collectAsStateWithLifecycle().value
    val error = viewModel.error.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) { viewModel.saved.collect { onDone() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        FormHeader(
            stringResource(R.string.product_form_title),
            if (onboarding) stringResource(R.string.product_form_step) else "",
            progress = 1f,
            onBack = onBack,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.product_form_name)) },
                placeholder = { Text(stringResource(R.string.product_form_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.price,
                onValueChange = viewModel::setPrice,
                label = { Text(stringResource(R.string.product_form_price)) },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { viewModel.stepStock(-1) }, modifier = Modifier.padding(top = 8.dp)) { Text("−") }
                OutlinedTextField(
                    value = form.stock,
                    onValueChange = viewModel::setStock,
                    label = { Text(stringResource(R.string.product_form_stock)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { viewModel.stepStock(1) }, modifier = Modifier.padding(top = 8.dp)) { Text("+") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PrimaryButton(
                stringResource(R.string.product_form_submit),
                onClick = viewModel::submit,
                enabled = form.toRequest() != null && !submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onboarding) TextButton(onClick = onDone) { Text(stringResource(R.string.product_form_skip)) }
        }
    }
}
