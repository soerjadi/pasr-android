package id.oemah.nusantara.core.network

import id.oemah.nusantara.api.model.Session
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

fun session(access: String = "old", refresh: String = "r1") =
    Session(accessToken = access, refreshToken = refresh, user = User(id = 1, phone = "+6281234567890"))

class FakeSessionStore(initial: Session? = null) : SessionStore {
    private val state = MutableStateFlow(initial)
    override val session: StateFlow<Session?> = state
    var clearCalls = 0
    override suspend fun save(session: Session) { state.value = session }
    override suspend fun clear() { clearCalls++; state.value = null }
}

fun json(code: Int, body: String): MockResponse = MockResponse.Builder()
    .code(code)
    .addHeader("Content-Type", "application/json")
    .body(body)
    .build()

fun OkHttpClient.Builder.noSilentRetry(): OkHttpClient.Builder = retryOnConnectionFailure(false)

fun MockWebServer.retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(url("/"))
    .client(client)
    .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
    .build()

const val USER_JSON = """{"id":1,"phone":"+6281234567890"}"""
fun sessionJson(access: String, refresh: String) =
    """{"accessToken":"$access","refreshToken":"$refresh","user":$USER_JSON}"""
