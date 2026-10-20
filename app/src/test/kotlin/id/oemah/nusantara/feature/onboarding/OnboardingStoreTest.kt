package id.oemah.nusantara.feature.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnboardingStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "onboarding.preferences_pb") } }

    @After fun stop() = scope.cancel()

    @Test fun a_fresh_install_has_not_seen_the_onboarding() = runBlocking {
        assertFalse(OnboardingStore(dataStore).seen.first())
    }

    @Test fun marking_it_seen_sticks_including_for_a_new_store() = runBlocking {
        OnboardingStore(dataStore).markSeen()

        assertTrue(OnboardingStore(dataStore).seen.first())
    }
}
