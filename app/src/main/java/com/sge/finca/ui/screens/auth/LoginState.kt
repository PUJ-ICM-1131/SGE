package com.sge.finca.ui.screens.auth

/**
 * Estado (modelo) de la pantalla de login.
 * Todos los campos tienen valor por defecto.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String = "",
    val showErrorMessage: Boolean = false,
    val navigate: Boolean = false,
)
