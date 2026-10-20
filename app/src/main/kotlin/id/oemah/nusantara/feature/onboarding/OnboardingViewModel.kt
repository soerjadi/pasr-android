package id.oemah.nusantara.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val store: OnboardingStore) : ViewModel() {
    /** Null until the store has answered (the splash screen covers that moment). */
    val seen: StateFlow<Boolean?> = store.seen.map<Boolean, Boolean?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markSeen() {
        viewModelScope.launch { store.markSeen() }
    }
}
