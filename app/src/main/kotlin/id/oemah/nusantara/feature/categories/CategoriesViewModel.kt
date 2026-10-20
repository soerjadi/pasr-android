package id.oemah.nusantara.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.catalog.CatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Category>>> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = catalog.categories().toUiState() }
    }
}

/** Material's core icon set has no food or craft icons, so categories use emoji keyed by the server's `iconKey`. */
fun Category.emoji(): String = when (iconKey) {
    "food" -> "🍜"
    "fashion" -> "👗"
    "craft" -> "🎨"
    "farm" -> "🌾"
    "beauty" -> "💄"
    "electronics" -> "📱"
    "home" -> "🏠"
    "auto" -> "🚗"
    else -> "🛍️"
}
