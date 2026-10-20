package id.oemah.nusantara.core.network

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

/** Flavor-specific interceptors (e.g. demo fixtures) contribute to this set; `live` contributes none. */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorsModule {
    @Multibinds
    abstract fun interceptors(): Set<@JvmSuppressWildcards Interceptor>
}
