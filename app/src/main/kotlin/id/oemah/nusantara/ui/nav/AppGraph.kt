package id.oemah.nusantara.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import id.oemah.nusantara.R
import id.oemah.nusantara.feature.auth.LoginRoute
import id.oemah.nusantara.feature.cart.CartRoute
import id.oemah.nusantara.feature.categories.CategoriesRoute
import id.oemah.nusantara.feature.checkout.CheckoutRoute
import id.oemah.nusantara.feature.home.HomeRoute
import id.oemah.nusantara.feature.onboarding.OnboardingRoute
import id.oemah.nusantara.feature.orders.ConfirmationRoute
import id.oemah.nusantara.feature.orders.TrackingRoute
import id.oemah.nusantara.feature.orders.history.OrderHistoryRoute
import id.oemah.nusantara.feature.products.ProductListRoute
import id.oemah.nusantara.feature.profile.ProfileRoute
import id.oemah.nusantara.feature.products.SearchRoute
import id.oemah.nusantara.feature.products.detail.ProductDetailRoute
import id.oemah.nusantara.feature.review.WriteReviewRoute
import id.oemah.nusantara.feature.seller.SellerRoute
import id.oemah.nusantara.feature.social.FavoritesRoute

/** Every real destination of the app. Later features register their screens here. */
fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable<Route.Onboarding> {
        OnboardingRoute(
            onFinish = { navController.navigate(Route.Home) { popUpTo(Route.Onboarding) { inclusive = true } } },
        )
    }
    composable<Route.Home> {
        val featuredTitle = stringResource(R.string.home_featured)
        HomeRoute(
            onSearch = { navController.navigate(Route.Search) },
            onOpenFeatured = { navController.navigate(Route.ProductList(null, featuredTitle, true)) },
            onOpenCategory = { navController.navigate(Route.ProductList(it.id, it.name, false)) },
            onOpenAllCategories = { navController.navigate(Route.Categories) { launchSingleTop = true } },
            onOpenProduct = { navController.navigate(Route.ProductDetail(it)) },
            onOpenCart = { navController.navigate(Route.Cart) },
        )
    }
    composable<Route.Categories> {
        CategoriesRoute(onOpenCategory = { navController.navigate(Route.ProductList(it.id, it.name, false)) })
    }
    composable<Route.ProductList> {
        ProductListRoute(
            onBack = { navController.popBackStack() },
            onOpenProduct = { navController.navigate(Route.ProductDetail(it)) },
            onOpenCart = { navController.navigate(Route.Cart) },
        )
    }
    composable<Route.Search> {
        SearchRoute(
            onBack = { navController.popBackStack() },
            onOpenProduct = { navController.navigate(Route.ProductDetail(it)) },
        )
    }
    composable<Route.ProductDetail> {
        ProductDetailRoute(
            onBack = { navController.popBackStack() },
            onOpenSeller = { navController.navigate(Route.Seller(it)) },
            onLoginRequired = { navController.navigate(Route.Login) },
            onOpenCart = { navController.navigate(Route.Cart) },
        )
    }
    composable<Route.Seller> {
        SellerRoute(
            onBack = { navController.popBackStack() },
            onOpenProduct = { navController.navigate(Route.ProductDetail(it)) },
            onLoginRequired = { navController.navigate(Route.Login) },
            onOpenCart = { navController.navigate(Route.Cart) },
        )
    }
    composable<Route.Cart> {
        CartRoute(
            onBack = { navController.popBackStack() },
            onBrowse = { navController.popBackStack(Route.Home, inclusive = false) },
            onCheckout = { navController.navigate(Route.Checkout) },
            onLoginRequired = { navController.navigate(Route.Login) },
        )
    }
    composable<Route.Checkout> {
        CheckoutRoute(
            onBack = { navController.popBackStack() },
            onPlaced = { orderId ->
                navController.navigate(Route.OrderConfirmation(orderId)) { popUpTo(Route.Home) }
            },
            onBackToCart = { navController.popBackStack() },
            onLoginRequired = { navController.navigate(Route.Login) },
        )
    }
    composable<Route.OrderConfirmation> {
        ConfirmationRoute(
            onViewOrder = { navController.navigate(Route.OrderTracking(it)) },
            onHome = { navController.popBackStack(Route.Home, inclusive = false) },
        )
    }
    composable<Route.OrderTracking>(
        deepLinks = listOf(navDeepLink<Route.OrderTracking>(basePath = "nusantara://order")),
    ) {
        TrackingRoute(
            onBack = { navController.popBackStack() },
            onReview = { itemId, name -> navController.navigate(Route.WriteReview(itemId, name)) },
        )
    }
    composable<Route.Orders> {
        OrderHistoryRoute(
            onOpenOrder = { navController.navigate(Route.OrderTracking(it)) },
            onLogin = { navController.navigate(Route.Login) },
        )
    }
    composable<Route.Favorites> {
        FavoritesRoute(
            onOpenProduct = { navController.navigate(Route.ProductDetail(it)) },
            onLogin = { navController.navigate(Route.Login) },
        )
    }
    composable<Route.WriteReview> { WriteReviewRoute(onClose = { navController.popBackStack() }) }
    composable<Route.Account> {
        ProfileRoute(
            onOpenOrders = { navController.navigate(Route.Orders) { launchSingleTop = true } },
            onLogin = { navController.navigate(Route.Login) },
        )
    }
    composable<Route.Login> { LoginRoute(onLoggedIn = { navController.popBackStack() }) }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.placeholder_screen, name))
    }
}
