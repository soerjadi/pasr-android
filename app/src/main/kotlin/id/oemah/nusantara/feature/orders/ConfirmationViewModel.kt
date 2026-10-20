package id.oemah.nusantara.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.feature.ads.adsOrEmpty
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrdersRepository,
    private val ads: AdsRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.toRoute<Route.OrderConfirmation>().orderId

    private val _state = MutableStateFlow<UiState<Order>>(UiState.Loading)
    val state: StateFlow<UiState<Order>> = _state.asStateFlow()

    private val _postPurchaseAds = MutableStateFlow<List<Ad>>(emptyList())
    val postPurchaseAds: StateFlow<List<Ad>> = _postPurchaseAds.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = orders.order(orderId).toUiState() }
        viewModelScope.launch { _postPurchaseAds.value = ads.adsOrEmpty(AdPlacement.POST_PURCHASE) }
    }
}
