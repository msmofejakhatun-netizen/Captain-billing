package com.example.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.MenuItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MenuViewModel(private val repository: CaptainRepository) : ViewModel() {

    val isOnline = repository.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems = _menuItems.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _addItemResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)
    val addItemResult = _addItemResult.asSharedFlow()

    val filteredItems = combine(_menuItems, _selectedCategory, _searchQuery) { items, cat, query ->
        items.filter { item ->
            val matchCat = cat == null || item.category == cat
            val matchQuery = query.isEmpty() || item.name.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)
            matchCat && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMenu()
        observeRealtimeMenuUpdates()
    }

    private fun observeRealtimeMenuUpdates() {
        viewModelScope.launch {
            repository.menuEventFlow.collect {
                loadMenu()
            }
        }
    }

    fun loadMenu() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val menuDeferred = async { repository.getMenuItems(refresh = true) }
            val categoriesDeferred = async { repository.getCategories(refresh = true) }
            
            val menuResult = menuDeferred.await()
            val categoriesResult = categoriesDeferred.await()
            
            menuResult.onSuccess { list ->
                _menuItems.value = list
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to synchronize menu items"
            }
            
            categoriesResult.onSuccess { cats ->
                _categories.value = cats
            }.onFailure { err ->
                if (_errorMessage.value == null) {
                    _errorMessage.value = err.message ?: "Failed to synchronize categories"
                }
            }
            
            _isLoading.value = false
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addItemToTable(tableId: String, menuItemId: String, quantity: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addItemToOrder(tableId, menuItemId, quantity)
            if (result.isSuccess) {
                _addItemResult.emit(Result.success(Unit))
            } else {
                _addItemResult.emit(Result.failure(result.exceptionOrNull() ?: Exception("Failed to add item to table")))
            }
            _isLoading.value = false
        }
    }
}
