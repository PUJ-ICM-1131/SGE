package com.sge.finca.ui.screens.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class LoginScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState

    fun updateEmail(input: String) {
        _uiState.update { it.copy(email = input) }
    }

    fun updatePassword(input: String) {
        _uiState.update { it.copy(password = input) }
    }

    fun loginButtonPress() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.update {
                it.copy(
                    showErrorMessage = true,
                    errorMessage = "Todos los campos son obligatorios",
                )
            }
        } else {
            _uiState.update { it.copy(showErrorMessage = false, navigate = true) }
        }
    }
}
