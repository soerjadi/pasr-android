package id.oemah.nusantara.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Number of lines in the cart, for the badge on the cart button. */
@HiltViewModel
class CartBadgeViewModel @Inject constructor(cart: CartRepository) : ViewModel() {
    val count: StateFlow<Int> = cart.lines.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
