package id.oemah.nusantara

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import dagger.hilt.android.HiltAndroidApp
import id.oemah.nusantara.push.PushChannels
import id.oemah.nusantara.push.PushStarter
import javax.inject.Inject
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class NusantaraApp : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var pushStarter: PushStarter

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        pushStarter.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(PushChannels.ORDER_STATUS, getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Image disk cache is on: product photos are re-seen constantly and mobile data is expensive. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
}
