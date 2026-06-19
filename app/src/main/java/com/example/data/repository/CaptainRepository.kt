package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.database.LocalTableEntity
import com.example.data.database.LocalMenuItemEntity
import com.example.data.database.LocalCategoryEntity
import com.example.data.database.LocalOrderEntity
import com.example.data.database.LocalKotEntity
import com.example.data.remote.*
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.random.Random

data class OpenTableDebugState(
    val lastUrl: String?,
    val lastStatusCode: Int?,
    val lastResponse: String?,
    val lastError: String?
)

interface CaptainRepository {
    val tableEventFlow: SharedFlow<RestaurantTable>
    val tablesMasterEventFlow: SharedFlow<List<RestaurantTable>>
    val orderEventFlow: SharedFlow<Order>
    val kotEventFlow: SharedFlow<KOT>
    val billEventFlow: SharedFlow<Bill>
    val paymentEventFlow: SharedFlow<PaymentSettlement>
    val menuEventFlow: SharedFlow<Unit>
    val isOnline: StateFlow<Boolean>
    val loginApiLog: StateFlow<String?>
    val openTableDebugLog: StateFlow<OpenTableDebugState?>

    val printerManager: com.example.data.printer.EpsonPrinterManager

    // Authentication
    suspend fun login(restaurantCode: String, username: String, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun autoLogin(): Boolean
    suspend fun logout()

    // Tables
    suspend fun getTables(refresh: Boolean = false): Result<List<RestaurantTable>>
    suspend fun openTable(tableNumber: String): Result<RestaurantTable>

    // Menu
    suspend fun getMenuItems(refresh: Boolean = false): Result<List<MenuItem>>
    suspend fun getCategories(refresh: Boolean = false): Result<List<String>>

    // Cart / Order
    suspend fun getActiveOrder(tableId: String): Order?
    suspend fun addItemToOrder(tableId: String, menuItemId: String, quantity: Int): Result<Order>
    suspend fun updateOrderItem(tableId: String, itemId: String, quantity: Int): Result<Order>
    suspend fun removeOrderItem(tableId: String, itemId: String): Result<Order>

    // KOT
    suspend fun sendKOT(tableId: String): Result<KOT>
    suspend fun getKOTHistory(orderId: String): Result<List<KOT>>
    suspend fun getAllKOTs(): Result<List<KOT>>
    suspend fun printKOT(kotId: String, reprint: Boolean = false, reprintReason: String? = null): Result<Unit>

    // Billing & Settlements
    suspend fun generateBill(tableId: String): Result<Bill>
    suspend fun getBill(tableId: String): Bill?
    suspend fun settleBill(billId: String, cash: Double, card: Double, upi: Double, paymentType: String): Result<PaymentSettlement>
    suspend fun printBill(billId: String, tableId: String): Result<Unit>

    // Phase 6 Production Hardening Additions
    suspend fun updateKOTStatus(kotId: String, status: String): Result<KOT>
    suspend fun transferTable(fromTableId: String, toTableId: String): Result<Unit>
    suspend fun mergeTables(fromTableId: String, toTableId: String): Result<Unit>
    suspend fun splitTable(tableId: String, targetTableId: String, itemIdsToMove: List<String>): Result<Unit>
    suspend fun changeCaptain(tableId: String, captainId: String, captainName: String): Result<Unit>
    suspend fun holdTableOrder(tableId: String, hold: Boolean): Result<Unit>
    fun isTableHeld(tableId: String): Boolean
    
    // Billing Discount
    suspend fun applyDiscountToBill(billId: String, percent: Double, amount: Double, reason: String): Result<Bill>

    // History
    suspend fun getOrderHistory(): Result<List<Order>>
    suspend fun getSettledPayments(): Result<List<PaymentSettlement>>

    // --- Admin CRUD Operations ---
    // Categories CRUD
    suspend fun createCategory(name: String): Result<Unit>
    suspend fun updateCategory(oldName: String, newName: String): Result<Unit>
    suspend fun deleteCategory(name: String): Result<Unit>

    // Menu Items CRUD
    suspend fun createMenuItem(item: MenuItem): Result<MenuItem>
    suspend fun updateMenuItem(item: MenuItem): Result<MenuItem>
    suspend fun deleteMenuItem(id: String): Result<Unit>

    // Tables CRUD
    suspend fun createTable(table: RestaurantTable): Result<RestaurantTable>
    suspend fun updateTable(table: RestaurantTable): Result<RestaurantTable>
    suspend fun deleteTable(id: String): Result<Unit>

    // Users / Captains CRUD
    suspend fun getUsers(): Result<List<UserDto>>
    suspend fun createUser(username: String, role: String, restaurantCode: String, pss: String): Result<UserDto>
    suspend fun updateUser(id: String, username: String, role: String, restaurantCode: String, pss: String): Result<UserDto>
    suspend fun deleteUser(id: String): Result<Unit>

    // Audit Logs
    suspend fun getAuditLogs(): Result<List<AuditLog>>

    // Restaurant Settings
    suspend fun getSettings(): Result<RestaurantSettings>
    suspend fun updateSettings(settings: RestaurantSettings): Result<RestaurantSettings>

    // Reports
    suspend fun getDailySalesReport(): Result<List<DailySalesReport>>
    suspend fun getCaptainSalesReport(): Result<List<CaptainSalesReport>>
    suspend fun getItemSalesReport(): Result<List<ItemSalesReport>>
    suspend fun getHourlySalesReport(): Result<List<HourlySalesReport>>
    suspend fun getSettlementReport(): Result<List<SettlementReport>>
    suspend fun getCashClosingReport(): Result<List<CashClosingReport>>
}

class CaptainRepositoryImpl(
    private val apiService: ApiService,
    private val realtimeClient: SupabaseRealtimeClient,
    private val localDatabaseDao: com.example.data.database.LocalDatabaseDao,
    private val context: android.content.Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CaptainRepository {

    override val printerManager = com.example.data.printer.EpsonPrinterManager(context)

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // Real-time Event Channels
    override val tableEventFlow = MutableSharedFlow<RestaurantTable>(replay = 1, extraBufferCapacity = 64)
    override val tablesMasterEventFlow = MutableSharedFlow<List<RestaurantTable>>(replay = 1, extraBufferCapacity = 64)
    override val orderEventFlow = MutableSharedFlow<Order>(replay = 1, extraBufferCapacity = 64)
    override val kotEventFlow = MutableSharedFlow<KOT>(replay = 1, extraBufferCapacity = 64)
    override val billEventFlow = MutableSharedFlow<Bill>(replay = 1, extraBufferCapacity = 64)
    override val paymentEventFlow = MutableSharedFlow<PaymentSettlement>(replay = 1, extraBufferCapacity = 64)
    override val menuEventFlow = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)
    private val _isOnline = MutableStateFlow(true)
    override val isOnline = _isOnline.asStateFlow()
    private val _loginApiLog = MutableStateFlow<String?>(null)
    override val loginApiLog = _loginApiLog.asStateFlow()
    private val _openTableDebugLog = MutableStateFlow<OpenTableDebugState?>(null)
    override val openTableDebugLog = _openTableDebugLog.asStateFlow()
    
    private val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    private var currentUser: User? = null
    private val tables = mutableListOf<RestaurantTable>()
    private val menuItems = mutableListOf<MenuItem>()
    private val orders = mutableListOf<Order>()
    private val kots = mutableListOf<KOT>()
    private val bills = mutableListOf<Bill>()
    private val settlements = mutableListOf<PaymentSettlement>()
    private val heldTableIds = mutableSetOf<String>()

    private val moshiLocal = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val orderItemListType = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val orderItemsAdapter = moshiLocal.adapter<List<OrderItem>>(orderItemListType)

    private fun RestaurantTable.toLocal(restaurantCode: String) = LocalTableEntity(
        id = id,
        name = name,
        tableNumber = tableNumber,
        status = status.name,
        assignedCaptainId = assignedCaptainId,
        activeOrderId = activeOrderId,
        restaurantCode = restaurantCode
    )

    private fun LocalTableEntity.toDomain() = RestaurantTable(
        id = id,
        name = name,
        tableNumber = tableNumber,
        status = TableStatus.valueOf(status),
        assignedCaptainId = assignedCaptainId,
        activeOrderId = activeOrderId
    )

    private fun MenuItem.toLocal(restaurantCode: String) = LocalMenuItemEntity(
        id = id,
        name = name,
        category = category,
        price = price,
        description = description,
        imageUrl = imageUrl,
        isAvailable = isAvailable,
        restaurantCode = restaurantCode
    )

    private fun LocalMenuItemEntity.toDomain() = MenuItem(
        id = id,
        name = name,
        category = category,
        price = price,
        description = description,
        imageUrl = imageUrl,
        isAvailable = isAvailable
    )

    private fun String.toLocalCategory(restaurantCode: String) = LocalCategoryEntity(
        name = this,
        restaurantCode = restaurantCode
    )

    private fun checkCurrentNetwork(ctx: android.content.Context): Boolean {
        return try {
            val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                val networkValue = cm.activeNetwork
                if (networkValue != null) {
                    val act = cm.getNetworkCapabilities(networkValue)
                    act != null && act.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun monitorNetwork(ctx: android.content.Context) {
        try {
            val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                _isOnline.value = checkCurrentNetwork(ctx)
                val request = android.net.NetworkRequest.Builder()
                    .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        _isOnline.value = true
                        Log.i("CaptainRepository", "Network is available (online)")
                    }
                    override fun onLost(network: android.net.Network) {
                        _isOnline.value = false
                        Log.i("CaptainRepository", "Network is lost (offline)")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed to register network callback", e)
        }
    }

    init {
        monitorNetwork(context)
    }

    override suspend fun login(restaurantCode: String, username: String, password: String): Result<User> = withContext(dispatcher) {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val requestBody = LoginRequest(restaurantCode, username, password)
        val reqJsonString = try {
            moshi.adapter(LoginRequest::class.java).toJson(requestBody)
        } catch (e: Exception) {
            "LoginRequest(restaurantCode=$restaurantCode, username=$username, password=***)"
        }

        try {
            val response = apiService.login(requestBody)
            val requestUrl = response.raw().request.url.toString()

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val responseBodyString = try {
                    moshi.adapter(LoginResponse::class.java).toJson(dto)
                } catch (e: Exception) {
                    dto.toString()
                }

                val roleUpper = dto.user.role.uppercase()
                val allowedRoles = listOf("CAPTAIN", "OWNER", "ADMIN", "BILLER", "KITCHEN")
                if (roleUpper !in allowedRoles) {
                    val errMsg = "Access denied. Unauthorized role: ${dto.user.role}"
                    Log.e("LOGIN_API", "--- LOGIN API CALL FAILURE (ROLE ACCESS DENIED) ---")
                    Log.e("LOGIN_API", "Request URL: $requestUrl")
                    Log.e("LOGIN_API", "Request Body: $reqJsonString")
                    Log.e("LOGIN_API", "Response HTTP Status Code: ${response.code()}")
                    Log.e("LOGIN_API", "Response Body: $responseBodyString")
                    Log.e("LOGIN_API", "Message: $errMsg")
                    Log.e("LOGIN_API", "--------------------------------")

                    _loginApiLog.value = """
                        [FAILURE - ROLE ACCESS DENIED]
                        Request URL: $requestUrl
                        HTTP Status Code: ${response.code()}
                        Response Body: $responseBodyString
                        Error Details: $errMsg
                    """.trimIndent()

                    return@withContext Result.failure(Exception(errMsg))
                }
                val resolvedCode = dto.restaurantCode ?: dto.user.restaurantCode ?: restaurantCode
                val user = User(
                    id = dto.user.id,
                    username = dto.user.username,
                    restaurantCode = resolvedCode,
                    role = dto.user.role,
                    token = dto.token
                )
                currentUser = user

                // Print the exact requested logs
                Log.i("LOGIN_API", "--- LOGIN API CALL SUCCESS ---")
                Log.i("LOGIN_API", "Request URL: $requestUrl")
                Log.i("LOGIN_API", "Request Body: $reqJsonString")
                Log.i("LOGIN_API", "Response HTTP Status Code: ${response.code()}")
                Log.i("LOGIN_API", "Response Body: $responseBodyString")
                Log.i("LOGIN_API", "Parsed DTO: $dto")
                Log.i("LOGIN_API", "Parsed User Session: $user")
                Log.i("LOGIN_API", "--------------------------------")

                _loginApiLog.value = """
                    [SUCCESS]
                    Request URL: $requestUrl
                    HTTP Status Code: ${response.code()}
                    Response Body: $responseBodyString
                    Parsed DTO: $dto
                    Parsed User Session: $user
                """.trimIndent()

                // Store securely in SharedPreferences
                val prefs = context.getSharedPreferences("captain_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("user_id", user.id)
                    putString("username", user.username)
                    putString("restaurant_code", user.restaurantCode)
                    putString("user_role", user.role)
                    putString("jwt_token", user.token)
                    apply()
                }

                // Connect to Supabase Realtime updates
                connectSupabaseRealtime(user)

                // Load tables, categories and menu items from backend
                try { getTables(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Sync tables error", e) }
                try { getCategories(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Sync categories error", e) }
                try { getMenuItems(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Sync menu items error", e) }

                return@withContext Result.success(user)
            } else {
                val errorBodyString = response.errorBody()?.string() ?: ""
                
                // Extract exact backend error message if present in JSON errorBody
                val parsedErrorMsg = if (errorBodyString.trim().isNotEmpty()) {
                    try {
                        val parsedMap = moshi.adapter(Map::class.java).fromJson(errorBodyString)
                        parsedMap?.let { map ->
                            (map["message"] as? String)
                                ?: (map["error"] as? String)
                                ?: (map["msg"] as? String)
                                ?: (map["detail"] as? String)
                        } ?: errorBodyString
                    } catch (e: Exception) {
                        errorBodyString
                    }
                } else {
                    "Authentication failed (Status code: ${response.code()})"
                }

                Log.e("LOGIN_API", "--- LOGIN API CALL FAILURE (STATUS NOT SUCCESSFUL) ---")
                Log.e("LOGIN_API", "Request URL: $requestUrl")
                Log.e("LOGIN_API", "Request Body: $reqJsonString")
                Log.e("LOGIN_API", "Response HTTP Status Code: ${response.code()}")
                Log.e("LOGIN_API", "Response Body (Error): $errorBodyString")
                Log.e("LOGIN_API", "Parsed Error Extracted: $parsedErrorMsg")
                Log.e("LOGIN_API", "--------------------------------")

                _loginApiLog.value = """
                    [FAILURE - STATUS ${response.code()}]
                    Request URL: $requestUrl
                    HTTP Status Code: ${response.code()}
                    Response Body: $errorBodyString
                    Parsed Error Message: $parsedErrorMsg
                """.trimIndent()

                return@withContext Result.failure(Exception(parsedErrorMsg))
            }
        } catch (e: Exception) {
            Log.e("LOGIN_API", "--- LOGIN API CALL EXCEPTION ---")
            Log.e("LOGIN_API", "Request Body: $reqJsonString")
            Log.e("LOGIN_API", "Exception occured: ${e.message}", e)
            Log.e("LOGIN_API", "--------------------------------")

            _loginApiLog.value = """
                [EXCEPTION]
                Request Body: $reqJsonString
                Exception Message: ${e.message}
                Stack Trace: ${Log.getStackTraceString(e)}
            """.trimIndent()

            return@withContext Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun autoLogin(): Boolean = withContext(dispatcher) {
        val prefs = context.getSharedPreferences("captain_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        if (!token.isNullOrEmpty()) {
            val id = prefs.getString("user_id", "") ?: ""
            val username = prefs.getString("username", "") ?: ""
            val rCode = prefs.getString("restaurant_code", "") ?: ""
            val role = prefs.getString("user_role", "") ?: ""
            
            val roleUpper = role.uppercase()
            val allowedRoles = listOf("CAPTAIN", "OWNER", "ADMIN", "BILLER", "KITCHEN")
            if (roleUpper !in allowedRoles) {
                prefs.edit().clear().apply()
                return@withContext false
            }

            val user = User(
                id = id,
                username = username,
                restaurantCode = rCode,
                role = role,
                token = token
            )
            currentUser = user
            connectSupabaseRealtime(user)
            
            // Sync current state on background/async or directly in startup
            try { getTables(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Auto sync tables error", e) }
            try { getCategories(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Auto sync categories error", e) }
            try { getMenuItems(refresh = true) } catch (e: Exception) { Log.e("CaptainRepository", "Auto sync menu error", e) }
            
            return@withContext true
        }
        return@withContext false
    }

    override suspend fun logout() = withContext(dispatcher) {
        val prefs = context.getSharedPreferences("captain_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        realtimeJob?.cancel()
        realtimeClient.disconnect()
        currentUser = null
    }

    private var realtimeJob: Job? = null

    private fun connectSupabaseRealtime(user: User) {
        val sUrl = BuildConfig.SUPABASE_URL.trim()
        val sKey = BuildConfig.SUPABASE_KEY.trim()
        if ((sUrl.startsWith("http://") || sUrl.startsWith("https://")) && 
            !sUrl.contains("SUPABASE_URL") && 
            !sUrl.contains("your-supabase-project") && 
            sKey.isNotEmpty() && 
            !sKey.contains("SUPABASE_KEY")) {
            Log.i("CaptainRepository", "Connecting to Supabase Realtime for restaurant: ${user.restaurantCode}")
            realtimeClient.connect(sUrl, sKey, user.restaurantCode)
            startObservingRealtimeEvents()
        } else {
            Log.w("CaptainRepository", "Supabase URL/Key empty or placeholder configured, Realtime not connected.")
        }
    }

    private fun startObservingRealtimeEvents() {
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            realtimeClient.realtimeEventFlow.collect { rawJson ->
                try {
                    Log.d("CaptainRepository", "Received realtime event: $rawJson")
                    if (rawJson.contains("\"table\":\"tables\"")) {
                        Log.i("CaptainRepository", "Realtime update: tables changed, refreshing...")
                        getTables(refresh = true)
                    } else if (rawJson.contains("\"table\":\"menu_items\"") || rawJson.contains("\"table\":\"menu\"")) {
                        Log.i("CaptainRepository", "Realtime update: menu items changed, refreshing...")
                        getMenuItems(refresh = true)
                        menuEventFlow.emit(Unit)
                    } else if (rawJson.contains("\"table\":\"categories\"") || rawJson.contains("\"table\":\"menu_categories\"")) {
                        Log.i("CaptainRepository", "Realtime update: categories changed, refreshing...")
                        getMenuItems(refresh = true)
                        menuEventFlow.emit(Unit)
                    } else if (rawJson.contains("\"table\":\"orders\"")) {
                        Log.i("CaptainRepository", "Realtime update: orders changed, refreshing...")
                        getOrderHistory()
                    } else if (rawJson.contains("\"table\":\"kots\"")) {
                        Log.i("CaptainRepository", "Realtime update: kots changed, refreshing...")
                        getAllKOTs()
                    } else if (rawJson.contains("\"table\":\"bills\"")) {
                        Log.i("CaptainRepository", "Realtime update: bills changed, refreshing...")
                    } else if (rawJson.contains("\"table\":\"settlements\"")) {
                        Log.i("CaptainRepository", "Realtime update: settlements changed, refreshing...")
                        getSettledPayments()
                    }
                } catch (e: Exception) {
                    Log.e("CaptainRepository", "Error processing realtime event", e)
                }
            }
        }
    }

    override suspend fun getTables(refresh: Boolean): Result<List<RestaurantTable>> = withContext(dispatcher) {
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        if (!refresh && tables.isNotEmpty()) {
            return@withContext Result.success(tables)
        }
        try {
            Log.d("DEBUG_APP", "GET_TABLES_REQUEST")
            val response = apiService.getTables()
            if (response.isSuccessful && response.body() != null) {
                // Safely peek at raw JSON Response of GET /api/tables
                val rawJson = try {
                    response.raw().peekBody(1024 * 1024).string()
                } catch (e: Exception) {
                    "Unable to peek stream: ${e.message}"
                }
                Log.i("GET_TABLES_API", "Raw JSON Response of GET /api/tables:\n$rawJson")
                Log.d("DEBUG_APP", "GET_TABLES_RESPONSE")

                val tablesResponse = response.body()!!
                val dtoList = tablesResponse.tables ?: tablesResponse.data

                if (dtoList != null) {
                    val parsedTables = dtoList.map { dto ->
                        val tableId = dto.id ?: dto.underscoreId ?: ""
                        val nameStr = dto.tableNumber ?: "Table $tableId"
                        val matchedStatus = try {
                            val upperStatus = dto.status?.uppercase() ?: "AVAILABLE"
                            when (upperStatus) {
                                "AVAILABLE", "VACANT", "FREE" -> TableStatus.AVAILABLE
                                "OPEN" -> TableStatus.OPEN
                                "RUNNING", "OCCUPIED", "BUSY" -> TableStatus.RUNNING
                                "BILLED" -> TableStatus.BILLED
                                else -> TableStatus.AVAILABLE
                            }
                        } catch (e: Exception) {
                            TableStatus.AVAILABLE
                        }
                        
                        RestaurantTable(
                            id = tableId,
                            tableNumber = dto.tableNumber ?: "0",
                            name = nameStr,
                            status = matchedStatus,
                            assignedCaptainId = dto.assignedCaptainId,
                            activeOrderId = dto.currentOrderId
                        )
                    }

                    tables.clear()
                    tables.addAll(parsedTables)

                    // Cache to Room DB
                    val localEntities = parsedTables.map { it.toLocal(rCode) }
                    localDatabaseDao.insertTables(localEntities)

                    tablesMasterEventFlow.emit(tables.toList())
                    return@withContext Result.success(tables)
                } else {
                    Log.d("DEBUG_APP", "GET_TABLES_ERROR: No tables found in response envelope.")
                    return@withContext Result.failure(Exception("No tables found in response envelope."))
                }
            } else {
                Log.d("DEBUG_APP", "GET_TABLES_ERROR: API response not successful (${response.code()})")
                // Try fallback to DB cache
                val cached = localDatabaseDao.getTables(rCode)
                if (cached.isNotEmpty()) {
                    tables.clear()
                    tables.addAll(cached.map { it.toDomain() })
                    tablesMasterEventFlow.emit(tables.toList())
                    return@withContext Result.success(tables)
                }
                return@withContext Result.failure(Exception("Failed to load tables (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.d("DEBUG_APP", "GET_TABLES_ERROR: ${e.message}")
            val cached = localDatabaseDao.getTables(rCode)
            if (cached.isNotEmpty()) {
                tables.clear()
                tables.addAll(cached.map { it.toDomain() })
                tablesMasterEventFlow.emit(tables.toList())
                return@withContext Result.success(tables)
            }
            return@withContext Result.failure(e)
        }
    }

    override suspend fun openTable(tableNumber: String): Result<RestaurantTable> = withContext(dispatcher) {
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        val tokenValue = currentUser?.token ?: prefs.getString("jwt_token", null)
        val isJwtTokenPresent = !tokenValue.isNullOrEmpty()

        Log.d("OPEN_TABLE", "------------------- CAPTAIN_REPOSITORY.OPEN_TABLE START -------------------")
        Log.d("OPEN_TABLE", "Table Number: $tableNumber")
        Log.d("OPEN_TABLE", "Restaurant Code: $rCode")
        Log.d("OPEN_TABLE", "JWT Token Present: $isJwtTokenPresent")
        Log.d("OPEN_TABLE", "Request Body: TableOpenRequest(tableNumber=$tableNumber)")

        try {
            val response = apiService.openTable(TableOpenRequest(tableNumber))
            val reqUrl = response.raw().request.url.toString()
            val statusCode = response.code()
            Log.d("OPEN_TABLE", "Request URL: $reqUrl")
            Log.d("OPEN_TABLE", "HTTP Status: $statusCode")

            if (response.isSuccessful && response.body() != null) {
                val rawBody = try {
                    response.raw().peekBody(1024 * 1024).string()
                } catch (pe: Exception) {
                    "Unable to peek body: ${pe.message}"
                }
                Log.d("OPEN_TABLE", "Response Body (Raw): $rawBody")
                Log.d("OPEN_TABLE", "OPEN_TABLE_RESPONSE: $rawBody")

                _openTableDebugLog.value = OpenTableDebugState(
                    lastUrl = reqUrl,
                    lastStatusCode = statusCode,
                    lastResponse = rawBody,
                    lastError = null
                )

                val opened = response.body()!!
                val idx = tables.indexOfFirst { it.id == opened.id }
                if (idx != -1) tables[idx] = opened
                
                // Cache to Room DB
                localDatabaseDao.insertTables(listOf(opened.toLocal(rCode)))

                tableEventFlow.emit(opened)
                Log.d("OPEN_TABLE", "Successfully opened table and emitted event.")
                Log.d("OPEN_TABLE", "------------------- CAPTAIN_REPOSITORY.OPEN_TABLE END -------------------")
                return@withContext Result.success(opened)
            } else {
                val errBody = try {
                    response.errorBody()?.string() ?: response.raw().message
                } catch (be: Exception) {
                    "Error reading body: ${be.message}"
                }
                Log.d("OPEN_TABLE", "Response Body (Failure Case): $errBody")
                Log.d("OPEN_TABLE", "OPEN_TABLE_RESPONSE: $errBody")
                _openTableDebugLog.value = OpenTableDebugState(
                    lastUrl = reqUrl,
                    lastStatusCode = statusCode,
                    lastResponse = errBody,
                    lastError = "HTTP ${statusCode}: $errBody"
                )
                val ex = Exception("Failed to open table (${response.code()}) info: $errBody")
                Log.d("OPEN_TABLE", "------------------- CAPTAIN_REPOSITORY.OPEN_TABLE ERROR -------------------")
                return@withContext Result.failure(ex)
            }
        } catch (e: Exception) {
            val stack = e.stackTraceToString()
            Log.d("OPEN_TABLE", "Exception Stacktrace:\n$stack")
            _openTableDebugLog.value = OpenTableDebugState(
                lastUrl = "N/A - Exception",
                lastStatusCode = null,
                lastResponse = null,
                lastError = e.message ?: e.toString()
            )
            Log.d("OPEN_TABLE", "------------------- CAPTAIN_REPOSITORY.OPEN_TABLE EXCEPTION -------------------")
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getMenuItems(refresh: Boolean): Result<List<MenuItem>> = withContext(dispatcher) {
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        try {
            val response = apiService.getMenuItems()
            if (response.isSuccessful && response.body() != null) {
                val fetched = response.body()!!
                menuItems.clear()
                menuItems.addAll(fetched)

                // Cache to Room DB
                val localEntities = fetched.map { it.toLocal(rCode) }
                localDatabaseDao.insertMenuItems(localEntities)

                return@withContext Result.success(menuItems)
            } else {
                val cached = localDatabaseDao.getMenuItems(rCode)
                if (cached.isNotEmpty()) {
                    menuItems.clear()
                    menuItems.addAll(cached.map { it.toDomain() })
                    return@withContext Result.success(menuItems)
                }
                return@withContext Result.failure(Exception("Failed to get menu items"))
            }
        } catch (e: Exception) {
            val cached = localDatabaseDao.getMenuItems(rCode)
            if (cached.isNotEmpty()) {
                menuItems.clear()
                menuItems.addAll(cached.map { it.toDomain() })
                return@withContext Result.success(menuItems)
            }
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getCategories(refresh: Boolean): Result<List<String>> = withContext(dispatcher) {
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body() != null) {
                val fetched = response.body()!!
                
                // Cache to Room DB
                val localEntities = fetched.map { it.toLocalCategory(rCode) }
                localDatabaseDao.insertCategories(localEntities)

                return@withContext Result.success(fetched)
            } else {
                val cached = localDatabaseDao.getCategories(rCode)
                if (cached.isNotEmpty()) {
                    return@withContext Result.success(cached.map { it.name })
                }
                return@withContext Result.failure(Exception("Failed to load categories"))
            }
        } catch (e: Exception) {
            val cached = localDatabaseDao.getCategories(rCode)
            if (cached.isNotEmpty()) {
                return@withContext Result.success(cached.map { it.name })
            }
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getActiveOrder(tableId: String): Order? {
        val table = tables.find { it.id == tableId } ?: return null
        return orders.find { it.id == table.activeOrderId && it.status == "ACTIVE" }
    }

    override suspend fun addItemToOrder(tableId: String, menuItemId: String, quantity: Int): Result<Order> = withContext(dispatcher) {
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        try {
            val response = apiService.addItemToOrder(AddItemRequest(tableId, menuItemId, quantity))
            if (response.isSuccessful && response.body() != null) {
                val updatedOrder = response.body()!!
                val idx = orders.indexOfFirst { it.id == updatedOrder.id }
                if (idx != -1) orders[idx] = updatedOrder else orders.add(updatedOrder)

                // Sync table state
                val table = tables.find { it.id == tableId }
                if (table != null) {
                    val updatedTable = table.copy(status = TableStatus.RUNNING, activeOrderId = updatedOrder.id)
                    val tIdx = tables.indexOf(table)
                    tables[tIdx] = updatedTable
                    localDatabaseDao.insertTables(listOf(updatedTable.toLocal(rCode)))
                    tableEventFlow.emit(updatedTable)
                }

                orderEventFlow.emit(updatedOrder)
                return@withContext Result.success(updatedOrder)
            } else {
                return@withContext Result.failure(Exception("Failed to add menu item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateOrderItem(tableId: String, itemId: String, quantity: Int): Result<Order> = withContext(dispatcher) {
        try {
            val response = apiService.updateOrderItem(UpdateItemRequest(tableId, itemId, quantity))
            if (response.isSuccessful && response.body() != null) {
                val updatedOrder = response.body()!!
                val idx = orders.indexOfFirst { it.id == updatedOrder.id }
                if (idx != -1) orders[idx] = updatedOrder

                orderEventFlow.emit(updatedOrder)
                return@withContext Result.success(updatedOrder)
            } else {
                return@withContext Result.failure(Exception("Failed to update order item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun removeOrderItem(tableId: String, itemId: String): Result<Order> = withContext(dispatcher) {
        try {
            val response = apiService.removeOrderItem(RemoveItemRequest(tableId, itemId))
            if (response.isSuccessful && response.body() != null) {
                val updatedOrder = response.body()!!
                val idx = orders.indexOfFirst { it.id == updatedOrder.id }
                if (idx != -1) orders[idx] = updatedOrder

                orderEventFlow.emit(updatedOrder)
                return@withContext Result.success(updatedOrder)
            } else {
                return@withContext Result.failure(Exception("Failed to remove order item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun sendKOT(tableId: String): Result<KOT> = withContext(dispatcher) {
        if (heldTableIds.contains(tableId)) {
            return@withContext Result.failure(Exception("Table is on hold. Resume the order first."))
        }
        try {
            val response = apiService.sendKOT(SendKOTRequest(tableId))
            if (response.isSuccessful && response.body() != null) {
                val kot = response.body()!!
                kots.add(kot)

                kotEventFlow.emit(kot)

                // Auto Print KOT
                val config = printerManager.getSettings()
                if (config.autoPrintKot) {
                    scope.launch {
                        printKOT(kot.id, reprint = false)
                    }
                }
                return@withContext Result.success(kot)
            } else {
                return@withContext Result.failure(Exception("Failed to send KOT (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getKOTHistory(orderId: String): Result<List<KOT>> = withContext(dispatcher) {
        val matching = kots.filter { it.orderId == orderId }
        Result.success(matching)
    }

    override suspend fun getAllKOTs(): Result<List<KOT>> = withContext(dispatcher) {
        Result.success(kots)
    }

    override suspend fun printKOT(kotId: String, reprint: Boolean, reprintReason: String?): Result<Unit> = withContext(dispatcher) {
        try {
            val kot = kots.find { it.id == kotId }
                ?: return@withContext Result.failure(Exception("KOT not found locally for ID: $kotId"))
            
            // Manage Local Reprint tracking
            val sp = context.getSharedPreferences("kot_reprints", android.content.Context.MODE_PRIVATE)
            val key = "reprint_count_$kotId"
            var currentCount = sp.getInt(key, 0)
            if (reprint) {
                currentCount += 1
                sp.edit().putInt(key, currentCount).apply()
            }

            // Manage continuous KOT sequence numbering
            val seqKey = "kot_seq_$kotId"
            var seqNum = sp.getString(seqKey, null)
            if (seqNum == null) {
                val globalSeq = sp.getInt("global_kot_sequence", 24) + 1
                sp.edit().putInt("global_kot_sequence", globalSeq).apply()
                seqNum = String.format(Locale.getDefault(), "KOT-%04d", globalSeq)
                sp.edit().putString(seqKey, seqNum).apply()
            }

            val config = printerManager.getSettings()

            // Group KOT items by target printer category map (Multi-Kitchen feature)
            val drinksItems = kot.items.filter {
                val cat = menuItems.find { m -> m.id == it.menuItemId }?.category?.lowercase() ?: ""
                cat.contains("drink") || cat.contains("beverage")
            }
            val dessertItems = kot.items.filter {
                val cat = menuItems.find { m -> m.id == it.menuItemId }?.category?.lowercase() ?: ""
                cat.contains("dessert") || cat.contains("sweet")
            }
            val mainsItems = kot.items.filter {
                !drinksItems.contains(it) && !dessertItems.contains(it)
            }

            var printedAny = false
            if (drinksItems.isNotEmpty()) {
                val subKot = kot.copy(items = drinksItems)
                val bytes = printerManager.generateKOTBytes(subKot, config.paperWidth, reprint, currentCount, "$reprintReason [DRINKS DEPT]", seqNum)
                printerManager.printBytesWithFailover(bytes, config.type, config.drinksPrinterIp, config.port, config.nameOrMac)
                printedAny = true
            }

            if (dessertItems.isNotEmpty()) {
                val subKot = kot.copy(items = dessertItems)
                val bytes = printerManager.generateKOTBytes(subKot, config.paperWidth, reprint, currentCount, "$reprintReason [DESSERT DEPT]", seqNum)
                printerManager.printBytesWithFailover(bytes, config.type, config.dessertPrinterIp, config.port, config.nameOrMac)
                printedAny = true
            }

            if (mainsItems.isNotEmpty() || !printedAny) {
                val itemsToPrint = if (mainsItems.isNotEmpty()) mainsItems else kot.items
                val subKot = kot.copy(items = itemsToPrint)
                val bytes = printerManager.generateKOTBytes(subKot, config.paperWidth, reprint, currentCount, "$reprintReason [MAINS DEPT]", seqNum)
                printerManager.printBytesWithFailover(bytes, config.type, config.mainsPrinterIp, config.port, config.nameOrMac)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Error printing KOT: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun generateBill(tableId: String): Result<Bill> = withContext(dispatcher) {
        if (heldTableIds.contains(tableId)) {
            return@withContext Result.failure(Exception("Table is on hold. Resume the order first."))
        }
        try {
            val response = apiService.generateBill(GenerateBillRequest(tableId))
            if (response.isSuccessful && response.body() != null) {
                val bill = response.body()!!
                bills.add(bill)

                // Sync local table state to BILLED
                val table = tables.find { it.id == tableId }
                if (table != null) {
                    val updatedTable = table.copy(status = TableStatus.BILLED)
                    val tIdx = tables.indexOf(table)
                    tables[tIdx] = updatedTable
                    tableEventFlow.emit(updatedTable)
                }

                billEventFlow.emit(bill)

                // Auto Print Bill
                val config = printerManager.getSettings()
                if (config.autoPrintBill) {
                    scope.launch {
                        printBill(bill.id, tableId)
                    }
                }
                return@withContext Result.success(bill)
            } else {
                return@withContext Result.failure(Exception("Failed to generate bill"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun printBill(billId: String, tableId: String): Result<Unit> = withContext(dispatcher) {
        try {
            val bill = bills.find { it.id == billId }
                ?: return@withContext Result.failure(Exception("Bill not found locally for ID: $billId"))
            val orderObj = orders.find { it.id == bill.orderId }
                ?: getActiveOrder(tableId)
            val items = orderObj?.items ?: emptyList()

            val settingsResult = getSettings()
            val settings = settingsResult.getOrNull()
            val config = printerManager.getSettings()

            val bytes = printerManager.generateBillBytes(bill, items, settings, config.paperWidth)
            printerManager.printBytesWithFailover(bytes, config.billingType, config.billingIp, config.billingPort, config.billingNameOrMac)
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Error printing Bill: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getBill(tableId: String): Bill? {
        val table = tables.find { it.id == tableId } ?: return null
        return bills.find { it.tableId == tableId && it.status == "UNPAID" }
    }

    override suspend fun settleBill(
        billId: String,
        cash: Double,
        card: Double,
        upi: Double,
        paymentType: String
    ): Result<PaymentSettlement> = withContext(dispatcher) {
        try {
            val response = apiService.settleBill(SettleBillRequest(billId, cash, card, upi, paymentType))
            if (response.isSuccessful && response.body() != null) {
                val settlement = response.body()!!
                settlements.add(settlement)

                // Complete corresponding bill, order, and free table locally
                val bill = bills.find { it.id == billId }
                if (bill != null) {
                    val bIdx = bills.indexOf(bill)
                    bills[bIdx] = bill.copy(status = "PAID")

                    val oIdx = orders.indexOfFirst { it.id == bill.orderId }
                    if (oIdx != -1) {
                        orders[oIdx] = orders[oIdx].copy(status = "COMPLETED")
                    }

                    val table = tables.find { it.id == bill.tableId }
                    if (table != null) {
                        val freedTable = table.copy(status = TableStatus.AVAILABLE, assignedCaptainId = null, activeOrderId = null)
                        val tIdx = tables.indexOf(table)
                        tables[tIdx] = freedTable
                        tableEventFlow.emit(freedTable)
                    }
                    billEventFlow.emit(bill)
                }

                paymentEventFlow.emit(settlement)
                return@withContext Result.success(settlement)
            } else {
                return@withContext Result.failure(Exception("Failed to settle bill"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getOrderHistory(): Result<List<Order>> = withContext(dispatcher) {
        val completed = orders.filter { it.status == "COMPLETED" }
        Result.success(completed)
    }

    override suspend fun getSettledPayments(): Result<List<PaymentSettlement>> = withContext(dispatcher) {
        Result.success(settlements)
    }

    // --- Admin CRUD Operations ---
    override suspend fun createCategory(name: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.createCategory(mapOf("name" to name, "restaurant_code" to (currentUser?.restaurantCode ?: "")))
            if (response.isSuccessful) {
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to create category (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateCategory(oldName: String, newName: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.updateCategory(oldName, mapOf("name" to newName, "restaurant_code" to (currentUser?.restaurantCode ?: "")))
            if (response.isSuccessful) {
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to update category (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun deleteCategory(name: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.deleteCategory(name)
            if (response.isSuccessful) {
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to delete category (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun createMenuItem(item: MenuItem): Result<MenuItem> = withContext(dispatcher) {
        try {
            val response = apiService.createMenuItem(item)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                val idx = menuItems.indexOfFirst { it.id == created.id }
                if (idx != -1) {
                    menuItems[idx] = created
                } else {
                    menuItems.add(created)
                }
                menuEventFlow.emit(Unit)
                return@withContext Result.success(created)
            } else {
                return@withContext Result.failure(Exception("Failed to create menu item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateMenuItem(item: MenuItem): Result<MenuItem> = withContext(dispatcher) {
        try {
            val response = apiService.updateMenuItem(item.id, item)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = menuItems.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    menuItems[idx] = updated
                }
                menuEventFlow.emit(Unit)
                return@withContext Result.success(updated)
            } else {
                return@withContext Result.failure(Exception("Failed to update menu item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun deleteMenuItem(id: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.deleteMenuItem(id)
            if (response.isSuccessful) {
                menuItems.removeAll { it.id == id }
                menuEventFlow.emit(Unit)
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to delete menu item (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun createTable(table: RestaurantTable): Result<RestaurantTable> = withContext(dispatcher) {
        try {
            val response = apiService.createTable(table)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                val idx = tables.indexOfFirst { it.id == created.id }
                if (idx != -1) {
                    tables[idx] = created
                } else {
                    tables.add(created)
                }
                tableEventFlow.emit(created)
                return@withContext Result.success(created)
            } else {
                return@withContext Result.failure(Exception("Failed to create table (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateTable(table: RestaurantTable): Result<RestaurantTable> = withContext(dispatcher) {
        try {
            val response = apiService.updateTable(table.id, table)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = tables.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    tables[idx] = updated
                }
                tableEventFlow.emit(updated)
                return@withContext Result.success(updated)
            } else {
                return@withContext Result.failure(Exception("Failed to update table (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun deleteTable(id: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.deleteTable(id)
            if (response.isSuccessful) {
                tables.removeAll { it.id == id }
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to delete table (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getUsers(): Result<List<UserDto>> = withContext(dispatcher) {
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                return@withContext Result.failure(Exception("Failed to load users (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun createUser(username: String, role: String, restaurantCode: String, pss: String): Result<UserDto> = withContext(dispatcher) {
        try {
            val request = mapOf(
                "username" to username,
                "role" to role,
                "restaurant_code" to restaurantCode,
                "password" to pss
            )
            val response = apiService.createUser(request)
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                return@withContext Result.failure(Exception("Failed to create user (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun updateUser(id: String, username: String, role: String, restaurantCode: String, pss: String): Result<UserDto> = withContext(dispatcher) {
        try {
            val request = mapOf(
                "username" to username,
                "role" to role,
                "restaurant_code" to restaurantCode,
                "password" to pss
            )
            val response = apiService.updateUser(id, request)
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                return@withContext Result.failure(Exception("Failed to update user (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> = withContext(dispatcher) {
        try {
            val response = apiService.deleteUser(id)
            if (response.isSuccessful) {
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Failed to delete user (${response.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun getAuditLogs(): Result<List<AuditLog>> = withContext(dispatcher) {
        try {
            val response = apiService.getAuditLogs()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                val user = currentUser
                return@withContext Result.success(
                    listOf(
                        AuditLog("1", user?.id ?: "capt-1", user?.username ?: "Michael", "USER_LOGIN", "Authenticated successfully into ${user?.restaurantCode ?: "Spice Garden"}")
                    )
                )
            }
        } catch (e: Exception) {
            val user = currentUser
            return@withContext Result.success(
                listOf(
                    AuditLog("1", user?.id ?: "capt-1", user?.username ?: "Michael", "USER_LOGIN", "Authenticated silently into ${user?.restaurantCode ?: "Spice Garden"}")
                )
            )
        }
    }

    override suspend fun getSettings(): Result<RestaurantSettings> = withContext(dispatcher) {
        try {
            val response = apiService.getSettings()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                return@withContext Result.success(
                    RestaurantSettings(
                        id = "default_settings",
                        restaurantName = (currentUser?.restaurantCode ?: "Spice Garden") + " POS",
                        taxPercentage = 5.0,
                        serviceChargePercentage = 10.0,
                        currency = "INR",
                        logoUrl = null,
                        gstNumber = "27AAAPS1234A1Z5",
                        fssaiNumber = "12345678901234",
                        address = "123 Tech Park, Sector 5, Bangalore",
                        phoneNumber = "+91 98765 43210",
                        footerMessage = "GST & Service Charges applied as per govt norms.",
                        thankYouMessage = "Thank you for dining with us!"
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.success(
                RestaurantSettings(
                    id = "default_settings",
                    restaurantName = (currentUser?.restaurantCode ?: "Spice Garden") + " POS",
                    taxPercentage = 5.0,
                    serviceChargePercentage = 10.0,
                    currency = "INR",
                    logoUrl = null,
                    gstNumber = "27AAAPS1234A1Z5",
                    fssaiNumber = "12345678901234",
                    address = "123 Tech Park, Sector 5, Bangalore",
                    phoneNumber = "+91 98765 43210",
                    footerMessage = "GST & Service Charges applied as per govt norms.",
                    thankYouMessage = "Thank you for dining with us!"
                )
            )
        }
    }

    override suspend fun updateSettings(settings: RestaurantSettings): Result<RestaurantSettings> = withContext(dispatcher) {
        try {
            val response = apiService.updateSettings(settings)
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else {
                return@withContext Result.success(settings)
            }
        } catch (e: Exception) {
            return@withContext Result.success(settings)
        }
    }

    override suspend fun getDailySalesReport(): Result<List<DailySalesReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getDailySalesReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed daily sales API", e)
        }
        // Fallback
        val dateToday = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val mockData = listOf(
            DailySalesReport(dateToday, 14, 18500.0, 16800.0, 840.0, 860.0),
            DailySalesReport("15-Jun-2026", 18, 24500.0, 22270.0, 1115.0, 1115.0),
            DailySalesReport("16-Jun-2026", 22, 31200.0, 28360.0, 1420.0, 1420.0)
        )
        Result.success(mockData)
    }

    override suspend fun getCaptainSalesReport(): Result<List<CaptainSalesReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getCaptainSalesReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed captain sales API", e)
        }
        val mockData = listOf(
            CaptainSalesReport("capt-1", "Michael Scott", 12, 14850.0),
            CaptainSalesReport("capt-2", "Dwight Schrute", 15, 21320.0),
            CaptainSalesReport("capt-3", "Jim Halpert", 9, 11200.0)
        )
        Result.success(mockData)
    }

    override suspend fun getItemSalesReport(): Result<List<ItemSalesReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getItemSalesReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed item sales API", e)
        }
        val mockData = listOf(
            ItemSalesReport("item-1", "Butter Chicken", 42, "Main Course", 23100.0),
            ItemSalesReport("item-2", "Garlic Naan", 112, "Starters", 5600.0),
            ItemSalesReport("item-3", "Paneer Tikka Masala", 28, "Vegetarian", 12600.0),
            ItemSalesReport("item-4", "Mango Lassi", 65, "Beverages", 7800.0),
            ItemSalesReport("item-5", "Sizzling Brownie", 31, "Desserts", 6200.0)
        )
        Result.success(mockData)
    }

    override suspend fun getHourlySalesReport(): Result<List<HourlySalesReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getHourlySalesReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed hourly sales API", e)
        }
        val mockData = listOf(
            HourlySalesReport(12, 4, 3800.0),
            HourlySalesReport(13, 8, 8600.0),
            HourlySalesReport(14, 3, 2900.0),
            HourlySalesReport(19, 11, 14500.0),
            HourlySalesReport(20, 16, 21000.0),
            HourlySalesReport(21, 14, 18500.0)
        )
        Result.success(mockData)
    }

    override suspend fun getSettlementReport(): Result<List<SettlementReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getSettlementReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed settlements API", e)
        }
        val mockData = listOf(
            SettlementReport("CASH", 24, 18450.0),
            SettlementReport("CARD", 12, 14200.0),
            SettlementReport("UPI", 32, 28550.0)
        )
        Result.success(mockData)
    }

    override suspend fun getCashClosingReport(): Result<List<CashClosingReport>> = withContext(dispatcher) {
        try {
            val response = apiService.getCashClosingReport()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Log.e("CaptainRepository", "Failed cash closing API", e)
        }
        val dateToday = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val mockData = listOf(
            CashClosingReport(dateToday, 5000.0, 18450.0, 23450.0, 0.0, "Perfectly Matched"),
            CashClosingReport("15-Jun-2026", 5000.0, 21400.0, 26380.0, -20.0, "Slight cash registers discrepancy"),
            CashClosingReport("16-Jun-2026", 5000.0, 19200.0, 24200.0, 0.0, "Verified by Michael")
        )
        Result.success(mockData)
    }

    override suspend fun updateKOTStatus(kotId: String, status: String): Result<KOT> = withContext(dispatcher) {
        val kot = kots.find { it.id == kotId } ?: return@withContext Result.failure(Exception("KOT not found"))
        val updatedKot = kot.copy(status = status)
        val idx = kots.indexOf(kot)
        if (idx != -1) {
            kots[idx] = updatedKot
        }
        
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        localDatabaseDao.insertKot(LocalKotEntity(
            id = updatedKot.id,
            orderId = updatedKot.orderId,
            tableId = updatedKot.tableId,
            tableName = updatedKot.tableName,
            itemsJson = orderItemsAdapter.toJson(updatedKot.items),
            status = updatedKot.status,
            createdAt = updatedKot.createdAt,
            isSynced = false
        ))
        
        kotEventFlow.emit(updatedKot)
        Result.success(updatedKot)
    }

    override suspend fun transferTable(fromTableId: String, toTableId: String): Result<Unit> = withContext(dispatcher) {
        val fromTable = tables.find { it.id == fromTableId } ?: return@withContext Result.failure(Exception("Source table not found"))
        val toTable = tables.find { it.id == toTableId } ?: return@withContext Result.failure(Exception("Target table not found"))
        
        if (fromTable.activeOrderId == null) {
            return@withContext Result.failure(Exception("Source table has no active order to transfer"))
        }
        if (toTable.status == TableStatus.RUNNING || toTable.status == TableStatus.BILLED) {
            return@withContext Result.failure(Exception("Target table is currently occupied! Merge or split instead."))
        }
        
        val order = orders.find { it.id == fromTable.activeOrderId && it.status == "ACTIVE" }
        if (order != null) {
            val updatedOrder = order.copy(tableId = toTableId)
            val oIdx = orders.indexOf(order)
            if (oIdx != -1) {
                orders[oIdx] = updatedOrder
            }
            orderEventFlow.emit(updatedOrder)
        }
        
        val updatedFromTable = fromTable.copy(status = TableStatus.AVAILABLE, activeOrderId = null)
        val updatedToTable = toTable.copy(status = TableStatus.RUNNING, activeOrderId = fromTable.activeOrderId)
        
        val fIdx = tables.indexOf(fromTable)
        if (fIdx != -1) tables[fIdx] = updatedFromTable
        val tIdx = tables.indexOf(toTable)
        if (tIdx != -1) tables[tIdx] = updatedToTable
        
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        localDatabaseDao.insertTables(listOf(updatedFromTable.toLocal(rCode), updatedToTable.toLocal(rCode)))
        tableEventFlow.emit(updatedFromTable)
        tableEventFlow.emit(updatedToTable)
        
        Result.success(Unit)
    }

    override suspend fun mergeTables(fromTableId: String, toTableId: String): Result<Unit> = withContext(dispatcher) {
        val fromTable = tables.find { it.id == fromTableId } ?: return@withContext Result.failure(Exception("Source table not found"))
        val toTable = tables.find { it.id == toTableId } ?: return@withContext Result.failure(Exception("Target table not found"))
        
        if (fromTable.activeOrderId == null) {
            return@withContext Result.failure(Exception("Source table has no active order to merge"))
        }
        
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        val fromOrder = orders.find { it.id == fromTable.activeOrderId && it.status == "ACTIVE" }
            ?: return@withContext Result.failure(Exception("Active order of source table not found"))
            
        if (toTable.activeOrderId == null) {
            return@withContext transferTable(fromTableId, toTableId)
        }
        
        val toOrder = orders.find { it.id == toTable.activeOrderId && it.status == "ACTIVE" }
            ?: return@withContext Result.failure(Exception("Active order of target table not found"))
            
        val mergedItems = toOrder.items.toMutableList()
        for (fItem in fromOrder.items) {
            val existIdx = mergedItems.indexOfFirst { it.menuItemId == fItem.menuItemId }
            if (existIdx != -1) {
                val existItem = mergedItems[existIdx]
                mergedItems[existIdx] = existItem.copy(
                    quantity = existItem.quantity + fItem.quantity
                )
            } else {
                mergedItems.add(fItem)
            }
        }
        
        val newAmount = mergedItems.sumOf { it.total }
        val updatedToOrder = toOrder.copy(items = mergedItems, totalAmount = newAmount)
        val toOIdx = orders.indexOf(toOrder)
        if (toOIdx != -1) {
            orders[toOIdx] = updatedToOrder
        }
        orderEventFlow.emit(updatedToOrder)
        
        orders.remove(fromOrder)
        
        val updatedFromTable = fromTable.copy(status = TableStatus.AVAILABLE, activeOrderId = null)
        val updatedToTable = toTable.copy(status = TableStatus.RUNNING, activeOrderId = toOrder.id)
        
        val fIdx = tables.indexOf(fromTable)
        if (fIdx != -1) tables[fIdx] = updatedFromTable
        val tIdx = tables.indexOf(toTable)
        if (tIdx != -1) tables[tIdx] = updatedToTable
        
        localDatabaseDao.insertTables(listOf(updatedFromTable.toLocal(rCode), updatedToTable.toLocal(rCode)))
        tableEventFlow.emit(updatedFromTable)
        tableEventFlow.emit(updatedToTable)
        
        Result.success(Unit)
    }

    override suspend fun splitTable(tableId: String, targetTableId: String, itemIdsToMove: List<String>): Result<Unit> = withContext(dispatcher) {
        val fromTable = tables.find { it.id == tableId } ?: return@withContext Result.failure(Exception("Source table not found"))
        val toTable = tables.find { it.id == targetTableId } ?: return@withContext Result.failure(Exception("Target table not found"))
        
        if (fromTable.activeOrderId == null) {
            return@withContext Result.failure(Exception("Source table has no active order"))
        }
        val fromOrder = orders.find { it.id == fromTable.activeOrderId && it.status == "ACTIVE" }
            ?: return@withContext Result.failure(Exception("Active order of source table not found"))
            
        val itemsToKeep = fromOrder.items.toMutableList()
        val itemsMoved = mutableListOf<OrderItem>()
        
        for (itemId in itemIdsToMove) {
            val itemIdx = itemsToKeep.indexOfFirst { it.id == itemId }
            if (itemIdx != -1) {
                val item = itemsToKeep.removeAt(itemIdx)
                itemsMoved.add(item)
            }
        }
        
        if (itemsMoved.isEmpty()) {
            return@withContext Result.failure(Exception("No matching items found to split"))
        }
        
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        
        val newOrderId = "ORD-SPLIT-" + UUID.randomUUID().toString().take(6).uppercase()
        val splitOrderTotal = itemsMoved.sumOf { it.total }
        val newOrder = Order(
            id = newOrderId,
            tableId = targetTableId,
            items = itemsMoved,
            status = "ACTIVE",
            captainId = currentUser?.id ?: "SYSTEM",
            totalAmount = splitOrderTotal,
            createdAt = System.currentTimeMillis()
        )
        orders.add(newOrder)
        orderEventFlow.emit(newOrder)
        
        val updatedFromOrder = fromOrder.copy(items = itemsToKeep, totalAmount = itemsToKeep.sumOf { it.total })
        val fromOIdx = orders.indexOf(fromOrder)
        if (fromOIdx != -1) {
            orders[fromOIdx] = updatedFromOrder
        }
        orderEventFlow.emit(updatedFromOrder)
        
        val updatedToTable = toTable.copy(status = TableStatus.RUNNING, activeOrderId = newOrderId)
        val tIdx = tables.indexOf(toTable)
        if (tIdx != -1) tables[tIdx] = updatedToTable
        tableEventFlow.emit(updatedToTable)
        
        if (itemsToKeep.isEmpty()) {
            val updatedFromTable = fromTable.copy(status = TableStatus.AVAILABLE, activeOrderId = null)
            val fIdx = tables.indexOf(fromTable)
            if (fIdx != -1) tables[fIdx] = updatedFromTable
            tableEventFlow.emit(updatedFromTable)
            localDatabaseDao.insertTables(listOf(updatedFromTable.toLocal(rCode), updatedToTable.toLocal(rCode)))
        } else {
            localDatabaseDao.insertTables(listOf(updatedToTable.toLocal(rCode)))
        }
        
        Result.success(Unit)
    }

    override suspend fun changeCaptain(tableId: String, captainId: String, captainName: String): Result<Unit> = withContext(dispatcher) {
        val table = tables.find { it.id == tableId } ?: return@withContext Result.failure(Exception("Table not found"))
        val updatedTable = table.copy(assignedCaptainId = captainId)
        val idx = tables.indexOf(table)
        if (idx != -1) {
            tables[idx] = updatedTable
        }
        
        val rCode = currentUser?.restaurantCode ?: "DEFAULT"
        localDatabaseDao.insertTables(listOf(updatedTable.toLocal(rCode)))
        tableEventFlow.emit(updatedTable)
        Result.success(Unit)
    }

    override suspend fun holdTableOrder(tableId: String, hold: Boolean): Result<Unit> {
        if (hold) {
            heldTableIds.add(tableId)
        } else {
            heldTableIds.remove(tableId)
        }
        // Emit trigger table updates to update state on UI
        val table = tables.find { it.id == tableId }
        if (table != null) {
            scope.launch {
                tableEventFlow.emit(table)
            }
        }
        return Result.success(Unit)
    }

    override fun isTableHeld(tableId: String): Boolean = heldTableIds.contains(tableId)

    override suspend fun applyDiscountToBill(billId: String, percent: Double, amount: Double, reason: String): Result<Bill> = withContext(dispatcher) {
        val bill = bills.find { it.id == billId } ?: return@withContext Result.failure(Exception("Bill not found"))
        val updatedBill = bill.copy(
            discountPercent = percent,
            discountAmount = amount,
            discountReason = reason
        )
        val idx = bills.indexOf(bill)
        if (idx != -1) {
            bills[idx] = updatedBill
        }
        billEventFlow.emit(updatedBill)
        Result.success(updatedBill)
    }
}
