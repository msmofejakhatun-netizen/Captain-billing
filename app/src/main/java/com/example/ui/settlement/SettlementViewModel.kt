package com.example.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.PaymentSettlement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettlementViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _settlementResult = MutableSharedFlow<Result<PaymentSettlement>>(extraBufferCapacity = 1)
    val settlementResult = _settlementResult.asSharedFlow()

    fun settleBill(
        billId: String,
        cash: Double,
        card: Double,
        upi: Double,
        paymentType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Validate totals if mixed
            val totalPaid = cash + card + upi
            if (totalPaid <= 0.0) {
                _errorMessage.value = "Enter a valid settlement amount"
                _isLoading.value = false
                return@launch
            }

            val result = repository.settleBill(billId, cash, card, upi, paymentType)
            _settlementResult.emit(result)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Settle bill failed"
            }
            _isLoading.value = false
        }
    }
}
