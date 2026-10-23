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
    /** Aviso informativo (no es un error), p. ej. "Tu sesión expiró". Vacío si no hay. */
    val notice: String = "",
    /** true mientras se muestra el diálogo "¿Activar inicio rápido con huella?". */
    val showBiometricOffer: Boolean = false,
)
