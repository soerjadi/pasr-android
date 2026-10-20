package id.oemah.nusantara.feature.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.feature.cart.CartRepository
import id.oemah.nusantara.feature.cart.addProduct
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class ProductListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val cart: CartRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Route.ProductList>()

    val title: String = route.title

    val products: Flow<PagingData<ProductSummary>> by lazy {
        catalog.products(ProductQuery(categoryId = route.categoryId, featured = route.featured)).cachedIn(viewModelScope)
    }

    fun addToCart(product: ProductSummary) {
        viewModelScope.launch { cart.addProduct(product) }
    }
}
