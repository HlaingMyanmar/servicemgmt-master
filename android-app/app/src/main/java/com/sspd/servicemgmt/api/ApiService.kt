package com.sspd.servicemgmt.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<AuthResponse>>

    @GET("dashboard/stats")
    suspend fun getStats(@Header("Authorization") auth: String): Response<ApiResponse<DashboardStats>>

    @GET("products")
    suspend fun getProducts(@Header("Authorization") auth: String): Response<ApiResponse<List<ProductDTO>>>

    @GET("sales")
    suspend fun getSales(@Header("Authorization") auth: String): Response<ApiResponse<List<SaleDTO>>>

    @GET("bookings")
    suspend fun getBookings(@Header("Authorization") auth: String): Response<ApiResponse<List<BookingDTO>>>

    @GET("service-jobs")
    suspend fun getServiceJobs(@Header("Authorization") auth: String): Response<ApiResponse<List<ServiceJobDTO>>>

    @GET("reports/staff")
    suspend fun getStaffReport(
        @Header("Authorization") auth: String,
        @Query("month") month: String
    ): Response<ApiResponse<List<StaffReportDTO>>>

    @GET("expenses")
    suspend fun getExpenses(@Header("Authorization") auth: String): Response<ApiResponse<List<ExpenseDTO>>>

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

    @GET("product-serials/by-serial/{serialNumber}")
    suspend fun findProductBySerial(
        @Header("Authorization") auth: String,
        @Path("serialNumber") serial: String
    ): Response<ApiResponse<ProductSerialDTO>>
}
