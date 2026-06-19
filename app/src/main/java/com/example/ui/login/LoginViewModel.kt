package com.example.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: CaptainRepository) : ViewModel() {
    private val _restaurantCode = MutableStateFlow("")
    val restaurantCode = _restaurantCode.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val loginApiLog = repository.loginApiLog

    fun onRestaurantCodeChange(code: String) {
        _restaurantCode.value = code
    }

    fun onUsernameChange(name: String) {
        _username.value = name
    }

    fun onPasswordChange(pass: String) {
        _password.value = pass
    }

    fun login() {
        val code = _restaurantCode.value.trim()
        val user = _username.value.trim()
        val pass = _password.value.trim()

        if (code.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            _uiState.value = LoginUiState.Error("Please fill in all standard fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.login(code, user, pass)
                .onSuccess {
                    _uiState.value = LoginUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Authentication failed")
                }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}
