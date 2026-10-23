package com.sge.finca.platform.biometric

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias locales del inicio rápido con huella.
 *
 * Se guarda el UID del usuario que activó la huella (no un simple booleano), para que si otra
 * cuenta inicia sesión en el mismo dispositivo no herede el inicio con huella de la anterior.
 * No se guarda ninguna contraseña ni credencial.
 */
@Singleton
class BiometricPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** ¿Este usuario tiene activado el inicio rápido con huella en este dispositivo? */
    fun isEnabledFor(uid: String): Boolean = prefs.getString(KEY_ENABLED_UID, null) == uid

    fun enableFor(uid: String) {
        prefs.edit { putString(KEY_ENABLED_UID, uid) }
    }

    fun disable() {
        prefs.edit { remove(KEY_ENABLED_UID) }
    }

    /** ¿Ya se le preguntó a este usuario si quiere activar la huella (aceptó o no)? */
    fun wasOfferAnswered(uid: String): Boolean = prefs.getString(KEY_OFFER_ANSWERED_UID, null) == uid

    fun markOfferAnswered(uid: String) {
        prefs.edit { putString(KEY_OFFER_ANSWERED_UID, uid) }
    }

    private companion object {
        const val FILE_NAME = "biometric_prefs"
        const val KEY_ENABLED_UID = "enabled_uid"
        const val KEY_OFFER_ANSWERED_UID = "offer_answered_uid"
    }
}
