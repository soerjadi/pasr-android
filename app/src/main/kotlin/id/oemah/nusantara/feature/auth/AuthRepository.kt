package id.oemah.nusantara.feature.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.model.OtpRequest
import id.oemah.nusantara.api.model.OtpVerify
import id.oemah.nusantara.api.model.RefreshRequest
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.session.SessionStore
import javax.inject.Inject

interface AuthRepository {
    suspend fun requestOtp(phoneE164: String): Result<Unit>

    /** On success the session is saved, so the user is logged in when this returns. */
    suspend fun verifyOtp(phoneE164: String, code: String): Result<Unit>

    suspend fun logout()
}

class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val sessions: SessionStore,
) : AuthRepository {
    override suspend fun requestOtp(phoneE164: String): Result<Unit> =
        apiCall { api.requestOtp(OtpRequest(phoneE164)) }

    override suspend fun verifyOtp(phoneE164: String, code: String): Result<Unit> =
        apiCall { api.verifyOtp(OtpVerify(phoneE164, code)) }.map { sessions.save(it) }

    /** Best-effort revoke; the user is logged out locally even when the server cannot be reached. */
    override suspend fun logout() {
        sessions.current()?.let { apiCall { api.logout(RefreshRequest(it.refreshToken)) } }
        sessions.clear()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds abstract fun authRepository(impl: DefaultAuthRepository): AuthRepository
}
