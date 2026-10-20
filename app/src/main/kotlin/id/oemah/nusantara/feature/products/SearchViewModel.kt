package id.oemah.nusantara.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.addProduct
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val cart: CartRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    /** The text exactly as typed, for the text field. */
    val queryText: StateFlow<String> = query.asStateFlow()

    /** True while there is nothing to search for (blank input). */
    val isIdle: StateFlow<Boolean> = query.map { it.isBlank() }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<ProductSummary>> = query
        .debounce(DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { text ->
            if (text.isEmpty()) flowOf(PagingData.empty()) else catalog.products(ProductQuery(query = text))
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun addToCart(product: ProductSummary) {
        viewModelScope.launch { cart.addProduct(product) }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
