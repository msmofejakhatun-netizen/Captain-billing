package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.RestaurantTable
import com.example.domain.model.TableStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: CaptainRepository) : ViewModel() {

    val isOnline = repository.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _tables = MutableStateFlow<List<RestaurantTable>>(emptyList())
    val tables = _tables.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Aggregated live KPIs
    val totalTables = _tables.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val runningOrdersCount = _tables.map { list ->
        list.count { it.status == TableStatus.RUNNING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingBillsCount = _tables.map { list ->
        list.count { it.status == TableStatus.BILLED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableTablesCount = _tables.map { list ->
        list.count { it.status == TableStatus.AVAILABLE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeCaptainsCount = flow {
        emit(4)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val currentUserRole = flow {
        emit(repository.getCurrentUser()?.role?.uppercase() ?: "CAPTAIN")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CAPTAIN")

    val currentUser = flow {
        emit(repository.getCurrentUser())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _menuItems = MutableStateFlow<List<com.example.domain.model.MenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<com.example.data.remote.UserDto>>(emptyList())
    val adminUsers = _adminUsers.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<com.example.domain.model.AuditLog>>(emptyList())
    val auditLogs = _auditLogs.asStateFlow()

    private val _settings = MutableStateFlow<com.example.domain.model.RestaurantSettings?>(null)
    val settings = _settings.asStateFlow()

    // CRUD categories
    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories(refresh = true).onSuccess { _categories.value = it }
        }
    }
    fun createCategory(name: String) {
        viewModelScope.launch {
            repository.createCategory(name).onSuccess { loadCategories() }
        }
    }
    fun updateCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.updateCategory(oldName, newName).onSuccess { loadCategories() }
        }
    }
    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name).onSuccess { loadCategories() }
        }
    }

    // CRUD menu items
    fun loadMenuItems() {
        viewModelScope.launch {
            repository.getMenuItems(refresh = true).onSuccess { _menuItems.value = it }
        }
    }
    fun createMenuItem(item: com.example.domain.model.MenuItem) {
        viewModelScope.launch {
            repository.createMenuItem(item).onSuccess { loadMenuItems() }
        }
    }
    fun updateMenuItem(item: com.example.domain.model.MenuItem) {
        viewModelScope.launch {
            repository.updateMenuItem(item).onSuccess { loadMenuItems() }
        }
    }
    fun deleteMenuItem(id: String) {
        viewModelScope.launch {
            repository.deleteMenuItem(id).onSuccess { loadMenuItems() }
        }
    }

    // CRUD tables
    fun createTable(table: RestaurantTable) {
        viewModelScope.launch {
            repository.createTable(table).onSuccess { loadDashboardData() }
        }
    }
    fun updateTable(table: RestaurantTable) {
        viewModelScope.launch {
            repository.updateTable(table).onSuccess { loadDashboardData() }
        }
    }
    fun deleteTable(id: String) {
        viewModelScope.launch {
            repository.deleteTable(id).onSuccess { loadDashboardData() }
        }
    }

    // Users CRUD
    fun loadUsers() {
        viewModelScope.launch {
            repository.getUsers().onSuccess { _adminUsers.value = it }
        }
    }
    fun createUser(username: String, role: String, code: String, pss: String) {
        viewModelScope.launch {
            repository.createUser(username, role, code, pss).onSuccess { loadUsers() }
        }
    }
    fun updateUser(id: String, username: String, role: String, code: String, pss: String) {
        viewModelScope.launch {
            repository.updateUser(id, username, role, code, pss).onSuccess { loadUsers() }
        }
    }
    fun deleteUser(id: String) {
        viewModelScope.launch {
            repository.deleteUser(id).onSuccess { loadUsers() }
        }
    }

    // Audit logs
    fun loadAuditLogs() {
        viewModelScope.launch {
            repository.getAuditLogs().onSuccess { _auditLogs.value = it }
        }
    }

    // Settings
    fun loadSettings() {
        viewModelScope.launch {
            repository.getSettings().onSuccess { _settings.value = it }
        }
    }
    fun updateSettings(updated: com.example.domain.model.RestaurantSettings) {
        viewModelScope.launch {
            repository.updateSettings(updated).onSuccess { _settings.value = it }
        }
    }

    init {
        loadDashboardData()
        observeRealtimeChanges()
    }

    fun loadDashboardData() {
        Log.d("DEBUG_APP", "DASHBOARD_START")
        viewModelScope.launch {
            Log.d("DEBUG_APP", "LOADING_TRUE")
            _isLoading.value = true
            _errorMessage.value = null
            try {
                Log.d("DEBUG_APP", "GET_TABLES_REQUEST")
                withTimeout(15000) {
                    repository.getTables(refresh = true)
                        .onSuccess { list ->
                            Log.d("DEBUG_APP", "GET_TABLES_RESPONSE: Size ${list.size}")
                            _tables.value = list
                        }
                        .onFailure { err ->
                            Log.d("DEBUG_APP", "GET_TABLES_ERROR: ${err.message}")
                            _errorMessage.value = err.message ?: "Failed to synchronize dashboard state"
                        }
                }
            } catch (e: TimeoutCancellationException) {
                Log.d("DEBUG_APP", "GET_TABLES_ERROR: Timeout")
                _errorMessage.value = "Request timed out, please check your connection."
            } catch (e: Exception) {
                Log.d("DEBUG_APP", "GET_TABLES_ERROR: ${e.message}")
                _errorMessage.value = e.message ?: "An unexpected error occurred."
            } finally {
                Log.d("DEBUG_APP", "LOADING_FALSE")
                _isLoading.value = false
            }
        }
        Log.d("DEBUG_APP", "DASHBOARD_END")
    }

    private fun observeRealtimeChanges() {
        viewModelScope.launch {
            // Subscribe to real-time table state updates
            repository.tableEventFlow.collect { updatedTable ->
                val currentList = _tables.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == updatedTable.id }
                if (index != -1) {
                    currentList[index] = updatedTable
                    _tables.value = currentList
                }
            }
        }
        viewModelScope.launch {
            repository.tablesMasterEventFlow.collect { freshList ->
                _tables.value = freshList
            }
        }
    }
}
