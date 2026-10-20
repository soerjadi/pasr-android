package id.oemah.nusantara.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.cart.CartRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface Submission {
    data object Idle : Submission
    data object Submitting : Submission
    data class Failed(val error: AppError) : Submission
    data class Placed(val order: Order) : Submission
}

sealed interface CheckoutEvent {
    data object LoginRequired : CheckoutEvent
    data class BackToCart(val message: String) : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cart: CartRepository,
    private val checkout: CheckoutRepository,
    private val sessions: SessionStore,
) : ViewModel() {
    /** The server's current view of the cart (prices, stock, each seller's pickup details). */
    private val _review = MutableStateFlow<UiState<CartValidation>>(UiState.Loading)
    val review: StateFlow<UiState<CartValidation>> = _review.asStateFlow()

    private val _submission = MutableStateFlow<Submission>(Submission.Idle)
    val submission: StateFlow<Submission> = _submission.asStateFlow()

    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events: Flow<CheckoutEvent> = _events.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        _review.value = UiState.Loading
        viewModelScope.launch {
            val lines = cart.lines.first()
            _review.value =
                if (lines.isEmpty()) UiState.Content(CartValidation(emptyList(), 0)) else checkout.validate(lines).toUiState()
        }
    }

    fun placeOrder() {
        // Claim the submission synchronously so a double tap cannot start two requests.
        val current = _submission.value
        if (current is Submission.Submitting || current is Submission.Placed) return
        if (sessions.current() == null) {
            _events.trySend(CheckoutEvent.LoginRequired)
            return
        }
        _submission.value = Submission.Submitting
        viewModelScope.launch {
            val lines = cart.lines.first()
            if (lines.isEmpty()) {
                _submission.value = Submission.Idle
                return@launch
            }
            checkout.placeOrder(lines).fold(
                onSuccess = { order ->
                    cart.removeAll(lines.map { it.id })
                    _submission.value = Submission.Placed(order)
                },
                onFailure = { error ->
                    if (error is AppError.Conflict && error.code in STOCK_CODES) {
                        _submission.value = Submission.Idle
                        _events.send(CheckoutEvent.BackToCart(STOCK_CHANGED))
                    } else {
                        _submission.value = Submission.Failed(error.asAppError())
                    }
                },
            )
        }
    }

    private companion object {
        val STOCK_CODES = setOf("INSUFFICIENT_STOCK", "PRODUCT_UNAVAILABLE")
        const val STOCK_CHANGED = "Stok berubah. Periksa keranjang Anda."
    }
}
