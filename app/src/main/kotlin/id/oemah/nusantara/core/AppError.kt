package id.oemah.nusantara.core

sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    data object Offline : AppError("offline")
    data object Unauthorized : AppError("unauthorized")
    data object Server : AppError("server")
    data class Conflict(val code: String, override val message: String?) : AppError()
    data class Rejected(val code: String, override val message: String?) : AppError()
    class Unknown(cause: Throwable?) : AppError(cause?.message, cause)
}

fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
