package com.sge.finca.ui.screens.auth

import javax.inject.Inject
import javax.inject.Singleton

/** Avisos que se muestran en el Login cuando el usuario llega ahí sin haber escrito nada. */
enum class AuthNotice(val message: String) {
    NO_FINGERPRINTS_ENROLLED(
        "No hay huellas registradas en este dispositivo. Inicia sesión con tu correo y contraseña.",
    ),
    BIOMETRIC_UNAVAILABLE(
        "La huella no está disponible en este momento. Inicia sesión con tu correo y contraseña.",
    ),
    BIOMETRIC_FAILED(
        "No se pudo verificar la huella. Inicia sesión con tu correo y contraseña.",
    ),
    SESSION_EXPIRED(
        "Tu sesión expiró. Inicia sesión de nuevo con tu correo y contraseña.",
    ),
}

/**
 * Buzón de un solo uso entre el Splash y el Login: quien decide ir al Login deja el aviso
 * con [post] y el Login lo lee y lo borra con [consume]. Evita cambiar las rutas de navegación.
 */
@Singleton
class AuthNoticeHolder @Inject constructor() {

    private var pending: AuthNotice? = null

    fun post(notice: AuthNotice) {
        pending = notice
    }

    fun consume(): AuthNotice? {
        val notice = pending
        pending = null
        return notice
    }
}
