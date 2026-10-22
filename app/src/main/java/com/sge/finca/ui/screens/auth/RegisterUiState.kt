package com.sge.finca.ui.screens.auth

data class RegisterUiState(
    val step: Int = 0,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val farmName: String = "",
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
