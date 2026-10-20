package id.oemah.nusantara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                AppNavHost(navController = navController) { appGraph(navController) }
            }
        }
    }
}
