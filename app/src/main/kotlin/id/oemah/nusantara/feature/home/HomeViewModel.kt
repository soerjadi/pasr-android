package id.oemah.nusantara.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.ads.AdPlacement
import id.oemah.nusantara.feature.ads.AdsRepository
import id.oemah.nusantara.feature.ads.adsOrEmpty
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.toCartSnapshot
import id.oemah.nusantara.feature.catalog.CatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Each section has its own state: one failing call must not blank the whole screen. */
data class HomeUiState(
    val categories: UiState<List<Category>> = UiState.Loading,
    val featured: UiState<List<ProductSummary>> = UiState.Loading,
    val partnerAds: List<Ad> = emptyList(),
    val sponsoredAds: List<Ad> = emptyList(),
) {
    val categoryShortcuts: List<Category>
        get() = (categories as? UiState.Content)?.value.orEmpty().take(SHORTCUT_COUNT)

    private companion object {
        const val SHORTCUT_COUNT = 4
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val ads: AdsRepository,
    private val cart: CartRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.update { it.copy(categories = UiState.Loading, featured = UiState.Loading) }
        viewModelScope.launch { catalog.categories().let { r -> _state.update { it.copy(categories = r.toUiState()) } } }
        viewModelScope.launch { catalog.featuredProducts().let { r -> _state.update { it.copy(featured = r.toUiState()) } } }
        viewModelScope.launch { ads.adsOrEmpty(AdPlacement.HOME_PARTNER_CAROUSEL).let { a -> _state.update { it.copy(partnerAds = a) } } }
        viewModelScope.launch { ads.adsOrEmpty(AdPlacement.HOME_SPONSORED_CARD).let { a -> _state.update { it.copy(sponsoredAds = a) } } }
    }

    fun addToCart(product: ProductSummary) {
        if (product.stock <= 0) return
        viewModelScope.launch { cart.add(product.toCartSnapshot()) }
    }
}
