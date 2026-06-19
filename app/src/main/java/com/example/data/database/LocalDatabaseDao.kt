package com.example.data.database

import androidx.room.*

@Dao
interface LocalDatabaseDao {

    // Tables
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<LocalTableEntity>)

    @Query("SELECT * FROM local_tables WHERE restaurantCode = :restaurantCode")
    suspend fun getTables(restaurantCode: String): List<LocalTableEntity>

    @Query("DELETE FROM local_tables")
    suspend fun clearTables()

    // Menu Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<LocalMenuItemEntity>)

    @Query("SELECT * FROM local_menu_items WHERE restaurantCode = :restaurantCode")
    suspend fun getMenuItems(restaurantCode: String): List<LocalMenuItemEntity>

    @Query("DELETE FROM local_menu_items")
    suspend fun clearMenuItems()

    // Categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<LocalCategoryEntity>)

    @Query("SELECT * FROM local_categories WHERE restaurantCode = :restaurantCode")
    suspend fun getCategories(restaurantCode: String): List<LocalCategoryEntity>

    @Query("DELETE FROM local_categories")
    suspend fun clearCategories()

    // Orders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: LocalOrderEntity)

    @Query("SELECT * FROM local_orders")
    suspend fun getAllOrders(): List<LocalOrderEntity>

    @Query("SELECT * FROM local_orders WHERE id = :id")
    suspend fun getOrderById(id: String): LocalOrderEntity?

    @Query("SELECT * FROM local_orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<LocalOrderEntity>

    @Query("DELETE FROM local_orders")
    suspend fun clearOrders()

    // KOTs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKot(kot: LocalKotEntity)

    @Query("SELECT * FROM local_kots")
    suspend fun getAllKots(): List<LocalKotEntity>

    @Query("SELECT * FROM local_kots WHERE id = :id")
    suspend fun getKotById(id: String): LocalKotEntity?

    @Query("SELECT * FROM local_kots WHERE isSynced = 0")
    suspend fun getUnsyncedKots(): List<LocalKotEntity>

    @Query("DELETE FROM local_kots")
    suspend fun clearKots()
}
