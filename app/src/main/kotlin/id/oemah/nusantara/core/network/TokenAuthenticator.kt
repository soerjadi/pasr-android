package id.oemah.nusantara.core.network

import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.model.RefreshRequest
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On a 401, rotates the session once and retries. Concurrent 401s share one refresh: whoever
 * enters second sees the token already changed and just retries. [authApi] must be built from a
 * client without this authenticator.
 */
class TokenAuthenticator(
    private val sessions: SessionStore,
    private val authApi: AuthApi,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.url.encodedPath.startsWith(AUTH_PREFIX) || response.priorResponse != null) return null
        val sentToken = request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(lock) {
            val current = sessions.current() ?: return null
            if (current.accessToken != sentToken) return request.withToken(current.accessToken)

            val refreshed = runBlocking { apiCall { authApi.refreshSession(RefreshRequest(current.refreshToken)) } }
            val session = refreshed.getOrElse { error ->
                // Only a refused refresh ends the session; a network blip must not log the user out.
                if (error is AppError.Unauthorized || error is AppError.Rejected) runBlocking { sessions.clear() }
                return null
            }
            runBlocking { sessions.save(session) }
            return request.withToken(session.accessToken)
        }
    }

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()
}
