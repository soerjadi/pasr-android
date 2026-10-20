package id.oemah.nusantara.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import id.oemah.nusantara.R
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState

enum class BottomTab(val route: Route, @StringRes val label: Int, val icon: ImageVector) {
    Home(Route.Home, R.string.tab_home, Icons.Default.Home),
    Categories(Route.Categories, R.string.tab_categories, Icons.Default.Menu),
    Orders(Route.Orders, R.string.tab_orders, Icons.AutoMirrored.Filled.List),
    Favorites(Route.Favorites, R.string.tab_favorites, Icons.Default.Favorite),
    Account(Route.Account, R.string.tab_account, Icons.Default.Person),
}

/** The tab shell (bottom bar + NavHost). The destinations come from [graph]. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Route = Route.Home,
    graph: NavGraphBuilder.() -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val onTab = BottomTab.entries.any { tab -> destination?.hasRoute(tab.route::class) == true }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (onTab || destination == null) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        val label = stringResource(tab.label)
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            builder = graph,
        )
    }
}
