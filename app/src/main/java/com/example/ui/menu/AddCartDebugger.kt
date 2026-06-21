package com.example.ui.menu

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AddCartDebugInfo(
    val endpoint: String = "",
    val fullUrl: String = "",
    val httpMethod: String = "",
    val requestBodyJson: String = "",
    val statusCode: Int? = null,
    val rawResponseBody: String = "",
    val exceptionStacktrace: String = ""
)

object AddCartDebugger {
    private val _debugFlow = MutableStateFlow<AddCartDebugInfo?>(null)
    val debugFlow = _debugFlow.asStateFlow()

    fun updateDebug(info: AddCartDebugInfo) {
        _debugFlow.value = info
    }

    fun clear() {
        _debugFlow.value = null
    }
}
