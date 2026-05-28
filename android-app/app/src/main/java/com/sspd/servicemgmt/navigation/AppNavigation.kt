package com.sspd.servicemgmt.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.sspd.servicemgmt.ui.screens.*
import com.sspd.servicemgmt.ui.viewmodel.ServerStatus
import com.sspd.servicemgmt.ui.viewmodel.ServerStatusViewModel
import com.sspd.servicemgmt.utils.PreferenceManager

val LocalServerStatus = compositionLocalOf { ServerStatus.CHECKING }

private const val ANIM_MS = 220

private fun NavGraphBuilder.screen(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(ANIM_MS - 40))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(ANIM_MS - 40))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)
            )
        }
    ) { entry -> content(entry) }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs   = remember { PreferenceManager(context) }
    val nav     = rememberNavController()
    val start   = if (prefs.authToken.isNotEmpty()) MAIN_GRAPH else AUTH_GRAPH

    val serverVm: ServerStatusViewModel = viewModel()
    val serverStatus by serverVm.status.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalServerStatus provides serverStatus) {
        NavHost(navController = nav, startDestination = start) {

            // ── Auth Graph ──────────────────────────────────────────────────
            navigation(startDestination = Screen.Login.route, route = AUTH_GRAPH) {
                screen(Screen.Login.route) {
                    LoginScreen(onSuccess = {
                        nav.navigate(MAIN_GRAPH) {
                            popUpTo(AUTH_GRAPH) { inclusive = true }
                        }
                    })
                }
            }

            // ── Main Graph ──────────────────────────────────────────────────
            navigation(startDestination = Screen.Home.route, route = MAIN_GRAPH) {

                screen(Screen.Home.route) {
                    HomeScreen(
                        onNavigate = { route -> nav.navigate(route) },
                        onLogout   = {
                            nav.navigate(AUTH_GRAPH) {
                                popUpTo(MAIN_GRAPH) { inclusive = true }
                            }
                        }
                    )
                }

                screen(Screen.Sales.route)        { SaleListScreen       { nav.popBackStack() } }
                screen(Screen.Products.route)     { ProductListScreen    { nav.popBackStack() } }
                screen(Screen.ServiceJobs.route)  { ServiceJobListScreen { nav.popBackStack() } }
                screen(Screen.Bookings.route)     { BookingListScreen    { nav.popBackStack() } }
                screen(Screen.StaffReport.route)  { StaffReportScreen    { nav.popBackStack() } }
                screen(Screen.Expenses.route)     { ExpenseScreen        { nav.popBackStack() } }
                screen(Screen.SalesRanking.route) { SalesRankingScreen   { nav.popBackStack() } }
                screen(Screen.AuditLog.route)     { AuditLogScreen       { nav.popBackStack() } }
                screen(Screen.Chat.route)         { ChatScreen           { nav.popBackStack() } }
                screen(Screen.Account.route)      { AccountSettingsScreen { nav.popBackStack() } }
                screen(Screen.About.route)        { AboutScreen          { nav.popBackStack() } }
            }
        }
    }
}
