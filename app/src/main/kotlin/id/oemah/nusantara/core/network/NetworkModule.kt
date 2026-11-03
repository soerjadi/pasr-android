package id.oemah.nusantara.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.BuildConfig
import id.oemah.nusantara.api.AdminApi
import id.oemah.nusantara.api.AdsApi
import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.CatalogApi
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.SocialApi
import id.oemah.nusantara.core.session.SessionStore
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BARE = "bare"

    /** No auth handling: used only to refresh the session, so refreshing cannot recurse into itself. */
    @Provides @Singleton @Named(BARE)
    fun bareClient(extra: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(RetryGetInterceptor())
        .apply { extra.forEach(::addInterceptor) }
        .build()

    @Provides @Singleton
    fun authedClient(
        @Named(BARE) bare: OkHttpClient,
        sessions: SessionStore,
        @Named(BARE) refreshApi: AuthApi,
    ): OkHttpClient = bare.newBuilder()
        .addInterceptor(AuthInterceptor(sessions))
        .authenticator(TokenAuthenticator(sessions, refreshApi))
        .build()

    @Provides @Singleton @Named(BARE)
    fun bareRetrofit(@Named(BARE) client: OkHttpClient): Retrofit = retrofit(client)

    @Provides @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton @Named(BARE)
    fun refreshApi(@Named(BARE) retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides @Singleton fun meApi(retrofit: Retrofit): MeApi = retrofit.create(MeApi::class.java)
    @Provides @Singleton fun catalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)
    @Provides @Singleton fun socialApi(retrofit: Retrofit): SocialApi = retrofit.create(SocialApi::class.java)
    @Provides @Singleton fun ordersApi(retrofit: Retrofit): OrdersApi = retrofit.create(OrdersApi::class.java)
    @Provides @Singleton fun adsApi(retrofit: Retrofit): AdsApi = retrofit.create(AdsApi::class.java)
    @Provides @Singleton fun adminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)
}
