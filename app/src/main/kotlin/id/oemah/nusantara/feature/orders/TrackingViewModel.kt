package id.oemah.nusantara.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.feature.ads.adsOrEmpty
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrdersRepository,
    private val ads: AdsRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.toRoute<Route.OrderTracking>().orderId

    private val _state = MutableStateFlow<UiState<Order>>(UiState.Loading)
    val state: StateFlow<UiState<Order>> = _state.asStateFlow()

    /** The "wait state" ad: only while the order is still open, since a finished order is no longer being waited on. */
    private val _waitAds = MutableStateFlow<List<Ad>>(emptyList())
    val waitAds: StateFlow<List<Ad>> = _waitAds.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result = orders.order(orderId)
            _state.value = result.toUiState()
            result.onSuccess { loadAds(it) }
        }
    }

    /** Pull-to-refresh: keeps the current order on screen and only replaces it when the server answers. */
    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            orders.order(orderId)
                .onSuccess {
                    _state.value = UiState.Content(it)
                    loadAds(it)
                }
                .onFailure { _events.send(UiEvent.Message(it.asAppError().userMessage())) }
            _refreshing.value = false
        }
    }

    private suspend fun loadAds(order: Order) {
        _waitAds.value = if (order.status.isFinished()) emptyList() else ads.adsOrEmpty(AdPlacement.WAIT_STATE)
    }
}
