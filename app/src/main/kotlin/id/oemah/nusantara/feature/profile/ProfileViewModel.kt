package id.oemah.nusantara.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.IndonesianPhone
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.auth.AuthRepository
import id.oemah.nusantara.ui.components.userMessage
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profile: ProfileRepository,
    private val auth: AuthRepository,
    sessions: SessionStore,
    @Named("supportWhatsapp") supportWhatsapp: String,
) : ViewModel() {
    val loggedIn: StateFlow<Boolean> = sessions.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.current() != null)

    private val _user = MutableStateFlow<UiState<User>>(UiState.Loading)
    val user: StateFlow<UiState<User>> = _user.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    /** The operator's WhatsApp for "Bantuan"; null (row hidden) when none is configured or it is not a valid number. */
    val supportUrl: String? = IndonesianPhone.toWhatsAppUrl(supportWhatsapp)

    init {
        viewModelScope.launch {
            loggedIn.collect { isLoggedIn -> if (isLoggedIn) reload() else _user.value = UiState.Loading }
        }
    }

    fun reload() {
        if (!loggedIn.value) return
        _user.value = UiState.Loading
        viewModelScope.launch { _user.value = profile.me().toUiState() }
    }

    fun updateName(raw: String) {
        val name = raw.trim()
        if (name.length !in 1..MAX_NAME) {
            _events.trySend(UiEvent.Message(INVALID_NAME))
            return
        }
        viewModelScope.launch {
            profile.updateName(name).fold(
                onSuccess = { _user.value = UiState.Content(it) },
                onFailure = { _events.send(UiEvent.Message(it.asAppError().userMessage())) },
            )
        }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }

    private companion object {
        const val MAX_NAME = 50
        const val INVALID_NAME = "Nama harus 1-50 karakter."
    }
}
