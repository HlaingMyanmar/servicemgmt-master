package com.sspd.servicemgmt.navigation

import android.net.Uri

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
    object About          : Screen("about")
    object ProductDetail  : Screen("product_detail/{productId}?serial={serialNumber}") {
        fun createRoute(id: Int, serial: String? = null) =
            if (serial != null) "product_detail/$id?serial=${Uri.encode(serial)}"
            else "product_detail/$id"
    }
    object SaleDetail     : Screen("sale_detail/{saleId}") {
        fun createRoute(id: Int) = "sale_detail/$id"
    }
    object NewSale         : Screen("new_sale")
    object SalePrint       : Screen("sale_print/{saleId}") {
        fun createRoute(id: Int) = "sale_print/$id"
    }
    object ServiceMgmt     : Screen("service_mgmt")
    object ShelfLocations  : Screen("shelf_locations")
    object ServiceJobDetail : Screen("service_job_detail/{jobId}") {
        fun createRoute(id: Int) = "service_job_detail/$id"
    }
    object BookingDetail   : Screen("booking_detail/{bookingId}") {
        fun createRoute(id: Int) = "booking_detail/$id"
    }
    object BookingPrint    : Screen("booking_print/{bookingId}") {
        fun createRoute(id: Int) = "booking_print/$id"
    }
    object NewBooking      : Screen("new_booking")
    object EditBooking     : Screen("edit_booking/{bookingId}") {
        fun createRoute(id: Int) = "edit_booking/$id"
    }
    object NewServiceJob   : Screen("new_service_job")
    object EditServiceJob  : Screen("edit_service_job/{jobId}") {
        fun createRoute(id: Int) = "edit_service_job/$id"
    }
    object ServiceJobPrint : Screen("service_job_print/{jobId}") {
        fun createRoute(id: Int) = "service_job_print/$id"
    }
}

const val AUTH_GRAPH = "auth_graph"
const val MAIN_GRAPH = "main_graph"
