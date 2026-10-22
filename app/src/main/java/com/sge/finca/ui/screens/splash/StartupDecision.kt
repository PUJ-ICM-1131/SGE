package com.sge.finca.ui.screens.splash

import com.sge.finca.platform.biometric.BiometricAvailability
import com.sge.finca.ui.screens.auth.AuthNotice

/** A dónde debe ir la app al abrirse. */
enum class StartupDestination { LOGIN, BIOMETRIC_PROMPT, HOME }

/** Destino de arranque y, si hace falta, un aviso para mostrar en el Login. */
data class StartupPlan(
    val destination: StartupDestination,
    val notice: AuthNotice? = null,
)

/**
 * Regla de arranque, sin dependencias de Android para poder probarla con tests unitarios:
 *
 * - Sin sesión de Firebase: Login.
 * - Con sesión y sin huella activada: Home (comportamiento anterior).
 * - Con sesión y huella activada: prompt de huella si hay sensor con huellas registradas;
 *   si no, Login con un aviso que explica por qué.
 */
object StartupDecision {

    fun decide(
        hasSession: Boolean,
        biometricEnabled: Boolean,
        availability: BiometricAvailability,
    ): StartupPlan = when {
        !hasSession -> StartupPlan(StartupDestination.LOGIN)
        !biometricEnabled -> StartupPlan(StartupDestination.HOME)
        availability == BiometricAvailability.AVAILABLE ->
            StartupPlan(StartupDestination.BIOMETRIC_PROMPT)
        availability == BiometricAvailability.NONE_ENROLLED ->
            StartupPlan(StartupDestination.LOGIN, AuthNotice.NO_FINGERPRINTS_ENROLLED)
        else ->
            StartupPlan(StartupDestination.LOGIN, AuthNotice.BIOMETRIC_UNAVAILABLE)
    }
}
