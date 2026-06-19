package com.example.ui.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.RestaurantTable
import com.example.domain.model.TableStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TablesViewModel(private val repository: CaptainRepository) : ViewModel() {

    val isOnline = repository.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _tables = MutableStateFlow<List<RestaurantTable>>(emptyList())
    val tables = _tables.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TableFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _openTableResult = MutableSharedFlow<Result<RestaurantTable>>(extraBufferCapacity = 1)
    val openTableResult = _openTableResult.asSharedFlow()

    val openTableDebugLog = repository.openTableDebugLog

    val filteredTables = combine(_tables, _selectedFilter) { list, filter ->
        when (filter) {
            TableFilter.ALL -> list
            TableFilter.AVAILABLE -> list.filter { it.status == TableStatus.AVAILABLE }
            TableFilter.OPEN -> list.filter { it.status == TableStatus.OPEN }
            TableFilter.RUNNING -> list.filter { it.status == TableStatus.RUNNING }
            TableFilter.BILLED -> list.filter { it.status == TableStatus.BILLED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserRole = flow {
        emit(repository.getCurrentUser()?.role?.uppercase() ?: "CAPTAIN")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CAPTAIN")

    init {
        loadTables()
        observeRealtimeChanges()
    }

    fun loadTables() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getTables(refresh = true)
                .onSuccess { list ->
                    _tables.value = list
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to synchronize tables"
                }
            _isLoading.value = false
        }
    }

    fun setFilter(filter: TableFilter) {
        _selectedFilter.value = filter
    }

    fun openTable(tableNumber: String) {
        android.util.Log.d("OPEN_TABLE", "TablesViewModel.openTable() called with tableNumber: $tableNumber")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            android.util.Log.d("OPEN_TABLE", "Calling repository.openTable(tableNumber=$tableNumber)")
            val result = repository.openTable(tableNumber)
            android.util.Log.d("OPEN_TABLE", "repository.openTable(tableNumber=$tableNumber) completed. isSuccess: ${result.isSuccess}")
            
            _openTableResult.emit(result)
            if (result.isSuccess) {
                // Instantly update local list
                val opened = result.getOrThrow()
                val currentList = _tables.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == opened.id }
                if (idx != -1) {
                    currentList[idx] = opened
                    _tables.value = currentList
                }
            } else {
                val err = result.exceptionOrNull()
                _errorMessage.value = err?.message ?: "Failed to open table"
                android.util.Log.e("OPEN_TABLE", "TablesViewModel.openTable error: ${_errorMessage.value}", err)
            }
            _isLoading.value = false
        }
    }

    private fun observeRealtimeChanges() {
        viewModelScope.launch {
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

    fun isTableOnHold(tableId: String): Boolean {
        return repository.isTableHeld(tableId)
    }

    fun toggleTableHold(tableId: String) {
        viewModelScope.launch {
            val currentlyHeld = repository.isTableHeld(tableId)
            repository.holdTableOrder(tableId, !currentlyHeld)
            loadTables()
        }
    }

    fun transferTable(fromTableId: String, toTableId: String, onFinished: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.transferTable(fromTableId, toTableId)
            if (res.isSuccess) loadTables()
            onFinished(res)
        }
    }

    fun mergeTables(fromTableId: String, toTableId: String, onFinished: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.mergeTables(fromTableId, toTableId)
            if (res.isSuccess) loadTables()
            onFinished(res)
        }
    }

    fun splitTable(tableId: String, targetTableId: String, itemIdsToMove: List<String>, onFinished: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.splitTable(tableId, targetTableId, itemIdsToMove)
            if (res.isSuccess) loadTables()
            onFinished(res)
        }
    }

    fun changeCaptain(tableId: String, captainId: String, captainName: String, onFinished: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.changeCaptain(tableId, captainId, captainName)
            if (res.isSuccess) loadTables()
            onFinished(res)
        }
    }
}

enum class TableFilter {
    ALL, AVAILABLE, OPEN, RUNNING, BILLED
}
