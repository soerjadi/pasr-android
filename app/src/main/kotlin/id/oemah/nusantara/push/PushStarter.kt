package id.oemah.nusantara.push

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.BuildConfig
import id.oemah.nusantara.feature.profile.ProfileRepository
import id.oemah.nusantara.core.session.SessionStore
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Starts push delivery at app launch. The demo flavor does nothing; the live flavor initialises Firebase. */
fun interface PushStarter {
    fun start()
}

object PushChannels {
    const val ORDER_STATUS = "order_status"
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object PushModule {
    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun pushTokenRegistrar(
        profile: ProfileRepository,
        sessions: SessionStore,
        @ApplicationScope scope: CoroutineScope,
    ): PushTokenRegistrar = PushTokenRegistrar(profile, sessions, scope)

    @Provides @Named("supportWhatsapp")
    fun supportWhatsapp(): String = BuildConfig.SUPPORT_WHATSAPP
}
