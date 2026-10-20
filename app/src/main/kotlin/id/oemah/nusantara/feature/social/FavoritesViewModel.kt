package id.oemah.nusantara.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.addProduct
import id.oemah.nusantara.ui.components.userMessage
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val social: SocialRepository,
    private val cart: CartRepository,
    sessions: SessionStore,
) : ViewModel() {
    val loggedIn: StateFlow<Boolean> = sessions.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.current() != null)

    private val reloads = MutableStateFlow(0)

    /** Products hidden because the user just removed them; the list reloads once the server confirms. */
    private val _removedIds = MutableStateFlow<Set<Long>>(emptySet())
    val removedIds: StateFlow<Set<Long>> = _removedIds.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val favorites: Flow<PagingData<ProductSummary>> = combine(loggedIn, reloads) { isLoggedIn, reload -> isLoggedIn to reload }
        .flatMapLatest { (isLoggedIn, _) -> if (isLoggedIn) social.favorites() else flowOf(PagingData.empty()) }
        .cachedIn(viewModelScope)

    fun remove(productId: Long) {
        _removedIds.update { it + productId }
        viewModelScope.launch {
            social.setFavorite(productId, false)
                .onSuccess { reloads.update { it + 1 } }
                .onFailure { error ->
                    _removedIds.update { it - productId }
                    _events.send(UiEvent.Message(error.asAppError().userMessage()))
                }
        }
    }

    fun addToCart(product: ProductSummary) {
        viewModelScope.launch { cart.addProduct(product) }
    }
}
