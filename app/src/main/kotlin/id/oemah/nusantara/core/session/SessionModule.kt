package id.oemah.nusantara.core.session

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides @Singleton
    fun tokenCipher(): TokenCipher = KeystoreTokenCipher()

    @Provides @Singleton
    fun sessionStore(@ApplicationContext context: Context, cipher: TokenCipher): SessionStore =
        DataStoreSessionStore(
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") },
            cipher,
        )
}
