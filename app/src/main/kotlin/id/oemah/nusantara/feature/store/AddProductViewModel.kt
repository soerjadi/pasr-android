package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.ui.components.userMessage
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S3 "Tambah produk". */
@HiltViewModel
class AddProductViewModel @Inject constructor(private val myStore: MyStoreRepository) : ViewModel() {
    private val _form = MutableStateFlow(ProductFormState())
    val form: StateFlow<ProductFormState> = _form.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    fun setName(value: String) = _form.update { it.copy(name = value.take(ProductFormState.MAX_NAME)) }

    /** Digits only, so "15.000" pasted from elsewhere still works. */
    fun setPrice(value: String) = _form.update { it.copy(price = value.filter(Char::isDigit).take(PRICE_DIGITS)) }

    fun setStock(value: String) = _form.update { it.copy(stock = value.filter(Char::isDigit).take(STOCK_DIGITS)) }

    fun stepStock(delta: Int) = _form.update {
        it.copy(stock = ((it.stock.toIntOrNull() ?: 0) + delta).coerceIn(0, ProductFormState.MAX_STOCK).toString())
    }

    fun submit() {
        val request = _form.value.toRequest() ?: return
        if (_submitting.value) return
        _submitting.value = true
        _error.value = null
        viewModelScope.launch {
            myStore.createProduct(request).fold(
                onSuccess = { _saved.send(Unit) },
                onFailure = { _error.value = it.asAppError().userMessage() },
            )
            _submitting.value = false
        }
    }

    private companion object {
        const val PRICE_DIGITS = 9
        const val STOCK_DIGITS = 4
    }
}
