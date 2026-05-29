package com.sspd.servicemgmt.api

// ─── Generic wrappers ────────────────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null
)

data class PagedResponse<T>(
    val content: List<T> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0
)

// ─── Auth ────────────────────────────────────────────────────────────────────

data class LoginRequest(val usernameOremail: String, val password: String)

data class AuthResponse(
    val accessToken: String,
    val username: String,
    val name: String? = null,
    val phone: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)

// ─── Dashboard ───────────────────────────────────────────────────────────────

data class DashboardStats(
    val todaySalesAmount: Long? = null,
    val todaySalesCount: Long? = null,
    val lowStockCount: Long? = null,
    val pendingServiceJobs: Long? = null,
    val totalOverdueAR: Long? = null,
    val overdueARCount: Long? = null,
    val totalPendingAR: Long? = null,
    val pendingARCount: Long? = null,
    val lowStockProducts: List<String>? = null,
    val recentSales: List<RecentSaleDTO>? = null
)

data class RecentSaleDTO(
    val id: Int? = null,
    val saleCode: String? = null,
    val customerName: String? = null,
    val amount: Long? = null,
    val date: String? = null,
    val status: String? = null
)

// ─── Products / Stock ────────────────────────────────────────────────────────

data class ProductDTO(
    val id: Int = 0,
    val productCode: String = "",
    val name: String = "",
    val stockQty: Int = 0,
    val availableSerialCount: Int? = null,
    val productType: String = "",
    val sellingPrice: Long = 0,
    val costPrice: Long? = null,
    val categoryName: String? = null,
    val brandName: String? = null,
    val unitName: String? = null,
    val reorderLevel: Int? = null,
    val shortageQty: Int? = null,
    val hasSerial: Boolean? = null,
    val warrantyMonths: Int? = null,
    val warrantyTerms: String? = null,
    val remark: String? = null,
    val photoBase64: String? = null
)

data class ProductSerialDTO(
    val id: Int? = null,
    val serialNumber: String = "",
    val status: String? = null,
    val productId: Int? = null,
    val productName: String? = null,
    val warrantyMonths: Int? = null,
    val warrantyStartDate: String? = null,
    val warrantyEndDate: String? = null,
    val condition: String? = null,
    val photoBase64: String? = null
)

// ─── Sales ───────────────────────────────────────────────────────────────────

data class PaymentTransactionDTO(
    val id: Int? = null,
    val referenceId: Int? = null,
    val referenceType: String? = null,
    val paymentMethodId: Int? = null,
    val paymentMethodName: String? = null,
    val amount: Double? = null,
    val transactionNo: String? = null,
    val paymentDate: String? = null,
    val referenceCode: String? = null,
    val entityName: String? = null
)

data class SalePaymentRequest(
    val paidAmount:      Double,
    val paymentMethodId: Int,
    val note:            String? = null,
    val staffId:         Int?    = null
)

data class SaleItemDTO(
    val productId: Int? = null,
    val productName: String? = null,
    val qty: Int? = null,
    val unitPrice: Double? = null,
    val subtotal: Double? = null,
    val discountAmount: Double? = null,
    val foc: Boolean? = null,
    val warrantyMonths: Int? = null,
    val warrantyExpiryDate: String? = null,
    val serialNumbers: List<String>? = null
)

data class CustomerCreditTermDTO(
    val id: Int? = null,
    val customerId: Int? = null,
    val creditLimit: Double? = null,
    val creditDays: Int? = null,
    val creditAllowed: Boolean? = null,
    val customerName: String? = null
)

data class PaymentMethodDTO(
    val id: Int = 0,
    val methodName: String = "",
    val active: Boolean = true
)

data class SaleDTO(
    val id: Int? = null,
    val saleCode: String? = null,
    val customerId: Int? = null,
    val customerName: String? = null,
    val staffId: Int? = null,
    val staffName: String? = null,
    val saleDate: String? = null,
    val totalAmount: Double? = null,
    val discountAmount: Double? = null,
    val netAmount: Double? = null,
    val paidAmount: Double? = null,
    val dueAmount: Double? = null,
    val paymentStatus: String? = null,
    val paymentMethodId: Int? = null,
    val paymentMethodName: String? = null,
    val dueDate: String? = null,
    val remark: String? = null,
    val details: List<SaleItemDTO>? = null
)

// ─── Bookings ────────────────────────────────────────────────────────────────

data class BookingDTO(
    val id: Int? = null,
    val invoiceNo: String? = null,
    val customerId: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val staffName: String? = null,
    val paymentMethodName: String? = null,
    val bookingDate: String? = null,
    val appointmentDate: String? = null,
    val status: String? = null,
    val totalAmount: Double? = null,
    val brand: String? = null,
    val model: String? = null,
    val deviceType: String? = null,
    val serialNumber: String? = null,
    val color: String? = null,
    val accessories: String? = null,
    val problemDesc: String? = null,
    val shelfLocation: String? = null,
    val remark: String? = null,
    val devices: List<BookingDeviceDTO>? = null,
    val details: List<BookingDetailItemDTO>? = null
)

// ─── Service Jobs ────────────────────────────────────────────────────────────

data class ServiceTypeDTO(
    val id: Int? = null,
    val name: String = "",
    val description: String? = null,
    val isActive: Boolean = true
)

data class SubServiceTypeDTO(
    val id: Int? = null,
    val name: String = "",
    val description: String? = null,
    val isActive: Boolean = true,
    val serviceTypeId: Int? = null,
    val serviceTypeName: String? = null
)

data class ServiceItemDTO(
    val id: Int? = null,
    val code: String? = null,
    val item: String = "",
    val price: Double = 0.0,
    val isActive: Boolean = true,
    val serviceTypeId: Int? = null,
    val serviceTypeName: String? = null,
    val subServiceTypeId: Int? = null,
    val subServiceTypeName: String? = null
)

data class ShelfLocationDTO(
    val id: Int? = null,
    val code: String = "",
    val label: String? = null,
    val active: Boolean = true
)

data class BookingDeviceDTO(
    val id: Int? = null,
    val deviceType: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val color: String? = null,
    val accessories: String? = null,
    val problemDesc: String? = null,
    val deviceConditions: String? = null
)

data class BookingDetailItemDTO(
    val id: Int? = null,
    val serviceId: Int? = null,
    val serviceName: String? = null,
    val qty: Int? = null,
    val price: Double? = null,
    val subtotal: Double? = null
)

data class ServiceJobLineDTO(
    val id: Int? = null,
    val serviceItemId: Int? = null,
    val serviceItemName: String? = null,
    val qty: Int? = null,
    val price: Double? = null,
    val subtotal: Double? = null,
    val warrantyMonths: Int? = null
)

data class ServiceJobPartDTO(
    val id: Int? = null,
    val productId: Int? = null,
    val productName: String? = null,
    val productCode: String? = null,
    val qty: Int? = null,
    val unitPrice: Double? = null,
    val discountAmount: Double? = null,
    val subtotal: Double? = null,
    val serialNumbers: List<String>? = null
)

data class SettleJobRequest(
    val finalCost:        Double,
    val discountAmount:   Double  = 0.0,
    val foc:              Boolean = false,
    val paidAmount:       Double,
    val staffId:          Int?    = null,
    val paymentMethodId:  Int?    = null,
    val transactionNo:    String? = null,
    val dueDate:          String? = null
)

data class ServiceJobPayDueRequest(
    val paidAmount:      Double,
    val paymentMethodId: Int,
    val transactionNo:   String? = null,
    val note:            String? = null
)

data class ReworkRequestDTO(
    val reworkType:      String,
    val problemDesc:     String? = null,
    val assignedStaffId: Int?   = null
)

data class ServiceJobDTO(
    val id: Int? = null,
    val jobNo: String? = null,
    val customerId: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val assignedStaffId: Int? = null,
    val assignedStaffName: String? = null,
    val itemName: String? = null,
    val itemCondition: String? = null,
    val deviceConditions: String? = null,
    val problemDesc: String? = null,
    val diagnosisNotes: String? = null,
    val accessories: String? = null,
    val estimatedCost: Double? = null,
    val finalCost: Double? = null,
    val discountAmount: Double? = null,
    val foc: Boolean? = null,
    val status: String? = null,
    val netAmount: Double? = null,
    val paidAmount: Double? = null,
    val dueAmount: Double? = null,
    val dueDate: String? = null,
    val paymentStatus: String? = null,
    val paymentMethodId: Int? = null,
    val paymentMethodName: String? = null,
    val receivedDate: String? = null,
    val estimatedCompletion: String? = null,
    val completedDate: String? = null,
    val deliveredDate: String? = null,
    val rework: Boolean? = null,
    val bookingId: Int? = null,
    val bookingNo: String? = null,
    val serialNo: String? = null,
    val color: String? = null,
    val remark: String? = null,
    val lines: List<ServiceJobLineDTO>? = null,
    val productParts: List<ServiceJobPartDTO>? = null
)

// ─── Staff ───────────────────────────────────────────────────────────────────

data class StaffDTO(
    val id: Int = 0,
    val name: String = "",
    val phone: String? = null,
    val role: String = "",
    val isActive: Boolean = true,
    val basicSalary: Long? = null
)

data class StaffReportDTO(
    val staffId: Int = 0,
    val staffName: String = "",
    val staffRole: String = "",
    val salesCount: Int = 0,
    val salesAmount: Long = 0,
    val serviceJobsCount: Int = 0,
    val completedJobsCount: Int = 0,
    val cancelledJobsCount: Int = 0,
    val reworkJobsCount: Int = 0,
    val inProgressJobsCount: Int = 0,
    val serviceJobsAmount: Long = 0,
    val completionRate: Double = 0.0
)

// ─── Customers ───────────────────────────────────────────────────────────────

data class CustomerDTO(
    val id: Int? = null,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val creditHold: Boolean = false,
    val blacklisted: Boolean = false,
    val creditHoldReason: String? = null,
    val blacklistReason: String? = null
)

// ─── Suppliers ───────────────────────────────────────────────────────────────

data class SupplierDTO(
    val id: Int = 0,
    val code: String? = null,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val openingBalance: Long? = null,
    val currentBalance: Long? = null
)

// ─── Purchases ───────────────────────────────────────────────────────────────

data class PurchaseDTO(
    val id: Int? = null,
    val purchaseCode: String? = null,
    val supplierName: String? = null,
    val staffName: String? = null,
    val purchaseDate: String? = null,
    val totalAmount: Long? = null,
    val paidAmount: Long? = null,
    val dueAmount: Long? = null,
    val paymentStatus: String? = null,
    val remark: String? = null
)

// ─── Expenses / Income ───────────────────────────────────────────────────────

data class ExpenseDTO(
    val id: Int? = null,
    val expenseCode: String? = null,
    val expenseDate: String? = null,
    val accountName: String? = null,
    val paymentMethodName: String? = null,
    val amount: Long = 0,
    val description: String? = null,
    val staffName: String? = null
)

data class IncomeDTO(
    val id: Int? = null,
    val incomeCode: String? = null,
    val incomeDate: String? = null,
    val accountName: String? = null,
    val paymentMethodName: String? = null,
    val amount: Long = 0,
    val description: String? = null,
    val staffName: String? = null
)

// ─── Reports ─────────────────────────────────────────────────────────────────

data class SalesRankingDTO(
    val staffId: Int? = null,
    val staffName: String? = null,
    val salesCount: Int? = null,
    val totalAmount: Long? = null,
    val rank: Int? = null
)

// ─── Audit Log ───────────────────────────────────────────────────────────────

data class AuditLogDTO(
    val id: Long? = null,
    val actor: String? = null,
    val actorRole: String? = null,
    val action: String? = null,
    val module: String? = null,
    val resourceId: String? = null,
    val description: String? = null,
    val ipAddress: String? = null,
    val deviceType: String? = null,
    val createdAt: String? = null
)

// ─── Print ───────────────────────────────────────────────────────────────────

data class PrintPreviewRequest(
    val documentType: String,
    val documentId:   Int,
    val paperSize:    String = "A4"
)

// ─── Chat ────────────────────────────────────────────────────────────────────

data class ChatMessageDTO(
    val id: Long? = null,
    val senderUsername: String = "",
    val senderName: String? = null,
    val senderRole: String? = null,
    val content: String = "",
    val sentAt: String? = null
)

data class SendMessageRequest(val content: String)

// ─── Users (RBAC) ────────────────────────────────────────────────────────────

data class UserDTO(
    val id: Long = 0,
    val username: String = "",
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
    val roles: List<String> = emptyList()
)
