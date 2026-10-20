package id.oemah.nusantara.feature.seller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.api.model.SellerDetail
import id.oemah.nusantara.core.IndonesianPhone
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.addProduct
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import id.oemah.nusantara.feature.social.SocialRepository
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
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
class SellerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val social: SocialRepository,
    private val cart: CartRepository,
    private val sessions: SessionStore,
) : ViewModel() {
    private val sellerId = savedStateHandle.toRoute<Route.Seller>().sellerId

    private val _state = MutableStateFlow<UiState<SellerDetail>>(UiState.Loading)
    val state: StateFlow<UiState<SellerDetail>> = _state.asStateFlow()

    private val _following = MutableStateFlow(false)
    val following: StateFlow<Boolean> = _following.asStateFlow()

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    /** Null when the seller's number is not a valid Indonesian mobile number: the chat button is then hidden. */
    val whatsappUrl: StateFlow<String?> = _state
        .map { (it as? UiState.Content)?.value?.seller?.whatsapp?.let(IndonesianPhone::toWhatsAppUrl) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val products: Flow<PagingData<ProductSummary>> by lazy {
        catalog.products(ProductQuery(sellerId = sellerId)).cachedIn(viewModelScope)
    }

    val reviews: Flow<PagingData<Review>> by lazy { catalog.sellerReviews(sellerId).cachedIn(viewModelScope) }

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result = catalog.seller(sellerId)
            result.onSuccess {
                _following.value = it.isFollowing == true
                _followerCount.value = it.followerCount
            }
            _state.value = result.toUiState()
        }
    }

    fun toggleFollow() {
        if (sessions.current() == null) {
            _events.trySend(UiEvent.LoginRequired)
            return
        }
        val target = !_following.value
        val delta = if (target) 1 else -1
        _following.value = target // optimistic; rolled back below if the server refuses
        _followerCount.value += delta
        viewModelScope.launch {
            social.setFollowing(sellerId, target).onFailure { error ->
                _following.value = !target
                _followerCount.value -= delta
                _events.send(UiEvent.Message(error.asAppError().userMessage()))
            }
        }
    }

    fun addToCart(product: ProductSummary) {
        viewModelScope.launch { cart.addProduct(product) }
    }
}
