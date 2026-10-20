package id.oemah.nusantara.core.network

import id.oemah.nusantara.core.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/** Adds the access token to every request except the auth endpoints, which are public. */
class AuthInterceptor(private val sessions: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessions.current()?.accessToken
        if (token == null || request.url.encodedPath.startsWith(AUTH_PREFIX)) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

internal const val AUTH_PREFIX = "/v1/auth/"
