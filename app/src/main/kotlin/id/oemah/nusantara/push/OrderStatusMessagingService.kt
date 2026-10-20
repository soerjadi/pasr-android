package id.oemah.nusantara.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.nav.Route
import javax.inject.Inject

@AndroidEntryPoint
class OrderStatusMessagingService : FirebaseMessagingService() {
    @Inject lateinit var registrar: PushTokenRegistrar

    override fun onNewToken(token: String) {
        registrar.onToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val route = NotificationRoutes.routeFor(message.data) as? Route.OrderTracking ?: return
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!canNotify) return

        val open = Intent(Intent.ACTION_VIEW, Uri.parse(NotificationRoutes.trackingUri(route.orderId))).setPackage(packageName)
        val pending = PendingIntent.getActivity(
            this, route.orderId.toInt(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, PushChannels.ORDER_STATUS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(message.data["title"] ?: getString(R.string.push_default_title))
            .setContentText(message.data["body"] ?: NotificationRoutes.defaultBody(message.data["status"]))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(route.orderId.toInt(), notification)
    }
}
