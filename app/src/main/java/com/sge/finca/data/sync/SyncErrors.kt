package com.sge.finca.data.sync

import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Separa los errores de Firestore en dos grupos (issue #32):
 * - Definitivos: reintentar daría el mismo error (Security Rules, datos inválidos). La fila
 *   queda FAILED y no se reintenta sola.
 * - Transitorios: red, tiempo de espera, servidor no disponible, sesión vencida. La fila sigue
 *   PENDING y WorkManager reintenta más tarde.
 */
object SyncErrors {

    private val PERMANENT_CODES = setOf(
        "PERMISSION_DENIED",
        "INVALID_ARGUMENT",
        "FAILED_PRECONDITION",
        "OUT_OF_RANGE",
    )

    fun isPermanent(error: Throwable): Boolean = when (error) {
        is FirebaseFirestoreException -> isPermanentCode(error.code.name)
        // Ruta inválida (id vacío o con "/"): nunca va a funcionar.
        is IllegalArgumentException -> true
        else -> false
    }

    fun isPermanentCode(codeName: String): Boolean = codeName in PERMANENT_CODES
}
