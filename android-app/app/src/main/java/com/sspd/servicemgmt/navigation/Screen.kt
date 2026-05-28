package com.sspd.servicemgmt.navigation

sealed class Screen(val route: String) {
    object Login        : Screen("login")
    object Home         : Screen("home")
    object Sales        : Screen("sales")
    object Products     : Screen("products")
    object ServiceJobs  : Screen("service_jobs")
    object Bookings     : Screen("bookings")
    object StaffReport  : Screen("staff_report")
    object Expenses     : Screen("expenses")
    object SalesRanking : Screen("sales_ranking")
    object AuditLog     : Screen("audit_log")
    object Chat         : Screen("chat")
    object Account      : Screen("account")
    object About        : Screen("about")
}

const val AUTH_GRAPH = "auth_graph"
const val MAIN_GRAPH = "main_graph"
