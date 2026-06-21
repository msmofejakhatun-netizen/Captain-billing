package com.example.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.data.repository.CaptainRepository
import com.example.domain.model.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CaptainRepository) : ViewModel() {

    val isOnline = repository.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder = _activeOrder.asStateFlow()

    private val _tableNumber = MutableStateFlow<String?>(null)
    val tableNumber = _tableNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Calculated fields
    val subtotal = _activeOrder.map { it?.totalAmount ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val taxAmount = subtotal.map { it * 0.05 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0) // 5% GST
    val serviceCharge = subtotal.map { it * 0.10 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0) // 10% Service Charge
    val grandTotal = combine(subtotal, taxAmount, serviceCharge) { sub, tax, service ->
        sub + tax + service
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun loadCart(tableId: String) {
        viewModelScope.launch {
            Log.d("CART_TABLE_ID", tableId)
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val table = repository.getTableById(tableId)
                _tableNumber.value = table?.tableNumber ?: tableId
                
                val order = repository.getActiveOrder(tableId)
                Log.d("CART_PARSED_ORDER", order?.toString() ?: "null")
                if (order != null) {
                    Log.d("CART_ORDER_ID", order.id)
                } else {
                    Log.d("CART_ORDER_ID", "null")
                }
                _activeOrder.value = order
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load order cart"
            }
            _isLoading.value = false
        }
    }

    fun increaseQuantity(tableId: String, orderItemId: String, currentQty: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateOrderItem(tableId, orderItemId, currentQty + 1)
                .onSuccess { updatedOrder ->
                    _activeOrder.value = updatedOrder
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to increase item quantity"
                }
            _isLoading.value = false
        }
    }

    fun decreaseQuantity(tableId: String, orderItemId: String, currentQty: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val newQty = currentQty - 1
            if (newQty <= 0) {
                removeItem(tableId, orderItemId)
            } else {
                repository.updateOrderItem(tableId, orderItemId, newQty)
                    .onSuccess { updatedOrder ->
                        _activeOrder.value = updatedOrder
                    }
                    .onFailure { err ->
                        _errorMessage.value = err.message ?: "Failed to reduce item quantity"
                    }
            }
            _isLoading.value = false
        }
    }

    fun removeItem(tableId: String, orderItemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.removeOrderItem(tableId, orderItemId)
                .onSuccess { updatedOrder ->
                    _activeOrder.value = updatedOrder
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to remove item"
                }
            _isLoading.value = false
        }
    }

    fun setupRealtimeUpdates(tableId: String) {
        viewModelScope.launch {
            repository.orderEventFlow.collect { updatedOrder ->
                if (updatedOrder.tableId == tableId) {
                    _activeOrder.value = updatedOrder
                }
            }
        }
    }
}
