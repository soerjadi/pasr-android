package id.oemah.nusantara.feature.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Remembers that the intro has been shown, so it appears on the first launch only. */
class OnboardingStore(private val dataStore: DataStore<Preferences>) {
    val seen: Flow<Boolean> = dataStore.data.map { it[SEEN] == true }

    suspend fun markSeen() {
        dataStore.edit { it[SEEN] = true }
    }

    private companion object {
        val SEEN = booleanPreferencesKey("seen")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {
    @Provides @Singleton
    fun onboardingStore(@ApplicationContext context: Context): OnboardingStore =
        OnboardingStore(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("onboarding") })
}
