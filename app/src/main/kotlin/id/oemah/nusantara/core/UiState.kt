package id.oemah.nusantara.core

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val value: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

sealed interface UiEvent {
    data object LoginRequired : UiEvent
    data class Message(val text: String) : UiEvent
}
