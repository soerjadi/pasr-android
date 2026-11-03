package id.oemah.nusantara.network

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val JSON = "application/json".toMediaType()

class FixtureTest {
    private val strictJson = Json { ignoreUnknownKeys = false }

    private fun resource(path: String) = javaClass.classLoader!!.getResourceAsStream(path)

    private val client = OkHttpClient.Builder()
        .addInterceptor(FixtureInterceptor { resource(it) })
        .build()

    private fun call(method: String, path: String) = client.newCall(
        Request.Builder()
            .url("http://localhost/$path")
            .method(method, if (method == "GET" || method == "DELETE") null else "{}".toRequestBody(JSON))
            .build(),
    ).execute()

    @Test fun contract_has_60_operations() {
        assertEquals(60, ApiOperations.all.size)
    }

    @Test fun every_generated_operation_has_a_fixture_or_is_bodyless() {
        ApiOperations.all.filter { it.bodyType != null }.forEach { op ->
            assertNotNull("missing fixtures/${op.name}.json", resource("fixtures/${op.name}.json"))
        }
    }

    @Test fun no_fixture_exists_without_a_body_returning_operation() {
        val withBody = ApiOperations.all.filter { it.bodyType != null }.map { it.name }.toSet()
        val allOperationNames = ApiOperations.all.map { it.name }.toSet()
        val files = File("../../docs/api/fixtures").listFiles { f -> f.extension == "json" }!!
            .map { it.nameWithoutExtension }
            .filter { it in allOperationNames }
            .toSet()
        assertEquals(withBody, files)
    }

    @Test fun fixtures_decode_into_generated_models() {
        ApiOperations.all.filter { it.bodyType != null }.forEach { op ->
            val text = resource("fixtures/${op.name}.json")!!.bufferedReader().readText()
            strictJson.decodeFromString(serializer(op.bodyType!!), text) // throws if the fixture drifts from the contract
        }
    }

    @Test fun interceptor_resolves_templated_path() {
        call("GET", "v1/products/42").use { response ->
            assertEquals(200, response.code)
            assertTrue(response.header("Content-Type")!!.startsWith("application/json"))
            val expected = resource("fixtures/getProduct.json")!!.bufferedReader().readText()
            assertEquals(expected, response.body.string())
        }
    }

    @Test fun unknown_route_returns_404() {
        call("GET", "v1/nope").use { assertEquals(404, it.code) }
    }

    @Test fun bodyless_operation_returns_204() {
        call("PUT", "v1/me/favorites/5").use { assertEquals(204, it.code) }
    }
}
