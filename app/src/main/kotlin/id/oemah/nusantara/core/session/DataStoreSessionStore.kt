package id.oemah.nusantara.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.oemah.nusantara.api.model.Session
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.network.AppJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** Persists the session; tokens are encrypted by [cipher], never written in clear text. */
class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : SessionStore {
    // Read once, synchronously: the first request must already know whether the user is logged in.
    private val state = MutableStateFlow(runBlocking { read() })
    override val session: StateFlow<Session?> = state.asStateFlow()

    override suspend fun save(session: Session) {
        dataStore.edit {
            it[ACCESS] = cipher.encrypt(session.accessToken)
            it[REFRESH] = cipher.encrypt(session.refreshToken)
            it[USER] = AppJson.encodeToString(session.user)
        }
        state.value = session
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
        state.value = null
    }

    /** An unreadable session (e.g. the keystore key was lost) counts as logged out. */
    private suspend fun read(): Session? = runCatching {
        val prefs = dataStore.data.first()
        Session(
            accessToken = cipher.decrypt(prefs[ACCESS] ?: return null),
            refreshToken = cipher.decrypt(prefs[REFRESH] ?: return null),
            user = AppJson.decodeFromString<User>(prefs[USER] ?: return null),
        )
    }.getOrNull()

    private companion object {
        val ACCESS = stringPreferencesKey("access")
        val REFRESH = stringPreferencesKey("refresh")
        val USER = stringPreferencesKey("user")
    }
}
