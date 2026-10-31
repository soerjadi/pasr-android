package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.core.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** S4 seller home. Refreshes on entry so the order count and verification status are current. */
@HiltViewModel
class SellerHomeViewModel @Inject constructor(private val myStore: MyStoreRepository, sessions: SessionStore) : ViewModel() {
    val store: StateFlow<StoreSummary?> = myStore.store.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ownerName: String? = sessions.current()?.user?.name

    init {
        viewModelScope.launch { myStore.refresh() }
    }
}
