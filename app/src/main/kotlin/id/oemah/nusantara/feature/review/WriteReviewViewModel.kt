package id.oemah.nusantara.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.asAppError
import id.oemah.nusantara.feature.social.SocialRepository
import id.oemah.nusantara.ui.components.userMessage
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ReviewEvent {
    /** The screen should close and show [message]. */
    data class Done(val message: String) : ReviewEvent
}

@HiltViewModel
class WriteReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val social: SocialRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Route.WriteReview>()
    val productName: String = route.productName

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _canSubmit = MutableStateFlow(false)
    val canSubmit: StateFlow<Boolean> = _canSubmit.asStateFlow()

    private val _events = Channel<ReviewEvent>(Channel.BUFFERED)
    val events: Flow<ReviewEvent> = _events.receiveAsFlow()

    private var submitting = false

    fun setRating(stars: Int) {
        if (stars !in 1..MAX_STARS) return
        _rating.value = stars
        _error.value = null
        updateCanSubmit()
    }

    fun setText(value: String) {
        _text.value = value.take(MAX_TEXT)
    }

    fun submit() {
        if (!_canSubmit.value) return
        submitting = true
        updateCanSubmit()
        viewModelScope.launch {
            social.submitReview(route.orderItemId, _rating.value, _text.value.trim().ifBlank { null }).fold(
                onSuccess = { _events.send(ReviewEvent.Done(THANKS)) },
                onFailure = { failure ->
                    if (failure is AppError.Conflict) {
                        _events.send(ReviewEvent.Done(ALREADY_REVIEWED))
                    } else {
                        _error.value = failure.asAppError().userMessage()
                    }
                },
            )
            submitting = false
            updateCanSubmit()
        }
    }

    private fun updateCanSubmit() {
        _canSubmit.value = _rating.value in 1..MAX_STARS && !submitting
    }

    private companion object {
        const val MAX_STARS = 5
        const val MAX_TEXT = 500
        const val THANKS = "Terima kasih atas ulasan Anda."
        const val ALREADY_REVIEWED = "Ulasan sudah dikirim."
    }
}
