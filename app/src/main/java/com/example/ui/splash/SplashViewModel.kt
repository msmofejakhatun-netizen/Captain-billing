package com.example.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CaptainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val repository: CaptainRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            try {
                val isLoggedIn = repository.autoLogin()
                if (isLoggedIn) {
                    _uiState.value = SplashUiState.Authenticated
                } else {
                    _uiState.value = SplashUiState.Unauthenticated
                }
            } catch (e: Exception) {
                _uiState.value = SplashUiState.Unauthenticated
            }
        }
    }
}

sealed interface SplashUiState {
    object Idle : SplashUiState
    object Loading : SplashUiState
    object Authenticated : SplashUiState
    object Unauthenticated : SplashUiState
}
