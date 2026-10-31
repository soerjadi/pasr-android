package id.oemah.nusantara.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.core.toUiState
import id.oemah.nusantara.feature.catalog.CatalogRepository
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

/** S2 "Data toko": step 1 of opening a store. */
@HiltViewModel
class StoreFormViewModel @Inject constructor(
    private val myStore: MyStoreRepository,
    private val catalog: CatalogRepository,
    sessions: SessionStore,
) : ViewModel() {
    /** Read-only: the number already proved by OTP at login. */
    val phone: String = sessions.current()?.user?.phone.orEmpty()

    private val _form = MutableStateFlow(StoreFormState(ownerName = sessions.current()?.user?.name.orEmpty()))
    val form: StateFlow<StoreFormState> = _form.asStateFlow()

    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<Category>>> = _categories.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _created = Channel<Unit>(Channel.BUFFERED)
    val created: Flow<Unit> = _created.receiveAsFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _categories.value = UiState.Loading
        viewModelScope.launch { _categories.value = catalog.categories().toUiState() }
    }

    fun edit(change: StoreFormState.() -> StoreFormState) = _form.update { it.change().clamped() }

    fun submit() {
        val request = _form.value.toRequest() ?: return
        if (_submitting.value) return
        _submitting.value = true
        _error.value = null
        viewModelScope.launch {
            myStore.createStore(request).fold(
                onSuccess = { _created.send(Unit) },
                onFailure = { _error.value = it.asAppError().userMessage() },
            )
            _submitting.value = false
        }
    }

    private fun StoreFormState.clamped() =
        copy(name = name.take(StoreFormState.MAX_NAME), ownerName = ownerName.take(StoreFormState.MAX_NAME), address = address.take(StoreFormState.MAX_ADDRESS))
}
