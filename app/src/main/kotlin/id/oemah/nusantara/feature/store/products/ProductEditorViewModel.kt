package id.oemah.nusantara.feature.store.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.feature.store.SellerRepository
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductEditorState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val price: String = "",
    val stock: String = "",
    val description: String = "",
    val photoId: Long? = null,
    val imageUrl: String? = null,
    val status: SellerProduct.Status = SellerProduct.Status.AVAILABLE,
    val isUploadingImage: Boolean = false,
    val isSubmitting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() &&
            (price.toLongOrNull()?.let { it in 1..MAX_PRICE } == true) &&
            (stock.toIntOrNull()?.let { it in 0..MAX_STOCK } == true)

    companion object {
        const val MAX_NAME = 80
        const val MAX_PRICE = 100_000_000L
        const val MAX_STOCK = 9_999
        const val PRICE_DIGITS = 9
        const val STOCK_DIGITS = 4
    }
}

@HiltViewModel
class ProductEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sellerRepo: SellerRepository,
) : ViewModel() {

    private val productId: Long? = runCatching {
        savedStateHandle.toRoute<Route.EditProduct>().productId
    }.getOrNull() ?: savedStateHandle.get<Long>("productId")?.takeIf { it > 0 }

    private val isEditMode: Boolean = productId != null && productId > 0L

    private val _state = MutableStateFlow(
        ProductEditorState(
            isEditMode = isEditMode,
            isLoading = isEditMode,
        )
    )
    val state: StateFlow<ProductEditorState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    init {
        if (isEditMode && productId != null) {
            loadProduct(productId)
        }
    }

    private fun loadProduct(id: Long) {
        viewModelScope.launch {
            sellerRepo.getMyProduct(id).fold(
                onSuccess = { product ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            name = product.name,
                            price = product.price.toString(),
                            stock = product.stock.toString(),
                            description = product.description,
                            status = product.status,
                            imageUrl = product.images.firstOrNull()?.medium
                                ?: product.images.firstOrNull()?.large
                                ?: product.images.firstOrNull()?.small,
                            photoId = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.asAppError().userMessage(),
                        )
                    }
                },
            )
        }
    }

    fun setName(value: String) {
        _state.update { it.copy(name = value.take(ProductEditorState.MAX_NAME)) }
    }

    fun setPrice(value: String) {
        _state.update { it.copy(price = value.filter(Char::isDigit).take(ProductEditorState.PRICE_DIGITS)) }
    }

    fun setStock(value: String) {
        _state.update { it.copy(stock = value.filter(Char::isDigit).take(ProductEditorState.STOCK_DIGITS)) }
    }

    fun stepStock(delta: Int) {
        _state.update {
            val current = it.stock.toIntOrNull() ?: 0
            val next = (current + delta).coerceIn(0, ProductEditorState.MAX_STOCK)
            it.copy(stock = next.toString())
        }
    }

    fun setDescription(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun setStatus(status: SellerProduct.Status) {
        _state.update { it.copy(status = status) }
    }

    fun uploadPhoto(
        bytes: ByteArray,
        filename: String = "product.jpg",
        mimeType: String = "image/jpeg",
    ) {
        if (_state.value.isUploadingImage) return
        _state.update { it.copy(isUploadingImage = true, error = null) }
        viewModelScope.launch {
            sellerRepo.uploadImage(bytes, filename, mimeType).fold(
                onSuccess = { upload ->
                    _state.update {
                        it.copy(
                            isUploadingImage = false,
                            photoId = upload.id,
                            imageUrl = upload.image.medium,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isUploadingImage = false,
                            error = error.asAppError().userMessage(),
                        )
                    }
                },
            )
        }
    }

    fun removePhoto() {
        _state.update { it.copy(photoId = null, imageUrl = null) }
    }

    fun openDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun closeDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun submit() {
        val s = _state.value
        if (!s.isFormValid || s.isSubmitting) return

        val trimmedName = s.name.trim()
        val priceVal = s.price.toLongOrNull() ?: return
        val stockVal = s.stock.toIntOrNull() ?: return
        val descVal = s.description.trim().takeIf { it.isNotBlank() }
        val photoIdsVal = s.photoId?.let { listOf(it) }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = if (isEditMode && productId != null) {
                val req = UpdateProductRequest(
                    name = trimmedName,
                    price = priceVal,
                    stock = stockVal,
                    description = descVal,
                    photoIds = photoIdsVal,
                    status = UpdateProductRequest.Status.valueOf(s.status.name),
                )
                sellerRepo.updateMyProduct(productId, req)
            } else {
                val req = CreateProductRequest(
                    name = trimmedName,
                    price = priceVal,
                    stock = stockVal,
                    description = descVal,
                    photoIds = photoIdsVal,
                )
                sellerRepo.createProduct(req)
            }

            result.fold(
                onSuccess = {
                    _saved.send(Unit)
                },
                onFailure = { error ->
                    _state.update { it.copy(error = error.asAppError().userMessage()) }
                },
            )
            _state.update { it.copy(isSubmitting = false) }
        }
    }

    fun delete() {
        if (!isEditMode || productId == null || _state.value.isSubmitting) return

        _state.update { it.copy(showDeleteDialog = false, isSubmitting = true, error = null) }
        viewModelScope.launch {
            sellerRepo.deleteMyProduct(productId).fold(
                onSuccess = {
                    _deleted.send(Unit)
                },
                onFailure = { error ->
                    _state.update { it.copy(error = error.asAppError().userMessage()) }
                },
            )
            _state.update { it.copy(isSubmitting = false) }
        }
    }
}
