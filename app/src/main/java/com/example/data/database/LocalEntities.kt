package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_tables")
data class LocalTableEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tableNumber: String,
    val status: String,
    val assignedCaptainId: String? = null,
    val activeOrderId: String? = null,
    val restaurantCode: String
)

@Entity(tableName = "local_menu_items")
data class LocalMenuItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val restaurantCode: String
)

@Entity(tableName = "local_categories")
data class LocalCategoryEntity(
    @PrimaryKey val name: String,
    val restaurantCode: String
)

@Entity(tableName = "local_orders")
data class LocalOrderEntity(
    @PrimaryKey val id: String,
    val tableId: String,
    val itemsJson: String, // JSON-encoded List<OrderItem>
    val status: String, // "PENDING", "ACTIVE", "COMPLETED"
    val captainId: String,
    val totalAmount: Double,
    val createdAt: Long,
    val isSynced: Boolean = false
)

@Entity(tableName = "local_kots")
data class LocalKotEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val tableId: String,
    val tableName: String,
    val itemsJson: String, // JSON-encoded List<OrderItem>
    val status: String, // "PENDING", "PREPARING", "SERVED", "CANCELLED"
    val createdAt: Long,
    val isSynced: Boolean = false
)
