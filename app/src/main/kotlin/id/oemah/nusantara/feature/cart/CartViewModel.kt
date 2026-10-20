package id.oemah.nusantara.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.api.model.LineIssue
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.Rupiah
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.checkout.CheckoutRepository
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CartEvent {
    data object LoginRequired : CartEvent
    data object OpenCheckout : CartEvent
}

data class CartLineUi(val line: CartLine, val issue: LineIssue?, val availableStock: Int?)

data class CartGroupUi(
    val sellerId: Long,
    val sellerName: String,
    val pickupAddress: String?,
    val openingHours: String?,
    val lines: List<CartLineUi>,
)

data class CartUiState(
    val groups: List<CartGroupUi> = emptyList(),
    /** Display only: the server recomputes prices when the order is placed. */
    val total: Rupiah = Rupiah(0),
    val notice: String? = null,
    val canCheckout: Boolean = false,
    val validating: Boolean = false,
    val error: AppError? = null,
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
    private val checkout: CheckoutRepository,
    private val sessions: SessionStore,
) : ViewModel() {
    private val validation = MutableStateFlow<Result<CartValidation>?>(null)
    private val validating = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events: Flow<CartEvent> = _events.receiveAsFlow()

    val state: StateFlow<CartUiState> =
        combine(cart.lines, validation, validating, notice, ::buildState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, CartUiState())

    init {
        var first = true
        viewModelScope.launch {
            // Validate straight away once, then wait for edits to settle so quick +/- taps cost one request.
            cart.lines
                .debounce { if (first) 0L.also { first = false } else DEBOUNCE_MS }
                .collectLatest { validate(it) }
        }
    }

    fun setQuantity(lineId: Long, quantity: Int) {
        viewModelScope.launch { cart.setQuantity(lineId, quantity) }
    }

    fun remove(lineId: Long) {
        viewModelScope.launch { cart.remove(lineId) }
    }

    fun adjustToAvailable(lineId: Long) {
        val available = state.value.groups.flatMap { it.lines }.firstOrNull { it.line.id == lineId }?.availableStock ?: return
        viewModelScope.launch { cart.setQuantity(lineId, available) }
    }

    fun dismissNotice() {
        notice.value = null
    }

    fun checkout() {
        if (!state.value.canCheckout) return
        _events.trySend(if (sessions.current() == null) CartEvent.LoginRequired else CartEvent.OpenCheckout)
    }

    private suspend fun validate(lines: List<CartLine>) {
        if (lines.isEmpty()) {
            validation.value = null
            return
        }
        validating.value = true
        try {
            val result = checkout.validate(lines)
            result.onSuccess { adoptServerPrices(lines, it) }
            validation.value = result
        } finally {
            validating.value = false
        }
    }

    /** The server is the source of truth for names and prices; keep the local snapshot in step and say so. */
    private suspend fun adoptServerPrices(lines: List<CartLine>, validated: CartValidation) {
        val local = lines.associateBy { it.productId }
        validated.groups.flatMap { it.lines }.forEach { server ->
            val mine = local[server.productId] ?: return@forEach
            if (server.unitPrice != mine.unitPrice.amount || server.name != mine.name) {
                cart.updateSnapshot(server.productId, server.name, Rupiah(server.unitPrice))
                if (server.unitPrice != mine.unitPrice.amount) notice.value = PRICE_UPDATED
            }
        }
    }

    private fun buildState(
        lines: List<CartLine>,
        result: Result<CartValidation>?,
        busy: Boolean,
        notice: String?,
    ): CartUiState {
        val validated = result?.getOrNull()
        val sellers = validated?.groups?.associate { it.seller.id to it.seller }.orEmpty()
        val serverLines = validated?.groups?.flatMap { it.lines }?.associateBy { it.productId }.orEmpty()

        val groups = lines.groupBy { it.sellerId }.map { (sellerId, sellerLines) ->
            CartGroupUi(
                sellerId = sellerId,
                sellerName = sellerLines.first().sellerName,
                pickupAddress = sellers[sellerId]?.pickupAddress,
                openingHours = sellers[sellerId]?.openingHours,
                lines = sellerLines.map { CartLineUi(it, serverLines[it.productId]?.issue, serverLines[it.productId]?.availableStock) },
            )
        }
        // A validated line only counts while its quantity still matches the cart.
        val total = lines.fold(Rupiah(0)) { sum, line ->
            val validatedTotal = serverLines[line.productId]?.takeIf { it.quantity == line.quantity }?.lineTotal
            sum + (validatedTotal?.let(::Rupiah) ?: (line.unitPrice * line.quantity))
        }
        val validationIsCurrent = lines.isNotEmpty() && lines.all { serverLines[it.productId]?.quantity == it.quantity }
        return CartUiState(
            groups = groups,
            total = total,
            notice = notice,
            canCheckout = validationIsCurrent && !busy && groups.all { g -> g.lines.all { it.issue == null } },
            validating = busy,
            error = result?.exceptionOrNull()?.asAppError(),
        )
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
        const val PRICE_UPDATED = "Harga diperbarui"
    }
}
