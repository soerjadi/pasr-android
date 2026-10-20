package id.oemah.nusantara.core.network

import id.oemah.nusantara.core.AppError
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.decodeFromString
import retrofit2.Response
import id.oemah.nusantara.api.model.Error as ApiError

/** Runs one generated-client call and turns every outcome into a `Result` carrying an [AppError] on failure. */
suspend fun <T> apiCall(block: suspend () -> Response<T>): Result<T> = try {
    val response = block()
    when {
        response.isSuccessful -> response.body()?.let { Result.success(it) } ?: noContent(response)
        else -> Result.failure(response.toAppError())
    }
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Result.failure(AppError.Offline)
} catch (e: Exception) {
    Result.failure(AppError.Unknown(e))
}

@Suppress("UNCHECKED_CAST")
private fun <T> noContent(response: Response<T>): Result<T> =
    if (response.code() == 204 || response.code() == 205) {
        Result.success(Unit as T) // only `Response<Unit>` operations return no content
    } else {
        Result.failure(AppError.Unknown(IllegalStateException("Empty body for HTTP ${response.code()}")))
    }

private fun Response<*>.toAppError(): AppError {
    val parsed = runCatching { errorBody()?.string() }.getOrNull()
        ?.let { runCatching { AppJson.decodeFromString<ApiError>(it) }.getOrNull() }
    return when (val status = code()) {
        401 -> AppError.Unauthorized
        409 -> AppError.Conflict(parsed?.code ?: "CONFLICT", parsed?.message)
        in 400..499 -> AppError.Rejected(parsed?.code ?: "HTTP_$status", parsed?.message)
        else -> AppError.Server
    }
}
