package id.oemah.nusantara.feature.products.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.ProductDetail
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.toCartSnapshot
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.social.SocialRepository
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
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val social: SocialRepository,
    private val cart: CartRepository,
    private val sessions: SessionStore,
) : ViewModel() {
    private val productId = savedStateHandle.toRoute<Route.ProductDetail>().productId

    private val _state = MutableStateFlow<UiState<ProductDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ProductDetail>> = _state.asStateFlow()

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    private val _favorite = MutableStateFlow(false)
    val favorite: StateFlow<Boolean> = _favorite.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result = catalog.product(productId)
            result.onSuccess {
                _favorite.value = it.isFavorite == true
                _quantity.value = 1
            }
            _state.value = result.toUiState()
        }
    }

    fun setQuantity(quantity: Int) {
        _quantity.value = quantity.coerceIn(1, maxQuantity())
    }

    fun addToCart() {
        val product = loaded()?.product ?: return
        if (product.stock <= 0) {
            _events.trySend(UiEvent.Message(SOLD_OUT))
            return
        }
        viewModelScope.launch {
            cart.add(product.toCartSnapshot(), _quantity.value)
            _events.send(UiEvent.Message(ADDED))
        }
    }

    fun toggleFavorite() {
        if (sessions.current() == null) {
            _events.trySend(UiEvent.LoginRequired)
            return
        }
        val target = !_favorite.value
        _favorite.value = target // optimistic; rolled back below if the server refuses
        viewModelScope.launch {
            social.setFavorite(productId, target).onFailure { error ->
                _favorite.value = !target
                _events.send(UiEvent.Message(error.asAppError().userMessage()))
            }
        }
    }

    private fun loaded(): ProductDetail? = (_state.value as? UiState.Content)?.value

    private fun maxQuantity(): Int = (loaded()?.product?.stock ?: 1).coerceIn(1, MAX_QUANTITY)

    private companion object {
        const val MAX_QUANTITY = 99
        const val SOLD_OUT = "Stok habis"
        const val ADDED = "Ditambahkan ke keranjang"
    }
}
