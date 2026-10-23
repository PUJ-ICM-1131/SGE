package com.sge.finca.ui.screens.settings

import com.sge.finca.platform.biometric.BiometricAvailability

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = false,
    /** Estado del sensor de huella del dispositivo. */
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
    /** ¿El usuario actual tiene activado el inicio rápido con huella? */
    val biometricEnabled: Boolean = false,
    /** Mensaje de una sola vez para mostrar en un Snackbar. */
    val message: String? = null,
)
