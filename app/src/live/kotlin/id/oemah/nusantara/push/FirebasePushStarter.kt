package id.oemah.nusantara.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.BuildConfig

/**
 * Initialises Firebase from Gradle properties (nusantara.firebase.*), so no google-services.json is
 * needed. With the properties blank, push is simply off.
 */
class FirebasePushStarter(private val context: Context, private val registrar: PushTokenRegistrar) : PushStarter {
    override fun start() {
        if (listOf(BuildConfig.FIREBASE_APP_ID, BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_SENDER_ID).any { it.isBlank() }) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { registrar.onToken(it) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LivePushModule {
    @Provides
    fun pushStarter(@ApplicationContext context: Context, registrar: PushTokenRegistrar): PushStarter =
        FirebasePushStarter(context, registrar)
}
