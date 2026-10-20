package id.oemah.nusantara.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.IndonesianPhone
import id.oemah.nusantara.ui.components.userMessage
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    val loading: Boolean

    data class Phone(val error: String? = null, override val loading: Boolean = false) : LoginUiState

    data class Code(
        val phoneE164: String,
        val resendInSeconds: Int,
        val error: String? = null,
        override val loading: Boolean = false,
    ) : LoginUiState
}

sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Phone())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    private var inFlight = false
    private var countdown: Job? = null

    fun submitPhone(raw: String) {
        val phone = IndonesianPhone.normalize(raw)
        if (phone == null) {
            _state.value = LoginUiState.Phone(error = INVALID_PHONE)
            return
        }
        launchOnce {
            repository.requestOtp(phone).fold(
                onSuccess = {
                    _state.value = LoginUiState.Code(phoneE164 = phone, resendInSeconds = COOLDOWN_SECONDS)
                    startCountdown()
                },
                onFailure = { _state.value = LoginUiState.Phone(error = it.message(wrongCode = false)) },
            )
        }
    }

    fun submitCode(code: String) {
        val step = _state.value as? LoginUiState.Code ?: return
        launchOnce {
            repository.verifyOtp(step.phoneE164, code.trim()).fold(
                onSuccess = { _events.send(LoginEvent.LoggedIn) },
                onFailure = { error ->
                    _state.update { (it as? LoginUiState.Code)?.copy(error = error.message(wrongCode = true)) ?: it }
                },
            )
        }
    }

    fun resend() {
        val step = _state.value as? LoginUiState.Code ?: return
        if (step.resendInSeconds > 0) return
        launchOnce {
            val error = repository.requestOtp(step.phoneE164).exceptionOrNull()
            _state.value = step.copy(resendInSeconds = COOLDOWN_SECONDS, error = error?.message(wrongCode = false))
            startCountdown()
        }
    }

    private fun launchOnce(block: suspend () -> Unit) {
        if (inFlight) return
        inFlight = true
        setLoading(true)
        viewModelScope.launch {
            try {
                block()
            } finally {
                inFlight = false
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) = _state.update {
        when (it) {
            is LoginUiState.Phone -> it.copy(loading = loading)
            is LoginUiState.Code -> it.copy(loading = loading)
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (remaining in COOLDOWN_SECONDS - 1 downTo 0) {
                delay(1_000)
                _state.update { if (it is LoginUiState.Code) it.copy(resendInSeconds = remaining) else it }
            }
        }
    }

    private fun Throwable.message(wrongCode: Boolean): String = when {
        this is AppError.Rejected && code == "OTP_RATE_LIMITED" -> RATE_LIMITED
        this is AppError.Rejected && code == "OTP_INVALID" && wrongCode -> WRONG_CODE
        this is AppError -> userMessage()
        else -> AppError.Unknown(this).userMessage()
    }

    private companion object {
        const val COOLDOWN_SECONDS = 60
        const val INVALID_PHONE = "Nomor HP tidak valid"
        const val WRONG_CODE = "Kode salah. Coba lagi."
        const val RATE_LIMITED = "Terlalu banyak percobaan. Coba lagi nanti."
    }
}
