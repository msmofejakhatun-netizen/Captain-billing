package com.example.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.Bill
import com.example.domain.model.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillingViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _bill = MutableStateFlow<Bill?>(null)
    val bill = _bill.asStateFlow()

    private val _runningOrder = MutableStateFlow<Order?>(null)
    val runningOrder = _runningOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _billGenerationResult = MutableSharedFlow<Result<Bill>>(extraBufferCapacity = 1)
    val billGenerationResult = _billGenerationResult.asSharedFlow()

    private val _printResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)
    val printResult = _printResult.asSharedFlow()

    fun loadBillInfo(tableId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Check if a bill is already generated
                val existingBill = repository.getBill(tableId)
                if (existingBill != null) {
                    _bill.value = existingBill
                } else {
                    // Check if there is an active running order to bill
                    val activeOrder = repository.getActiveOrder(tableId)
                    _runningOrder.value = activeOrder
                    _bill.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to retrieve billing statistics"
            }
            _isLoading.value = false
        }
    }

    fun generateBill(tableId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.generateBill(tableId)
            _billGenerationResult.emit(result)
            if (result.isSuccess) {
                _bill.value = result.getOrThrow()
                _runningOrder.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate bill"
            }
            _isLoading.value = false
        }
    }

    fun printBill(billId: String, tableId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.printBill(billId, tableId)
            _printResult.emit(result)
            _isLoading.value = false
        }
    }

    fun applyDiscount(
        billId: String,
        tableId: String,
        isPercent: Boolean,
        amount: Double,
        reason: String,
        managerPin: String,
        onFinished: (Result<Bill>) -> Unit
    ) {
        viewModelScope.launch {
            if (managerPin != "1234" && managerPin != "4321" && managerPin != "9999") {
                onFinished(Result.failure(Exception("Unauthorized Manager Passcode! Please try again.")))
                return@launch
            }
            if (reason.trim().isEmpty()) {
                onFinished(Result.failure(Exception("Discount reason is mandatory!")))
                return@launch
            }
            val res = repository.applyDiscountToBill(
                billId = billId,
                percent = if (isPercent) amount else 0.0,
                amount = if (!isPercent) amount else 0.0,
                reason = reason
            )
            res.onSuccess { updatedBill ->
                _bill.value = updatedBill
                repository.getTables(refresh = true)
            }
            onFinished(res)
        }
    }

    fun setupRealtimeBilling(tableId: String) {
        viewModelScope.launch {
            repository.billEventFlow.collect { updatedBill ->
                if (updatedBill.tableId == tableId) {
                    _bill.value = updatedBill
                }
            }
        }
    }
}
