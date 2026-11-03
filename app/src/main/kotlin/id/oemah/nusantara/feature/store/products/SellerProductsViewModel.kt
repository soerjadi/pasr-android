package id.oemah.nusantara.feature.store.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.SellerProductPageCounts
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.ui.components.userMessage
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

data class SellerProductsState(
    val selectedStatus: MeApi.StatusListMyProducts? = null,
    val counts: SellerProductPageCounts? = null,
    val productToDelete: SellerProduct? = null,
    val actionInProgressId: Long? = null,
    val actionError: String? = null,
)

@HiltViewModel
class SellerProductsViewModel @Inject constructor(
    private val sellerRepo: SellerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SellerProductsState())
    val state: StateFlow<SellerProductsState> = _state.asStateFlow()

    private val reloads = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: Flow<PagingData<SellerProduct>> = combine(
        _state.map { it.selectedStatus }.distinctUntilChanged(),
        reloads,
    ) { status, _ -> status }
        .flatMapLatest { status -> sellerRepo.myProducts(status) }
        .cachedIn(viewModelScope)

    init {
        refreshCounts()
    }

    fun selectStatus(status: MeApi.StatusListMyProducts?) {
        if (_state.value.selectedStatus != status) {
            _state.update { it.copy(selectedStatus = status) }
            refreshCounts()
        }
    }

    fun refresh() {
        reloads.update { it + 1 }
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            sellerRepo.listMyProducts(status = _state.value.selectedStatus, limit = 1)
                .onSuccess { page ->
                    _state.update { it.copy(counts = page.counts) }
                }
        }
    }

    fun updateProductStatus(productId: Long, status: UpdateProductRequest.Status) {
        _state.update { it.copy(actionInProgressId = productId, actionError = null) }
        viewModelScope.launch {
            sellerRepo.updateMyProduct(productId, UpdateProductRequest(status = status))
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

    fun openDeleteDialog(product: SellerProduct) {
        _state.update { it.copy(productToDelete = product) }
    }

    fun closeDeleteDialog() {
        _state.update { it.copy(productToDelete = null) }
    }

    fun deleteProduct() {
        val target = _state.value.productToDelete ?: return
        _state.update { it.copy(actionInProgressId = target.id, actionError = null) }
        viewModelScope.launch {
            sellerRepo.deleteMyProduct(target.id)
                .onSuccess {
                    _state.update { it.copy(actionInProgressId = null, productToDelete = null) }
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

    fun dismissError() {
        _state.update { it.copy(actionError = null) }
    }
}
