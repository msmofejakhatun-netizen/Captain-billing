package com.example.data.remote

import com.example.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("/api/tables")
    suspend fun getTables(): Response<TablesResponse>

    @POST("/api/tables/open")
    suspend fun openTable(
        @Body request: TableOpenRequest
    ): Response<RestaurantTable>

    @GET("/api/menu-items")
    suspend fun getMenuItems(): Response<okhttp3.ResponseBody>

    @GET("/api/categories")
    suspend fun getCategories(): Response<okhttp3.ResponseBody>

    @POST("/api/orders/add-item")
    suspend fun addItemToOrder(
        @Body request: AddItemRequest
    ): Response<okhttp3.ResponseBody>

    @POST("/api/orders/update-item")
    suspend fun updateOrderItem(
        @Body request: UpdateItemRequest
    ): Response<okhttp3.ResponseBody>

    @POST("/api/orders/remove-item")
    suspend fun removeOrderItem(
        @Body request: RemoveItemRequest
    ): Response<okhttp3.ResponseBody>

    @POST("/api/kot/send")
    suspend fun sendKOT(
        @Body request: SendKOTRequest
    ): Response<KOT>

    @POST("/api/bills/generate")
    suspend fun generateBill(
        @Body request: GenerateBillRequest
    ): Response<Bill>

    @POST("/api/bills/settle")
    suspend fun settleBill(
        @Body request: SettleBillRequest
    ): Response<PaymentSettlement>

    @GET("/api/orders")
    suspend fun getOrders(): Response<List<Order>>

    // --- Admin POS Dashboard CRUD Endpoints ---

    // Categories CRUD
    @POST("/api/categories")
    suspend fun createCategory(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @PUT("/api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @DELETE("/api/categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: String
    ): Response<Void>

    // Menu Items CRUD
    @POST("/api/menu-items")
    suspend fun createMenuItem(
        @Body item: MenuItemDto
    ): Response<MenuItemDto>

    @PUT("/api/menu-items/{id}")
    suspend fun updateMenuItem(
        @Path("id") id: String,
        @Body item: MenuItemDto
    ): Response<MenuItemDto>

    @DELETE("/api/menu-items/{id}")
    suspend fun deleteMenuItem(
        @Path("id") id: String
    ): Response<Void>

    // Tables CRUD
    @POST("/api/tables")
    suspend fun createTable(
        @Body table: RestaurantTable
    ): Response<RestaurantTable>

    @PUT("/api/tables/{id}")
    suspend fun updateTable(
        @Path("id") id: String,
        @Body table: RestaurantTable
    ): Response<RestaurantTable>

    @DELETE("/api/tables/{id}")
    suspend fun deleteTable(
        @Path("id") id: String
    ): Response<Void>

    // Users / Captains CRUD
    @GET("/api/users")
    suspend fun getUsers(): Response<okhttp3.ResponseBody>

    @POST("/api/users")
    suspend fun createUser(
        @Body request: Map<String, String>
    ): Response<UserDto>

    @PUT("/api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<UserDto>

    @DELETE("/api/users/{id}")
    suspend fun deleteUser(
        @Path("id") id: String
    ): Response<Void>

    // Audit Logs
    @GET("/api/audit-logs")
    suspend fun getAuditLogs(): Response<List<AuditLog>>

    // Restaurant Settings
    @GET("/api/restaurant/settings")
    suspend fun getSettings(): Response<RestaurantSettings>

    @POST("/api/restaurant/settings")
    suspend fun updateSettings(
        @Body settings: RestaurantSettings
    ): Response<RestaurantSettings>

    // Reports Endpoints
    @GET("/api/reports/daily")
    suspend fun getDailySalesReport(): Response<List<DailySalesReport>>

    @GET("/api/reports/captain-wise")
    suspend fun getCaptainSalesReport(): Response<List<CaptainSalesReport>>

    @GET("/api/reports/item-wise")
    suspend fun getItemSalesReport(): Response<List<ItemSalesReport>>

    @GET("/api/reports/hourly")
    suspend fun getHourlySalesReport(): Response<List<HourlySalesReport>>

    @GET("/api/reports/settlement")
    suspend fun getSettlementReport(): Response<List<SettlementReport>>

    @GET("/api/reports/cash-closing")
    suspend fun getCashClosingReport(): Response<List<CashClosingReport>>
}
