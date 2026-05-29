package com.sspd.servicemgmt.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.ui.screens.*
import com.sspd.servicemgmt.ui.theme.Primary
import com.sspd.servicemgmt.ui.theme.TextMuted
import com.sspd.servicemgmt.ui.viewmodel.ServerStatus
import com.sspd.servicemgmt.ui.viewmodel.ServerStatusViewModel
import com.sspd.servicemgmt.utils.PreferenceManager

val LocalServerStatus = compositionLocalOf { ServerStatus.CHECKING }

private const val ANIM_MS = 220

private data class BottomNavItem(
    val route: String,
    val icon:  ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route,        Icons.Default.Home,         "ပင်မ"),
    BottomNavItem(Screen.Sales.route,       Icons.Default.Receipt,      "ရောင်းချ"),
    BottomNavItem(Screen.Bookings.route,    Icons.Default.CalendarMonth,"Booking"),
    BottomNavItem(Screen.ServiceJobs.route, Icons.Default.Build,        "Job"),
    BottomNavItem(Screen.Products.route,    Icons.Default.Inventory2,   "ကုန်ပစ္စည်း")
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

private fun NavGraphBuilder.screen(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) +
            fadeOut(tween(ANIM_MS - 40))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) +
            fadeIn(tween(ANIM_MS - 40))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing))
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

    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    fun navigateTab(route: String) {
        nav.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    CompositionLocalProvider(LocalServerStatus provides serverStatus) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick  = { navigateTab(item.route) },
                                icon = {
                                    Icon(item.icon, contentDescription = item.label)
                                },
                                label = {
                                    Text(
                                        item.label,
                                        fontSize   = 10.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = Primary,
                                    selectedTextColor   = Primary,
                                    indicatorColor      = Primary.copy(alpha = 0.12f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController    = nav,
                startDestination = start,
                modifier         = Modifier.padding(innerPadding)
            ) {

                // ── Auth Graph ──────────────────────────────────────────────
                navigation(startDestination = Screen.Login.route, route = AUTH_GRAPH) {
                    screen(Screen.Login.route) {
                        LoginScreen(onSuccess = {
                            nav.navigate(MAIN_GRAPH) {
                                popUpTo(AUTH_GRAPH) { inclusive = true }
                            }
                        })
                    }
                }

                // ── Main Graph ──────────────────────────────────────────────
                navigation(startDestination = Screen.Home.route, route = MAIN_GRAPH) {

                    screen(Screen.Home.route) {
                        HomeScreen(
                            onNavigate = { route ->
                                if (route in bottomNavRoutes) navigateTab(route)
                                else nav.navigate(route)
                            },
                            onLogout = {
                                nav.navigate(AUTH_GRAPH) {
                                    popUpTo(MAIN_GRAPH) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Sales ───────────────────────────────────────────────
                    screen(Screen.Sales.route) {
                        SaleListScreen(
                            onBack      = { nav.popBackStack() },
                            onSaleClick = { id -> nav.navigate(Screen.SaleDetail.createRoute(id)) },
                            onNewSale   = { nav.navigate(Screen.NewSale.route) }
                        )
                    }
                    screen(Screen.NewSale.route) {
                        NewSaleScreen(
                            onBack    = { nav.popBackStack() },
                            onSuccess = { id ->
                                nav.navigate(Screen.SaleDetail.createRoute(id)) {
                                    popUpTo(Screen.NewSale.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.SaleDetail.route,
                        arguments = listOf(navArgument("saleId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) { entry ->
                        val saleId = entry.arguments?.getInt("saleId") ?: 0
                        SaleDetailScreen(
                            onBack  = { nav.popBackStack() },
                            onPrint = { nav.navigate(Screen.SalePrint.createRoute(saleId)) }
                        )
                    }
                    composable(
                        route = Screen.SalePrint.route,
                        arguments = listOf(navArgument("saleId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) { entry ->
                        val saleId = entry.arguments?.getInt("saleId") ?: 0
                        SalePrintScreen(saleId = saleId, onBack = { nav.popBackStack() })
                    }

                    // ── Products ────────────────────────────────────────────
                    screen(Screen.Products.route) {
                        ProductListScreen(
                            onBack         = { nav.popBackStack() },
                            onProductClick = { id -> nav.navigate(Screen.ProductDetail.createRoute(id)) },
                            onScanNavigate = { id, serial -> nav.navigate(Screen.ProductDetail.createRoute(id, serial)) }
                        )
                    }
                    composable(
                        route = Screen.ProductDetail.route,
                        arguments = listOf(
                            navArgument("productId") { type = NavType.IntType },
                            navArgument("serialNumber") { type = NavType.StringType; nullable = true; defaultValue = null }
                        ),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) {
                        ProductDetailScreen(onBack = { nav.popBackStack() })
                    }

                    // ── Bookings ────────────────────────────────────────────
                    screen(Screen.Bookings.route) {
                        BookingListScreen(
                            onBack         = { nav.popBackStack() },
                            onBookingClick = { id -> nav.navigate(Screen.BookingDetail.createRoute(id)) },
                            onNewBooking   = { nav.navigate(Screen.NewBooking.route) },
                            onEditBooking  = { id -> nav.navigate(Screen.EditBooking.createRoute(id)) }
                        )
                    }
                    screen(Screen.NewBooking.route) {
                        BookingFormScreen(
                            onBack    = { nav.popBackStack() },
                            onSuccess = {
                                nav.navigate(Screen.Bookings.route) {
                                    popUpTo(Screen.NewBooking.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.EditBooking.route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) {
                        BookingFormScreen(
                            onBack    = { nav.popBackStack() },
                            onSuccess = { nav.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.BookingDetail.route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) { entry ->
                        val bookingId = entry.arguments?.getInt("bookingId") ?: -1
                        BookingDetailScreen(
                            onBack       = { nav.popBackStack() },
                            onJobCreated = { nav.navigate(Screen.ServiceJobs.route) },
                            onEdit       = { nav.navigate(Screen.EditBooking.createRoute(bookingId)) },
                            onPrint      = { nav.navigate(Screen.BookingPrint.createRoute(bookingId)) }
                        )
                    }
                    composable(
                        route = Screen.BookingPrint.route,
                        arguments = listOf(navArgument("bookingId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) {
                        BookingPrintScreen(onBack = { nav.popBackStack() })
                    }

                    // ── Service Jobs ────────────────────────────────────────
                    screen(Screen.ServiceJobs.route) {
                        ServiceJobListScreen(
                            onBack     = { nav.popBackStack() },
                            onJobClick = { id -> nav.navigate(Screen.ServiceJobDetail.createRoute(id)) },
                            onNewJob   = { nav.navigate(Screen.NewServiceJob.route) }
                        )
                    }
                    screen(Screen.NewServiceJob.route) {
                        ServiceJobFormScreen(
                            onBack    = { nav.popBackStack() },
                            onSuccess = { job ->
                                nav.navigate(Screen.ServiceJobDetail.createRoute(job.id!!)) {
                                    popUpTo(Screen.NewServiceJob.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.EditServiceJob.route,
                        arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) {
                        ServiceJobFormScreen(
                            onBack    = { nav.popBackStack() },
                            onSuccess = { nav.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.ServiceJobDetail.route,
                        arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) { entry ->
                        val jobId = entry.arguments?.getInt("jobId") ?: 0
                        ServiceJobDetailScreen(
                            onBack    = { nav.popBackStack() },
                            onEdit    = { nav.navigate(Screen.EditServiceJob.createRoute(jobId)) },
                            onPrint   = { nav.navigate(Screen.ServiceJobPrint.createRoute(jobId)) },
                            onDeleted = { nav.navigate(Screen.ServiceJobs.route) {
                                popUpTo(Screen.ServiceJobDetail.route) { inclusive = true }
                            }}
                        )
                    }
                    composable(
                        route = Screen.ServiceJobPrint.route,
                        arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
                        enterTransition    = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) },
                        exitTransition     = { slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(ANIM_MS - 40)) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(ANIM_MS - 40)) },
                        popExitTransition  = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_MS, easing = FastOutSlowInEasing)) }
                    ) {
                        ServiceJobPrintScreen(onBack = { nav.popBackStack() })
                    }

                    // ── Other screens ───────────────────────────────────────
                    screen(Screen.ServiceMgmt.route)    { ServiceManagementScreen { nav.popBackStack() } }
                    screen(Screen.ShelfLocations.route) { ShelfLocationScreen     { nav.popBackStack() } }
                    screen(Screen.StaffReport.route)    { StaffReportScreen       { nav.popBackStack() } }
                    screen(Screen.Expenses.route)       { ExpenseScreen           { nav.popBackStack() } }
                    screen(Screen.SalesRanking.route)   { SalesRankingScreen      { nav.popBackStack() } }
                    screen(Screen.AuditLog.route)       { AuditLogScreen          { nav.popBackStack() } }
                    screen(Screen.Chat.route)           { ChatScreen              { nav.popBackStack() } }
                    screen(Screen.Account.route)        { AccountSettingsScreen   { nav.popBackStack() } }
                    screen(Screen.About.route)          { AboutScreen             { nav.popBackStack() } }
                }
            }
        }
    }
}
