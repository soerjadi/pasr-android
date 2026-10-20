package id.oemah.nusantara.push

import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the server informed of this device's push token for the logged-in user: holds a token
 * received while logged out, registers each token once, retries on the next trigger after a
 * failure, and registers again after logout/login so the new user gets the notifications.
 */
class PushTokenRegistrar(
    private val profile: ProfileRepository,
    private val sessions: SessionStore,
    private val scope: CoroutineScope,
) {
    private val lock = Mutex()
    private var token: String? = null
    private var registered: String? = null

    init {
        scope.launch {
            sessions.session.map { it != null }.distinctUntilChanged().collect { loggedIn ->
                if (loggedIn) flush() else lock.withLock { registered = null }
            }
        }
    }

    fun onToken(newToken: String) {
        scope.launch {
            lock.withLock { token = newToken }
            if (sessions.current() != null) flush()
        }
    }

    private suspend fun flush() = lock.withLock {
        val current = token ?: return@withLock
        if (current == registered) return@withLock
        profile.registerDevice(current).onSuccess { registered = current }
    }
}
