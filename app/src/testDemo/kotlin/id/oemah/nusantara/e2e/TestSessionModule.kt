package id.oemah.nusantara.e2e

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.session.SessionModule
import id.oemah.nusantara.core.session.SessionStore
import javax.inject.Singleton

/** The Android Keystore does not exist on the JVM, so end-to-end tests keep the session in memory. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SessionModule::class])
object TestSessionModule {
    @Provides @Singleton
    fun sessionStore(): SessionStore = FakeSessionStore()
}
