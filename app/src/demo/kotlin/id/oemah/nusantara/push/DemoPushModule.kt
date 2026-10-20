package id.oemah.nusantara.push

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The demo flavor never initialises Firebase. */
@Module
@InstallIn(SingletonComponent::class)
object DemoPushModule {
    @Provides
    fun pushStarter(): PushStarter = PushStarter { }
}
