package id.oemah.nusantara.feature.checkout

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.feature.cart.CartLine
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Singleton

/**
 * One idempotency key per distinct cart, remembered on disk until the order succeeds. A retry after
 * a timeout, a double tap, or the app being killed mid-request therefore reuses the key and the
 * server returns the order it already created instead of creating a second one.
 */
class IdempotencyKeyStore(private val dataStore: DataStore<Preferences>) {
    suspend fun keyFor(lines: List<CartLine>): String {
        val fingerprint = fingerprint(lines)
        lateinit var key: String
        dataStore.edit { prefs ->
            val stored = prefs[KEY]
            key = if (prefs[FINGERPRINT] == fingerprint && stored != null) {
                stored
            } else {
                UUID.randomUUID().toString().also {
                    prefs[FINGERPRINT] = fingerprint
                    prefs[KEY] = it
                }
            }
        }
        return key
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun fingerprint(lines: List<CartLine>): String {
        val canonical = lines.sortedBy { it.productId }.joinToString(",") { "${it.productId}:${it.quantity}" }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val FINGERPRINT = stringPreferencesKey("fingerprint")
        val KEY = stringPreferencesKey("key")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object IdempotencyModule {
    @Provides @Singleton
    fun idempotencyKeyStore(@ApplicationContext context: Context): IdempotencyKeyStore =
        IdempotencyKeyStore(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("checkout") })
}
