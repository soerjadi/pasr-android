package id.oemah.nusantara.feature.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.DeviceRequest
import id.oemah.nusantara.api.model.UpdateMeRequest
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.network.apiCall
import javax.inject.Inject

interface ProfileRepository {
    suspend fun me(): Result<User>
    suspend fun updateName(name: String): Result<User>
    suspend fun registerDevice(token: String): Result<Unit>
}

class DefaultProfileRepository @Inject constructor(private val api: MeApi) : ProfileRepository {
    override suspend fun me(): Result<User> = apiCall { api.getMe() }

    override suspend fun updateName(name: String): Result<User> = apiCall { api.updateMe(UpdateMeRequest(name)) }

    override suspend fun registerDevice(token: String): Result<Unit> = apiCall { api.registerDevice(DeviceRequest(token)) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds abstract fun profileRepository(impl: DefaultProfileRepository): ProfileRepository
}
