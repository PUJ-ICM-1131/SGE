package com.sge.finca.platform.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Estado del sensor biométrico del dispositivo (sólo huella/biometría "fuerte"). */
enum class BiometricAvailability {
    /** Hay sensor y al menos una huella registrada. */
    AVAILABLE,

    /** El dispositivo no tiene sensor biométrico. */
    NO_HARDWARE,

    /** Hay sensor pero el usuario no ha registrado ninguna huella. */
    NONE_ENROLLED,

    /** Sensor ocupado, requiere actualización de seguridad u otro estado no usable. */
    UNAVAILABLE,
}

/** Resultado final de mostrar el BiometricPrompt. */
sealed interface BiometricResult {
    /** La huella fue reconocida. */
    data object Success : BiometricResult

    /** El usuario canceló o pulsó el botón negativo ("Usar contraseña"). */
    data object Cancelled : BiometricResult

    /** Error no recuperable (bloqueo por intentos, sensor no disponible, etc.). */
    data class Failed(val message: String) : BiometricResult
}

/**
 * Verifica la disponibilidad del sensor y muestra el BiometricPrompt.
 *
 * La biometría NO reemplaza a Firebase Auth: sólo confirma que quien tiene el
 * dispositivo es su dueño. No se guarda ninguna contraseña ni credencial aquí.
 */
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }

    /**
     * Muestra el prompt del sistema. [onResult] se llama una sola vez, en el hilo principal,
     * cuando el flujo termina (éxito, cancelación o error). Un intento fallido aislado
     * (huella no reconocida) NO termina el flujo: el sistema deja reintentar.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = DEFAULT_TITLE,
        subtitle: String = DEFAULT_SUBTITLE,
        negativeText: String = DEFAULT_NEGATIVE_TEXT,
        onResult: (BiometricResult) -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> BiometricResult.Cancelled

                    else -> BiometricResult.Failed(errString.toString())
                }
                onResult(result)
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(promptInfo)
    }

    companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

        const val DEFAULT_TITLE = "Inicia sesión en SGE"
        const val DEFAULT_SUBTITLE = "Usa tu huella dactilar"
        const val DEFAULT_NEGATIVE_TEXT = "Usar contraseña"
    }
}
