package id.oemah.nusantara.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.BuildConfig
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.feature.ads.rememberLinkOpener
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.StateHost

@Composable
fun ProfileRoute(onOpenOrders: () -> Unit, onLogin: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val loggedIn = viewModel.loggedIn.collectAsStateWithLifecycle().value
    val user = viewModel.user.collectAsStateWithLifecycle().value
    val openLink = rememberLinkOpener()
    val snackbar = remember { SnackbarHostState() }
    var showAbout by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it is UiEvent.Message) snackbar.showSnackbar(it.text) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(stringResource(R.string.tab_account), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            if (!loggedIn) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.profile_guest))
                    PrimaryButton(text = stringResource(R.string.orders_login_action), onClick = onLogin)
                    SupportAndAbout(viewModel.supportUrl, openLink) { showAbout = true }
                }
            } else {
                StateHost(state = user, onRetry = viewModel::reload) { current ->
                    Column(Modifier.fillMaxSize()) {
                        Header(current) { editingName = true }
                        HorizontalDivider()
                        MenuRow(stringResource(R.string.profile_my_orders), onOpenOrders)
                        SupportAndAbout(viewModel.supportUrl, openLink) { showAbout = true }
                        MenuRow(stringResource(R.string.profile_logout), viewModel::logout)
                    }
                    if (editingName) {
                        NameDialog(
                            initial = current.name.orEmpty(),
                            onSave = { viewModel.updateName(it); editingName = false },
                            onDismiss = { editingName = false },
                        )
                    }
                }
            }
        }
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.profile_close)) } },
            title = { Text(stringResource(R.string.profile_about)) },
            text = { Text(stringResource(R.string.profile_about_text, BuildConfig.VERSION_NAME)) },
        )
    }
}

@Composable
private fun SupportAndAbout(supportUrl: String?, openLink: (String) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (supportUrl != null) MenuRow(stringResource(R.string.profile_help)) { openLink(supportUrl) }
        MenuRow(stringResource(R.string.profile_about), onAbout)
    }
}

@Composable
private fun Header(user: User, onEditName: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(user.name ?: stringResource(R.string.profile_no_name), style = MaterialTheme.typography.titleMedium)
            Text(user.phone, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onEditName) { Text(stringResource(R.string.profile_edit_name)) }
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun NameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.profile_name_label)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.profile_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
    )
}
