package id.oemah.nusantara.network

import java.io.InputStream
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Serves `fixtures/<operationId>.json` instead of calling a backend (demo flavor only). */
class FixtureInterceptor(private val openResource: (String) -> InputStream?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.removePrefix("/")
        val operation = ApiOperations.all.firstOrNull { it.httpMethod == request.method && it.pattern.matches(path) }
            ?: return respond(request, 404, "Not Found", NOT_FOUND_BODY)

        if (operation.bodyType == null) return respond(request, 204, "No Content", "")

        val body = openResource("fixtures/${operation.name}.json")?.use { it.bufferedReader().readText() }
            ?: return respond(request, 404, "Not Found", NOT_FOUND_BODY)
        return respond(request, 200, "OK", body)
    }

    private fun respond(request: Request, code: Int, message: String, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .header("Content-Type", "application/json")
            .body(body.toResponseBody(JSON))
            .build()

    private companion object {
        val JSON = "application/json".toMediaType()
        const val NOT_FOUND_BODY = """{"code":"NOT_FOUND","message":"No fixture for this route"}"""
    }
}
