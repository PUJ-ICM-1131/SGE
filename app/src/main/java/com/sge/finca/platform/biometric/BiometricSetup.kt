package com.sge.finca.platform.biometric

import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

/**
 * Coordina la activación/desactivación del inicio rápido con huella.
 * La comparten Login, Registro y Configuración para no duplicar reglas.
 */
class BiometricSetup @Inject constructor(
    private val helper: BiometricHelper,
    private val preferences: BiometricPreferences,
) {

    fun availability(): BiometricAvailability = helper.availability()

    fun isEnabledFor(uid: String): Boolean = preferences.isEnabledFor(uid)

    /**
     * Se ofrece activar la huella sólo si hay sensor con huellas registradas, el usuario no la
     * tiene activada y nunca se le ha preguntado antes (se pregunta una vez por cuenta; después
     * puede cambiarlo desde Configuración).
     */
    fun shouldOfferActivation(uid: String): Boolean =
        helper.availability() == BiometricAvailability.AVAILABLE &&
            !preferences.isEnabledFor(uid) &&
            !preferences.wasOfferAnswered(uid)

    fun declineOffer(uid: String) {
        preferences.markOfferAnswered(uid)
    }

    /**
     * Pide una huella para confirmar que el sensor funciona y, si es correcta, activa el inicio
     * rápido. [onDone] recibe true si quedó activado.
     */
    fun enableWithConfirmation(
        activity: FragmentActivity,
        uid: String,
        onDone: (enabled: Boolean) -> Unit,
    ) {
        helper.authenticate(
            activity = activity,
            title = "Activar inicio rápido",
            subtitle = "Confirma con tu huella dactilar",
            negativeText = "Cancelar",
        ) { result ->
            preferences.markOfferAnswered(uid)
            val enabled = result is BiometricResult.Success
            if (enabled) preferences.enableFor(uid)
            onDone(enabled)
        }
    }

    fun disable() {
        preferences.disable()
    }
}
