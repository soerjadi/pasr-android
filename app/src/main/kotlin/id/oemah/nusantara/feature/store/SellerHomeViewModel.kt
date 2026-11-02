package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val <T> UiState.Content<T>.data: T get() = value

data class SellerHomeState(
    val store: StoreSummary? = null,
    val summaryState: UiState<StoreSummaryResponse> = UiState.Loading,
    val statsState: UiState<StoreStats> = UiState.Loading,
    val selectedRange: MeApi.RangeGetMyStoreStats = MeApi.RangeGetMyStoreStats.TODAY,
    val isTogglingOpen: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class SellerHomeViewModel @Inject constructor(
    private val myStore: MyStoreRepository,
    private val sellerRepo: SellerRepository,
    sessions: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SellerHomeState())
    val state: StateFlow<SellerHomeState> = _state.asStateFlow()

    /** Kept for backward compatibility with SellerHomeScreen until Task 3. */
    val store: StateFlow<StoreSummary?> = myStore.store.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ownerName: String? = sessions.current()?.user?.name

    init {
        viewModelScope.launch {
            myStore.store.collect { store ->
                _state.update { it.copy(store = store) }
                if (store?.status == StoreStatus.VERIFIED) {
                    loadSummary()
                    loadStats(_state.value.selectedRange)
                }
            }
        }
        viewModelScope.launch { myStore.refresh() }
    }

    fun loadSummary() {
        viewModelScope.launch {
            _state.update { it.copy(summaryState = sellerRepo.getMyStoreSummary().toUiState()) }
        }
    }

    fun loadStats(range: MeApi.RangeGetMyStoreStats) {
        viewModelScope.launch {
            _state.update { it.copy(selectedRange = range, statsState = UiState.Loading) }
            _state.update { it.copy(statsState = sellerRepo.getMyStoreStats(range).toUiState()) }
        }
    }

    fun selectStatsRange(range: MeApi.RangeGetMyStoreStats) {
        if (_state.value.selectedRange == range) return
        loadStats(range)
    }

    fun toggleStoreOpen() {
        val currentSummary = (_state.value.summaryState as? UiState.Content)?.value ?: return
        if (_state.value.isTogglingOpen) return

        val targetOpen = !currentSummary.isOpen
        // Optimistic update
        _state.update {
            it.copy(
                isTogglingOpen = true,
                summaryState = UiState.Content(currentSummary.copy(isOpen = targetOpen)),
                actionError = null,
            )
        }

        viewModelScope.launch {
            sellerRepo.setMyStoreOpen(targetOpen).fold(
                onSuccess = {
                    _state.update { it.copy(isTogglingOpen = false) }
                },
                onFailure = {
                    // Revert to original
                    _state.update {
                        it.copy(
                            isTogglingOpen = false,
                            summaryState = UiState.Content(currentSummary),
                            actionError = "Gagal mengubah status toko. Silakan coba lagi.",
                        )
                    }
                },
            )
        }
    }

    fun dismissActionError() {
        _state.update { it.copy(actionError = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            myStore.refresh()
            if (_state.value.store?.status == StoreStatus.VERIFIED) {
                loadSummary()
                loadStats(_state.value.selectedRange)
            }
        }
    }
}
