package id.oemah.nusantara.feature.orders.seller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreOrderPageCounts
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerOrdersState(
    val selectedTab: MeApi.TabListMyStoreOrders = MeApi.TabListMyStoreOrders.NEW,
    val counts: StoreOrderPageCounts? = null,
    val activeRejectOrder: StoreOrder? = null,
    val activeReadyOrder: StoreOrder? = null,
    val actionInProgressId: Long? = null,
    val actionError: String? = null,
)

@HiltViewModel
class SellerOrdersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sellerRepo: SellerRepository,
) : ViewModel() {

    private val initialTab: MeApi.TabListMyStoreOrders = runCatching {
        val tabStr = runCatching { savedStateHandle.toRoute<Route.SellerOrders>().initialTab }
            .getOrNull() ?: savedStateHandle.get<String>("initialTab")
        when (tabStr?.uppercase()) {
            "PROCESSING" -> MeApi.TabListMyStoreOrders.PROCESSING
            "DONE" -> MeApi.TabListMyStoreOrders.DONE
            else -> MeApi.TabListMyStoreOrders.NEW
        }
    }.getOrDefault(MeApi.TabListMyStoreOrders.NEW)

    private val _state = MutableStateFlow(SellerOrdersState(selectedTab = initialTab))
    val state: StateFlow<SellerOrdersState> = _state.asStateFlow()

    private val reloads = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: Flow<PagingData<StoreOrder>> = combine(
        _state.map { it.selectedTab }.distinctUntilChanged(),
        reloads,
    ) { tab, _ -> tab }
        .flatMapLatest { tab -> sellerRepo.myStoreOrders(tab) }
        .cachedIn(viewModelScope)

    init {
        refreshCounts()
    }

    fun selectTab(tab: MeApi.TabListMyStoreOrders) {
        if (_state.value.selectedTab != tab) {
            _state.update { it.copy(selectedTab = tab) }
            refreshCounts()
        }
    }

    fun refresh() {
        reloads.update { it + 1 }
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            sellerRepo.listMyStoreOrders(tab = _state.value.selectedTab, limit = 1)
                .onSuccess { page ->
                    _state.update { it.copy(counts = page.counts) }
                }
        }
    }

    fun acceptOrder(orderId: Long) {
        _state.update { it.copy(actionInProgressId = orderId, actionError = null) }
        viewModelScope.launch {
            sellerRepo.acceptMyStoreOrder(orderId)
                .onSuccess {
                    _state.update { it.copy(actionInProgressId = null) }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            actionInProgressId = null,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun rejectOrder(orderId: Long, reason: RejectRequest.Reason, note: String?) {
        _state.update { it.copy(actionInProgressId = orderId, actionError = null) }
        viewModelScope.launch {
            sellerRepo.rejectMyStoreOrder(orderId, reason, note)
                .onSuccess {
                    _state.update { it.copy(actionInProgressId = null, activeRejectOrder = null) }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            actionInProgressId = null,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun readyOrder(orderId: Long, note: String?) {
        _state.update { it.copy(actionInProgressId = orderId, actionError = null) }
        viewModelScope.launch {
            sellerRepo.readyMyStoreOrder(orderId, note)
                .onSuccess {
                    _state.update { it.copy(actionInProgressId = null, activeReadyOrder = null) }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            actionInProgressId = null,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun completeOrder(orderId: Long) {
        _state.update { it.copy(actionInProgressId = orderId, actionError = null) }
        viewModelScope.launch {
            sellerRepo.completeMyStoreOrder(orderId)
                .onSuccess {
                    _state.update { it.copy(actionInProgressId = null) }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            actionInProgressId = null,
                            actionError = error.asAppError().userMessage(),
                        )
                    }
                }
        }
    }

    fun openRejectDialog(order: StoreOrder) {
        _state.update { it.copy(activeRejectOrder = order) }
    }

    fun closeRejectDialog() {
        _state.update { it.copy(activeRejectOrder = null) }
    }

    fun openReadyDialog(order: StoreOrder) {
        _state.update { it.copy(activeReadyOrder = order) }
    }

    fun closeReadyDialog() {
        _state.update { it.copy(activeReadyOrder = null) }
    }

    fun dismissError() {
        _state.update { it.copy(actionError = null) }
    }
}
