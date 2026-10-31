package id.oemah.nusantara.feature.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader

/** S1: what opening a store involves, before asking for anything. */
@Composable
fun OpenStoreIntroRoute(onBack: () -> Unit, onStart: () -> Unit, onLogin: () -> Unit, viewModel: OpenStoreIntroViewModel = hiltViewModel()) {
    val loggedIn = viewModel.loggedIn.collectAsStateWithLifecycle().value
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.store_cta_action), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("🏪", style = MaterialTheme.typography.headlineSmall)
                }
                Text(stringResource(R.string.store_intro_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.store_intro_body), style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.store_intro_steps), style = MaterialTheme.typography.titleMedium)
                Step(1, R.string.store_intro_step1_title, R.string.store_intro_step1_body)
                Step(2, R.string.store_intro_step2_title, R.string.store_intro_step2_body)
                Step(3, R.string.store_intro_step3_title, R.string.store_intro_step3_body)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(stringResource(R.string.store_cta_action), onClick = if (loggedIn) onStart else onLogin, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            if (!loggedIn) TextButton(onClick = onLogin) { Text(stringResource(R.string.store_intro_login)) }
            Text(stringResource(R.string.store_intro_footer), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Step(number: Int, title: Int, body: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
