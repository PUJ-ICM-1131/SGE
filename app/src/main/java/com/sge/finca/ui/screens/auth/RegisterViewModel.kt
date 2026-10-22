package com.sge.finca.ui.screens.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onFarmNameChanged(value: String) {
        _uiState.update { it.copy(farmName = value, errorMessage = null) }
    }

    fun onInviteCodeChanged(value: String) {
        _uiState.update { it.copy(inviteCode = value, errorMessage = null) }
    }

    fun onContinueToFarmSetup() {
        _uiState.update { it.copy(step = 1) }
    }

    fun onCreateFarm() {
        // TODO: validate and call Firebase Auth + Firestore
    }

    fun onJoinFarm() {
        // TODO: validate invite code and join farm
    }
}
