package id.oemah.nusantara.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

fun interface Sleeper {
    fun sleep(ms: Long)
}

/** Retries idempotent GETs twice (after 300 ms, then 900 ms) on I/O failure or HTTP 503. Never retries other methods. */
class RetryGetInterceptor(private val sleeper: Sleeper = Sleeper { Thread.sleep(it) }) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                if (response.code != 503 || attempt >= BACKOFF_MS.size) return response
                response.close()
            } catch (e: IOException) {
                if (attempt >= BACKOFF_MS.size) throw e
            }
            sleeper.sleep(BACKOFF_MS[attempt++])
        }
    }

    private companion object {
        val BACKOFF_MS = longArrayOf(300, 900)
    }
}
