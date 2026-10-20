package id.oemah.nusantara.feature.review

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader

@Composable
fun WriteReviewRoute(onClose: () -> Unit, viewModel: WriteReviewViewModel = hiltViewModel()) {
    val rating = viewModel.rating.collectAsStateWithLifecycle().value
    val text = viewModel.text.collectAsStateWithLifecycle().value
    val error = viewModel.error.collectAsStateWithLifecycle().value
    val canSubmit = viewModel.canSubmit.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                is ReviewEvent.Done -> {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                    onClose()
                }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.review_title), onBack = onClose)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(viewModel.productName, style = MaterialTheme.typography.titleMedium)
            Row {
                (1..5).forEach { star ->
                    val description = stringResource(R.string.review_star, star)
                    IconButton(
                        onClick = { viewModel.setRating(star) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = description },
                    ) {
                        Text(
                            "★",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (star <= rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = viewModel::setText,
                placeholder = { Text(stringResource(R.string.review_hint)) },
                supportingText = { Text("${text.length}/500") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(
                text = stringResource(R.string.review_submit),
                onClick = viewModel::submit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
