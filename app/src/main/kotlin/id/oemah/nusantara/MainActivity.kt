package id.oemah.nusantara

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import id.oemah.nusantara.ui.nav.AppNavHost
import id.oemah.nusantara.ui.nav.appGraph
import id.oemah.nusantara.ui.theme.NusantaraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NusantaraTheme {
                val navController = rememberNavController()
                // A notification tapped while the app is already open arrives as a new intent.
                DisposableEffect(navController) {
                    val listener = Consumer<Intent> { navController.handleDeepLink(it) }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }
                AppNavHost(navController = navController) { appGraph(navController) }
            }
        }
    }
}
