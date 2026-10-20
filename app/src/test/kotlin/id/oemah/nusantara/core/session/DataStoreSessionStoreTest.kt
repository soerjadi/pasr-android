package id.oemah.nusantara.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import id.oemah.nusantara.core.network.session
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSessionStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file by lazy { File(tmp.root, "session.preferences_pb") }
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) }

    /** Marks values so a test can tell encrypted from clear text. */
    private val fakeCipher = object : TokenCipher {
        override fun encrypt(plain: String) = "enc:" + plain.reversed()
        override fun decrypt(cipher: String) = cipher.removePrefix("enc:").reversed()
    }

    @After fun stop() = scope.cancel()

    @Test fun a_new_store_over_the_same_data_reads_the_saved_session() = runBlocking {
        DataStoreSessionStore(dataStore, fakeCipher).save(session(access = "a1", refresh = "r1"))

        val reopened = DataStoreSessionStore(dataStore, fakeCipher)

        assertEquals(session(access = "a1", refresh = "r1"), reopened.current())
    }

    @Test fun clear_empties_the_session_for_new_stores_too() = runBlocking {
        val store = DataStoreSessionStore(dataStore, fakeCipher)
        store.save(session())

        store.clear()

        assertNull(store.current())
        assertNull(DataStoreSessionStore(dataStore, fakeCipher).current())
    }

    @Test fun tokens_are_not_written_in_clear_text() = runBlocking {
        DataStoreSessionStore(dataStore, fakeCipher).save(session(access = "access-secret", refresh = "refresh-secret"))

        val raw = file.readBytes().toString(Charsets.ISO_8859_1)

        assertFalse(raw.contains("access-secret"))
        assertFalse(raw.contains("refresh-secret"))
    }
}
