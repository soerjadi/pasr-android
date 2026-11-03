package id.oemah.nusantara.feature.orders.seller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerOrderDetailState(
    val orderState: UiState<StoreOrder> = UiState.Loading,
    val isActionInProgress: Boolean = false,
    val showRejectDialog: Boolean = false,
    val showReadyDialog: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class SellerOrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sellerRepo: SellerRepository,
) : ViewModel() {

    val orderId: Long = runCatching {
        savedStateHandle.toRoute<Route.SellerOrderDetail>().orderId
    }.getOrNull() ?: savedStateHandle.get<Long>("orderId") ?: 0L

    private val _state = MutableStateFlow(SellerOrderDetailState())
    val state: StateFlow<SellerOrderDetailState> = _state.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        _state.update { it.copy(orderState = UiState.Loading) }
        viewModelScope.launch {
            _state.update { it.copy(orderState = sellerRepo.getMyStoreOrder(orderId).toUiState()) }
        }
    }

    fun acceptOrder() {
        _state.update { it.copy(isActionInProgress = true, actionError = null) }
        viewModelScope.launch {
            sellerRepo.acceptMyStoreOrder(orderId)
                .onSuccess { updatedOrder ->
                    _state.update {
                        it.copy(
                            orderState = UiState.Content(updatedOrder),
                            isActionInProgress = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun rejectOrder(reason: RejectRequest.Reason, note: String?) {
        _state.update { it.copy(isActionInProgress = true, actionError = null) }
        viewModelScope.launch {
            sellerRepo.rejectMyStoreOrder(orderId, reason, note)
                .onSuccess { updatedOrder ->
                    _state.update {
                        it.copy(
                            orderState = UiState.Content(updatedOrder),
                            isActionInProgress = false,
                            showRejectDialog = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun readyOrder(note: String?) {
        _state.update { it.copy(isActionInProgress = true, actionError = null) }
        viewModelScope.launch {
            sellerRepo.readyMyStoreOrder(orderId, note)
                .onSuccess { updatedOrder ->
                    _state.update {
                        it.copy(
                            orderState = UiState.Content(updatedOrder),
                            isActionInProgress = false,
                            showReadyDialog = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun completeOrder() {
        _state.update { it.copy(isActionInProgress = true, actionError = null) }
        viewModelScope.launch {
            sellerRepo.completeMyStoreOrder(orderId)
                .onSuccess { updatedOrder ->
                    _state.update {
                        it.copy(
                            orderState = UiState.Content(updatedOrder),
                            isActionInProgress = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun openRejectDialog() {
        _state.update { it.copy(showRejectDialog = true) }
    }

    fun closeRejectDialog() {
        _state.update { it.copy(showRejectDialog = false) }
    }

    fun openReadyDialog() {
        _state.update { it.copy(showReadyDialog = true) }
    }

    fun closeReadyDialog() {
        _state.update { it.copy(showReadyDialog = false) }
    }

    fun dismissError() {
        _state.update { it.copy(actionError = null) }
    }
}
