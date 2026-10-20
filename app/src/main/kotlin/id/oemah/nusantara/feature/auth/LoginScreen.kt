package id.oemah.nusantara.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.PrimaryButton

@Composable
fun LoginRoute(onLoggedIn: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == LoginEvent.LoggedIn) onLoggedIn() }
    }
    LoginScreen(
        state = state,
        onSubmitPhone = viewModel::submitPhone,
        onSubmitCode = viewModel::submitCode,
        onResend = viewModel::resend,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onSubmitPhone: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    onResend: () -> Unit,
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall)

        when (state) {
            is LoginUiState.Phone -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.login_phone_label)) },
                    placeholder = { Text(stringResource(R.string.login_phone_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = stringResource(R.string.login_send_code),
                    onClick = { onSubmitPhone(phone) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is LoginUiState.Code -> {
                Text(stringResource(R.string.login_code_sent, state.phoneE164))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.login_code_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = stringResource(R.string.login_submit),
                    onClick = { onSubmitCode(code) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onResend, enabled = state.resendInSeconds == 0 && !state.loading) {
                    Text(
                        if (state.resendInSeconds == 0) {
                            stringResource(R.string.login_resend)
                        } else {
                            stringResource(R.string.login_resend_in, state.resendInSeconds)
                        },
                    )
                }
            }
        }
    }
}
