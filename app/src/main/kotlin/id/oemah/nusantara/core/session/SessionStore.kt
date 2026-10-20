package id.oemah.nusantara.core.session

import id.oemah.nusantara.api.model.Session
import kotlinx.coroutines.flow.StateFlow

interface SessionStore {
    val session: StateFlow<Session?>
    fun current(): Session? = session.value
    suspend fun save(session: Session)
    suspend fun clear()
}
