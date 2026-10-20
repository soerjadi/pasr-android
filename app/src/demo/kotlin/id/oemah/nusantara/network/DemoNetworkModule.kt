package id.oemah.nusantara.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
object DemoNetworkModule {
    @Provides
    @IntoSet
    fun fixtureInterceptor(): Interceptor =
        FixtureInterceptor { path -> FixtureInterceptor::class.java.classLoader?.getResourceAsStream(path) }
}
