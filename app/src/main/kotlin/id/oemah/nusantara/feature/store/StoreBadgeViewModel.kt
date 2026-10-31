package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** New orders waiting for the seller, for the badge on the Akun tab. */
@HiltViewModel
class StoreBadgeViewModel @Inject constructor(myStore: MyStoreRepository) : ViewModel() {
    val newOrders: StateFlow<Int> = myStore.store.map { it?.newOrderCount ?: 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
