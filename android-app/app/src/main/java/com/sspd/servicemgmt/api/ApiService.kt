package com.sspd.servicemgmt.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("app/version")
    suspend fun getAppVersion(): Response<ApiResponse<AppVersionDTO>>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<AuthResponse>>

    @GET("dashboard/stats")
    suspend fun getStats(@Header("Authorization") auth: String): Response<ApiResponse<DashboardStats>>

    @GET("products")
    suspend fun getProducts(@Header("Authorization") auth: String): Response<ApiResponse<List<ProductDTO>>>

    @GET("sales")
    suspend fun getSales(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("search") search: String = ""
    ): Response<ApiResponse<PagedResponse<SaleDTO>>>

    @GET("payment-transactions/reference/{refId}")
    suspend fun getPaymentTransactions(
        @Header("Authorization") auth: String,
        @Path("refId") refId: Int,
        @Query("type") type: String = "Sale"
    ): Response<ApiResponse<List<PaymentTransactionDTO>>>

    @POST("sales/{id}/pay")
    suspend fun payDue(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: SalePaymentRequest
    ): Response<ApiResponse<SaleDTO>>

    @GET("sales/{id}")
    suspend fun getSaleById(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<SaleDTO>>

    // ── Sale Returns ──────────────────────────────────────────────────────────
    @GET("sale-returns")
    suspend fun getSaleReturns(
        @Header("Authorization") auth: String,
        @Query("page")   page:   Int    = 0,
        @Query("size")   size:   Int    = 50,
        @Query("search") search: String = ""
    ): Response<ApiResponse<PagedResponse<SaleReturnDTO>>>

    @GET("sale-returns/{id}")
    suspend fun getSaleReturnById(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<SaleReturnDTO>>

    @POST("sale-returns")
    suspend fun createSaleReturn(
        @Header("Authorization") auth: String,
        @Body body: SaleReturnDTO
    ): Response<ApiResponse<SaleReturnDTO>>

    @PUT("sale-returns/{id}")
    suspend fun updateSaleReturn(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: SaleReturnDTO
    ): Response<ApiResponse<SaleReturnDTO>>

    @POST("sales")
    suspend fun createSale(
        @Header("Authorization") auth: String,
        @Body body: SaleDTO
    ): Response<ApiResponse<SaleDTO>>

    @GET("customers")
    suspend fun getCustomers(@Header("Authorization") auth: String): Response<ApiResponse<List<CustomerDTO>>>

    @POST("customers")
    suspend fun createCustomer(
        @Header("Authorization") auth: String,
        @Body body: CustomerDTO
    ): Response<ApiResponse<CustomerDTO>>

    @GET("staffs/active")
    suspend fun getActiveStaff(@Header("Authorization") auth: String): Response<ApiResponse<List<StaffDTO>>>

    @GET("payment-methods/active")
    suspend fun getActivePaymentMethods(@Header("Authorization") auth: String): Response<ApiResponse<List<PaymentMethodDTO>>>

    @GET("credit-terms/customer/{customerId}")
    suspend fun getCreditTerm(
        @Header("Authorization") auth: String,
        @Path("customerId") customerId: Int
    ): Response<ApiResponse<CustomerCreditTermDTO>>

    // ── Service Types ─────────────────────────────────────────────────────────
    @GET("service-types")
    suspend fun getServiceTypes(@Header("Authorization") auth: String): Response<ApiResponse<List<ServiceTypeDTO>>>

    @GET("service-types/active")
    suspend fun getActiveServiceTypes(@Header("Authorization") auth: String): Response<ApiResponse<List<ServiceTypeDTO>>>

    @POST("service-types")
    suspend fun createServiceType(
        @Header("Authorization") auth: String,
        @Body body: ServiceTypeDTO
    ): Response<ApiResponse<ServiceTypeDTO>>

    @PUT("service-types/{id}")
    suspend fun updateServiceType(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ServiceTypeDTO
    ): Response<ApiResponse<ServiceTypeDTO>>

    @DELETE("service-types/{id}")
    suspend fun deleteServiceType(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Void>>

    // ── Sub Service Types ─────────────────────────────────────────────────────
    @GET("sub-service-types/by-type/{typeId}")
    suspend fun getSubServiceTypes(
        @Header("Authorization") auth: String,
        @Path("typeId") typeId: Int
    ): Response<ApiResponse<List<SubServiceTypeDTO>>>

    @POST("sub-service-types")
    suspend fun createSubServiceType(
        @Header("Authorization") auth: String,
        @Body body: SubServiceTypeDTO
    ): Response<ApiResponse<SubServiceTypeDTO>>

    @PUT("sub-service-types/{id}")
    suspend fun updateSubServiceType(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: SubServiceTypeDTO
    ): Response<ApiResponse<SubServiceTypeDTO>>

    @DELETE("sub-service-types/{id}")
    suspend fun deleteSubServiceType(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Void>>

    // ── Service Items ─────────────────────────────────────────────────────────
    @GET("services")
    suspend fun getAllServiceItems(@Header("Authorization") auth: String): Response<ApiResponse<List<ServiceItemDTO>>>

    @GET("services/active")
    suspend fun getActiveServiceItems(@Header("Authorization") auth: String): Response<ApiResponse<List<ServiceItemDTO>>>

    @POST("services")
    suspend fun createServiceItem(
        @Header("Authorization") auth: String,
        @Body body: ServiceItemDTO
    ): Response<ApiResponse<ServiceItemDTO>>

    @PUT("services/{id}")
    suspend fun updateServiceItem(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ServiceItemDTO
    ): Response<ApiResponse<ServiceItemDTO>>

    @DELETE("services/{id}")
    suspend fun deleteServiceItem(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Void>>

    // ── Shelf Locations ───────────────────────────────────────────────────────
    @GET("shelf-locations")
    suspend fun getShelfLocations(@Header("Authorization") auth: String): Response<ApiResponse<List<ShelfLocationDTO>>>

    @GET("shelf-locations/active")
    suspend fun getActiveShelfLocations(@Header("Authorization") auth: String): Response<ApiResponse<List<ShelfLocationDTO>>>

    @POST("shelf-locations")
    suspend fun createShelfLocation(
        @Header("Authorization") auth: String,
        @Body body: ShelfLocationDTO
    ): Response<ApiResponse<ShelfLocationDTO>>

    @PUT("shelf-locations/{id}")
    suspend fun updateShelfLocation(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ShelfLocationDTO
    ): Response<ApiResponse<ShelfLocationDTO>>

    // ── Bookings ──────────────────────────────────────────────────────────────
    @GET("bookings")
    suspend fun getBookings(
        @Header("Authorization") auth: String,
        @Query("page")     page:     Int    = 0,
        @Query("size")     size:     Int    = 100,
        @Query("search")   search:   String = "",
        @Query("dateFrom") dateFrom: String = "",
        @Query("dateTo")   dateTo:   String = ""
    ): Response<ApiResponse<PagedResponse<BookingDTO>>>

    @GET("bookings/{id}")
    suspend fun getBookingById(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<BookingDTO>>

    @PATCH("bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Query("status") status: String
    ): Response<ApiResponse<BookingDTO>>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") auth: String,
        @Body body: BookingDTO
    ): Response<ApiResponse<BookingDTO>>

    @PUT("bookings/{id}")
    suspend fun updateBooking(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: BookingDTO
    ): Response<ApiResponse<BookingDTO>>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Void>>

    @POST("bookings/{id}/convert-to-job")
    suspend fun convertBookingToJob(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<List<ServiceJobDTO>>>

    // ── Service Jobs ──────────────────────────────────────────────────────────
    @GET("service-jobs")
    suspend fun getServiceJobs(
        @Header("Authorization") auth: String,
        @Query("page")     page:     Int    = 0,
        @Query("size")     size:     Int    = 100,
        @Query("search")   search:   String = "",
        @Query("dateFrom") dateFrom: String = "",
        @Query("dateTo")   dateTo:   String = ""
    ): Response<ApiResponse<PagedResponse<ServiceJobDTO>>>

    @POST("service-jobs")
    suspend fun createServiceJob(
        @Header("Authorization") auth: String,
        @Body body: ServiceJobDTO
    ): Response<ApiResponse<ServiceJobDTO>>

    @PUT("service-jobs/{id}")
    suspend fun updateServiceJob(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ServiceJobDTO
    ): Response<ApiResponse<ServiceJobDTO>>

    @DELETE("service-jobs/{id}")
    suspend fun deleteServiceJob(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Void>>

    @POST("service-jobs/{id}/rework")
    suspend fun createRework(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ReworkRequestDTO
    ): Response<ApiResponse<ServiceJobDTO>>

    @GET("service-jobs/{id}")
    suspend fun getServiceJobById(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<ServiceJobDTO>>

    @PATCH("service-jobs/{id}/status")
    suspend fun updateServiceJobStatus(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Query("status") status: String
    ): Response<ApiResponse<ServiceJobDTO>>

    @POST("service-jobs/{id}/settle")
    suspend fun settleServiceJob(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: SettleJobRequest
    ): Response<ApiResponse<ServiceJobDTO>>

    @POST("service-jobs/{id}/pay-due")
    suspend fun payServiceJobDue(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ServiceJobPayDueRequest
    ): Response<ApiResponse<ServiceJobDTO>>

    @GET("reports/staff")
    suspend fun getStaffReport(
        @Header("Authorization") auth: String,
        @Query("month") month: String
    ): Response<ApiResponse<List<StaffReportDTO>>>

    @GET("expenses")
    suspend fun getExpenses(@Header("Authorization") auth: String): Response<ApiResponse<List<ExpenseDTO>>>

    @POST("expenses")
    suspend fun createExpense(
        @Header("Authorization") auth: String,
        @Body body: ExpenseDTO
    ): Response<ApiResponse<ExpenseDTO>>

    @GET("incomes")
    suspend fun getIncomes(@Header("Authorization") auth: String): Response<ApiResponse<List<IncomeDTO>>>

    @POST("incomes")
    suspend fun createIncome(
        @Header("Authorization") auth: String,
        @Body body: IncomeDTO
    ): Response<ApiResponse<IncomeDTO>>

    @GET("chart-of-accounts")
    suspend fun getChartOfAccounts(@Header("Authorization") auth: String): Response<ApiResponse<List<ChartOfAccountDTO>>>

    @GET("audit-logs")
    suspend fun getAuditLogs(@Header("Authorization") auth: String): Response<ApiResponse<List<AuditLogDTO>>>

    @GET("reports/sales-ranking")
    suspend fun getSalesRanking(
        @Header("Authorization") auth: String,
        @Query("month") month: String
    ): Response<ApiResponse<List<SalesRankingDTO>>>

    @GET("chat/messages")
    suspend fun getChatMessages(@Header("Authorization") auth: String): Response<ApiResponse<List<ChatMessageDTO>>>

    @POST("chat/send")
    suspend fun sendMessage(
        @Header("Authorization") auth: String,
        @Body body: SendMessageRequest
    ): Response<ApiResponse<ChatMessageDTO>>

    @GET("products/{id}")
    suspend fun getProduct(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<ApiResponse<ProductDTO>>

    @GET("product-serials/by-product/{productId}")
    suspend fun getProductSerials(
        @Header("Authorization") auth: String,
        @Path("productId") productId: Int
    ): Response<ApiResponse<List<ProductSerialDTO>>>

    @GET("product-serials/by-serial/{serialNumber}")
    suspend fun findProductBySerial(
        @Header("Authorization") auth: String,
        @Path("serialNumber") serial: String
    ): Response<ApiResponse<ProductSerialDTO>>

    @PUT("product-serials/{id}")
    suspend fun updateProductSerial(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: ProductSerialDTO
    ): Response<ApiResponse<ProductSerialDTO>>

    // ── Print ─────────────────────────────────────────────────────────────────
    @POST("print/preview")
    suspend fun getBookingPrintHtml(
        @Header("Authorization") auth: String,
        @Body body: PrintPreviewRequest
    ): Response<ResponseBody>

    @GET("print/preview/service-job/{id}")
    suspend fun getServiceJobPrintHtml(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Query("paper") paper: String = "A4"
    ): Response<ResponseBody>

    @GET("print/preview/sale/{id}")
    suspend fun getSalePrintHtml(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Query("paper") paper: String = "A4"
    ): Response<ResponseBody>

    // ── Income & Profit Reports ───────────────────────────────────────────────
    @GET("reports/daily-summary")
    suspend fun getPeriodSummary(
        @Header("Authorization") auth: String,
        @Query("from") from: String,
        @Query("to")   to:   String
    ): Response<ApiResponse<PeriodSummaryDTO>>

    @GET("reports/yearly-summary")
    suspend fun getYearlySummary(
        @Header("Authorization") auth: String,
        @Query("year") year: Int
    ): Response<ApiResponse<YearlySummaryDTO>>

    @GET("reports/profit-loss")
    suspend fun getProfitLoss(
        @Header("Authorization") auth: String,
        @Query("from") from: String,
        @Query("to")   to:   String
    ): Response<ApiResponse<ProfitLossReportDTO>>

    @PUT("products/{id}/photo")
    suspend fun updateProductPhoto(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Void>>
}
