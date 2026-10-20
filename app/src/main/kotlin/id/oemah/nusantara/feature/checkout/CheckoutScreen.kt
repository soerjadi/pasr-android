package id.oemah.nusantara.feature.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.StateHost
import id.oemah.nusantara.ui.components.userMessage

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onPlaced: (orderId: Long) -> Unit,
    onBackToCart: () -> Unit,
    onLoginRequired: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val review = viewModel.review.collectAsStateWithLifecycle().value
    val submission = viewModel.submission.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                CheckoutEvent.LoginRequired -> onLoginRequired()
                is CheckoutEvent.BackToCart -> {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                    onBackToCart()
                }
            }
        }
    }
    LaunchedEffect(submission) {
        if (submission is Submission.Placed) onPlaced(submission.order.id)
    }
    CheckoutScreen(
        review = review,
        submission = submission,
        onPlaceOrder = viewModel::placeOrder,
        onRetryReview = viewModel::reload,
        onBack = onBack,
    )
}

@Composable
fun CheckoutScreen(
    review: UiState<CartValidation>,
    submission: Submission,
    onPlaceOrder: () -> Unit,
    onRetryReview: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.checkout_title), onBack = onBack)
        StateHost(state = review, onRetry = onRetryReview, modifier = Modifier.weight(1f)) { validation ->
            val hasIssues = validation.groups.any { g -> g.lines.any { it.issue != null } }
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    validation.groups.forEach { group ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.checkout_pickup), style = MaterialTheme.typography.titleSmall)
                                Text(group.seller.name, style = MaterialTheme.typography.titleMedium)
                                Text(group.seller.pickupAddress)
                                Text(group.seller.openingHours, style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                                group.lines.forEach { line ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(R.string.checkout_line, line.quantity, line.name), Modifier.weight(1f))
                                        Text(Rupiah(line.lineTotal).format())
                                    }
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.checkout_payment), style = MaterialTheme.typography.titleSmall)
                    PaymentOption(selected = true, enabled = true, title = stringResource(R.string.checkout_cash), note = null)
                    PaymentOption(selected = false, enabled = false, title = stringResource(R.string.checkout_qris), note = stringResource(R.string.checkout_qris_soon))
                }
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasIssues) Text(stringResource(R.string.checkout_has_issues), color = MaterialTheme.colorScheme.error)
                        (submission as? Submission.Failed)?.let {
                            Text(it.error.userMessage(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.titleMedium)
                            Text(Rupiah(validation.total).format(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        PrimaryButton(
                            text = stringResource(if (submission is Submission.Submitting) R.string.checkout_submitting else R.string.checkout_place),
                            onClick = onPlaceOrder,
                            enabled = !hasIssues && validation.groups.isNotEmpty() && submission !is Submission.Submitting && submission !is Submission.Placed,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOption(selected: Boolean, enabled: Boolean, title: String, note: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
