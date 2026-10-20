package id.oemah.nusantara.feature.orders.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.orders.OrderTab
import id.oemah.nusantara.feature.orders.OrdersRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val repository: OrdersRepository,
    sessions: SessionStore,
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(OrderTab.ALL)
    val selectedTab: StateFlow<OrderTab> = _selectedTab.asStateFlow()

    val loggedIn: StateFlow<Boolean> = sessions.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.current() != null)

    /** The selected tab's orders; nothing is requested while logged out. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: Flow<PagingData<Order>> = combine(_selectedTab, loggedIn) { tab, isLoggedIn -> tab to isLoggedIn }
        .distinctUntilChanged()
        .flatMapLatest { (tab, isLoggedIn) ->
            if (isLoggedIn) repository.orders(tab) else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    fun select(tab: OrderTab) {
        _selectedTab.value = tab
    }
}
