package id.oemah.nusantara

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class NusantaraApp : Application(), SingletonImageLoader.Factory {
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
