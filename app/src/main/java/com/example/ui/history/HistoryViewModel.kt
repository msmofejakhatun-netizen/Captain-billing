package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.Order
import com.example.domain.model.PaymentSettlement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _completedOrders = MutableStateFlow<List<Order>>(emptyList())
    val completedOrders = _completedOrders.asStateFlow()

    private val _settledPayments = MutableStateFlow<List<PaymentSettlement>>(emptyList())
    val settledPayments = _settledPayments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val totalEarnings = _settledPayments.map { list ->
        list.sumOf { it.cashAmount + it.cardAmount + it.upiAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val ordersCount = _completedOrders.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadHistory()
        observeRealtimeSettlements()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val ordersResult = repository.getOrderHistory()
            val paymentsResult = repository.getSettledPayments()

            if (ordersResult.isSuccess && paymentsResult.isSuccess) {
                _completedOrders.value = ordersResult.getOrThrow().sortedByDescending { it.createdAt }
                _settledPayments.value = paymentsResult.getOrThrow().sortedByDescending { it.createdAt }
            } else {
                _errorMessage.value = "Failed to load database logs"
            }
            _isLoading.value = false
        }
    }

    private fun observeRealtimeSettlements() {
        viewModelScope.launch {
            repository.paymentEventFlow.collect { settlement ->
                val currentSettles = _settledPayments.value.toMutableList()
                currentSettles.add(0, settlement)
                _settledPayments.value = currentSettles

                // Refresh order history to pull the corresponding completed order
                repository.getOrderHistory().onSuccess { list ->
                    _completedOrders.value = list.sortedByDescending { it.createdAt }
                }
            }
        }
    }
}
