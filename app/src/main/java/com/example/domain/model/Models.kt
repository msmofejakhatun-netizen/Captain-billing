package com.example.domain.model

data class User(
    val id: String,
    val username: String,
    val restaurantCode: String,
    val role: String,
    val token: String? = null
)

enum class TableStatus {
    AVAILABLE, OPEN, RUNNING, BILLED
}

data class RestaurantTable(
    val id: String,
    val tableNumber: String,
    val name: String,
    val status: TableStatus,
    val assignedCaptainId: String? = null,
    val activeOrderId: String? = null
)

data class MenuItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true
)

data class OrderItem(
    val id: String,
    val menuItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int
) {
    val total: Double get() = price * quantity
}

data class Order(
    val id: String,
    val tableId: String,
    val items: List<OrderItem> = emptyList(),
    val status: String, // "PENDING", "ACTIVE", "COMPLETED"
    val captainId: String,
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class KOT(
    val id: String,
    val orderId: String,
    val tableId: String,
    val tableName: String,
    val items: List<OrderItem>,
    val status: String, // "PENDING", "PREPARING", "SERVED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis()
)

data class Bill(
    val id: String,
    val orderId: String,
    val tableId: String,
    val tableName: String,
    val totalAmount: Double,
    val taxAmount: Double,
    val serviceCharge: Double,
    val grandTotal: Double,
    val status: String, // "UNPAID", "PAID"
    val createdAt: Long = System.currentTimeMillis(),
    
    // Billing Enhancements
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountReason: String? = null,
    val couponCode: String? = null
) {
    val invoiceNumber: String
        get() {
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = createdAt }
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            // Financial year starts in April (month == 3)
            val fy = if (month >= 3) year else year - 1
            val digits = id.filter { it.isDigit() }
            val seq = if (digits.isNotEmpty()) {
                digits.takeLast(6).padStart(6, '0')
            } else {
                val numericHash = Math.abs(id.hashCode() % 100000)
                numericHash.toString().padStart(6, '0')
            }
            return "INV-$fy-$seq"
        }
}

data class PaymentSettlement(
    val id: String,
    val billId: String,
    val cashAmount: Double,
    val cardAmount: Double,
    val upiAmount: Double,
    val paymentType: String, // "CASH", "CARD", "UPI", "MIXED"
    val status: String, // "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis()
)

data class AuditLog(
    val id: String,
    val userId: String,
    val username: String,
    val action: String, // "CREATE_TABLE", "DELETE_MENU_ITEM", etc.
    val details: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class RestaurantSettings(
    val id: String,
    val restaurantName: String,
    val taxPercentage: Double,
    val serviceChargePercentage: Double,
    val currency: String = "USD",
    val logoUrl: String? = null,
    val gstNumber: String? = null,
    val fssaiNumber: String? = null,
    val address: String? = null,
    val phoneNumber: String? = null,
    val footerMessage: String? = null,
    val thankYouMessage: String? = null
)

data class DailySalesReport(
    val date: String,
    val totalOrders: Int,
    val grossSales: Double,
    val netSales: Double,
    val taxCollected: Double,
    val serviceChargeCollected: Double
)

data class CaptainSalesReport(
    val captainId: String,
    val captainName: String,
    val totalOrders: Int,
    val totalSales: Double
)

data class ItemSalesReport(
    val menuItemId: String,
    val itemName: String,
    val quantitySold: Int,
    val category: String,
    val totalRevenue: Double
)

data class HourlySalesReport(
    val hour: Int,
    val totalOrders: Int,
    val totalSales: Double
)

data class SettlementReport(
    val paymentMethod: String,
    val count: Int,
    val totalAmount: Double
)

data class CashClosingReport(
    val date: String,
    val openingCash: Double,
    val totalCashSales: Double,
    val closingCash: Double,
    val difference: Double,
    val remarks: String
)

