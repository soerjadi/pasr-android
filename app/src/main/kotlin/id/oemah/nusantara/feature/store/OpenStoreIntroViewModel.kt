package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.core.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** S1: a guest is sent to login first, a signed-in user straight to the store form. */
@HiltViewModel
class OpenStoreIntroViewModel @Inject constructor(sessions: SessionStore) : ViewModel() {
    val loggedIn: StateFlow<Boolean> = sessions.session.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, sessions.current() != null)
}
