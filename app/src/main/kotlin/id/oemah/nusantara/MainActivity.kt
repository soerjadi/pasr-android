package id.oemah.nusantara

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import id.oemah.nusantara.feature.onboarding.OnboardingViewModel
import id.oemah.nusantara.feature.onboarding.SplashScreen
import id.oemah.nusantara.ui.nav.AppNavHost
import id.oemah.nusantara.ui.nav.Route
import id.oemah.nusantara.ui.nav.appGraph
import id.oemah.nusantara.ui.theme.NusantaraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NusantaraTheme {
                // Decided once, from the first answer: the intro is shown on the first launch only, and the
                // graph must not be rebuilt when the "seen" flag flips while the user is finishing it.
                val seen = hiltViewModel<OnboardingViewModel>().seen.collectAsStateWithLifecycle().value
                var start by remember { mutableStateOf<Route?>(null) }
                if (start == null && seen != null) start = if (seen) Route.Home else Route.Onboarding

                when (val startDestination = start) {
                    null -> SplashScreen()
                    else -> {
                        val navController = rememberNavController()
                        // A notification tapped while the app is already open arrives as a new intent.
                        DisposableEffect(navController) {
                            val listener = Consumer<Intent> { navController.handleDeepLink(it) }
                            addOnNewIntentListener(listener)
                            onDispose { removeOnNewIntentListener(listener) }
                        }
                        AppNavHost(navController = navController, startDestination = startDestination) { appGraph(navController) }
                    }
                }
            }
        }
    }
}
