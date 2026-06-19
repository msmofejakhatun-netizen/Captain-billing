package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.printer.PrinterSettings
import com.example.data.repository.CaptainRepository
import com.example.domain.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: CaptainRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _logoutFinished = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val logoutFinished = _logoutFinished.asSharedFlow()

    // Printer states
    private val _printerSettings = MutableStateFlow(PrinterSettings())
    val printerSettings = _printerSettings.asStateFlow()

    private val _printerStatus = MutableStateFlow("Disconnected")
    val printerStatus = _printerStatus.asStateFlow()

    private val _printResult = MutableSharedFlow<Result<Unit>>(extraBufferCapacity = 1)
    val printResult = _printResult.asSharedFlow()

    init {
        loadProfile()
        loadPrinterSettings()
        checkPrinterStatus()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _currentUser.value = repository.getCurrentUser()
        }
    }

    fun loadPrinterSettings() {
        _printerSettings.value = repository.printerManager.getSettings()
    }

    fun savePrinterSettings(settings: PrinterSettings) {
        repository.printerManager.saveSettings(settings)
        _printerSettings.value = settings
        // Check connectivity status with new settings
        checkPrinterStatus()
    }

    fun checkPrinterStatus() {
        viewModelScope.launch {
            _printerStatus.value = "Checking..."
            val online = repository.printerManager.checkStatus()
            _printerStatus.value = if (online) "Connected" else "Disconnected"
        }
    }

    fun printTestPage() {
        viewModelScope.launch {
            val settings = _printerSettings.value
            val bytes = repository.printerManager.generateTestPageBytes(settings.paperWidth)
            val result = repository.printerManager.printBytes(bytes)
            _printResult.emit(result)
            // Re-check status
            val online = repository.printerManager.checkStatus()
            _printerStatus.value = if (online) "Connected" else "Disconnected"
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _logoutFinished.emit(true)
        }
    }
}
