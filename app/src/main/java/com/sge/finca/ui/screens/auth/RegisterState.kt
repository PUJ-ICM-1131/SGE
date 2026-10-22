package com.sge.finca.ui.screens.auth

/**
 * Estado (modelo) de la pantalla de registro.
 * step: 0 = datos de la cuenta, 1 = configurar finca.
 */
data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val farmName: String = "",
    val inviteCode: String = "",
    val step: Int = 0,
    val errorMessage: String = "",
    val showErrorMessage: Boolean = false,
    val isLoading: Boolean = false,
    val navigate: Boolean = false,
)