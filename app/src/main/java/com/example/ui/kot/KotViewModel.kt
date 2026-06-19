package com.example.ui.kot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import com.example.domain.model.KOT
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KotViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _kots = MutableStateFlow<List<KOT>>(emptyList())
    val kots = _kots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _sendResult = MutableSharedFlow<Result<KOT>>(extraBufferCapacity = 1)
    val sendResult = _sendResult.asSharedFlow()

    private val _printResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)
    val printResult = _printResult.asSharedFlow()

    init {
        loadAllKOTs()
        observeRealtimeKots()
    }

    fun loadAllKOTs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getAllKOTs()
                .onSuccess { list ->
                    _kots.value = list.sortedByDescending { it.createdAt }
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to load KOT dispatch histories"
                }
            _isLoading.value = false
        }
    }

    fun loadKOTHistory(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getKOTHistory(orderId)
                .onSuccess { list ->
                    _kots.value = list.sortedByDescending { it.createdAt }
                }
                _isLoading.value = false
        }
    }

    fun sendKOT(tableId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.sendKOT(tableId)
            _sendResult.emit(result)
            if (result.isSuccess) {
                loadAllKOTs() // Refresh history listing
            }
            _isLoading.value = false
        }
    }

    fun printKOT(kotId: String, reprint: Boolean = false, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.printKOT(kotId, reprint, reason)
            _printResult.emit(result)
        }
    }

    fun updateKotStatus(kotId: String, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateKOTStatus(kotId, status)
            if (result.isSuccess) {
                loadAllKOTs()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update status"
            }
            _isLoading.value = false
        }
    }

    private fun observeRealtimeKots() {
        viewModelScope.launch {
            repository.kotEventFlow.collect { updatedKot ->
                val currentList = _kots.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == updatedKot.id }
                if (idx != -1) {
                    currentList[idx] = updatedKot
                } else {
                    currentList.add(0, updatedKot)
                }
                _kots.value = currentList
            }
        }
    }
}
