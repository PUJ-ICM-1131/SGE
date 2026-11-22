package com.sge.finca.domain.model

/** Estado de sincronización de un registro, visto desde la UI (issue #32). */
enum class SyncState {
    /** Ya está en Firestore. */
    SYNCED,

    /** Guardado en el celular; se sube cuando haya conexión. */
    PENDING,

    /** Firestore lo rechazó (p. ej. permisos). No se reintenta solo. */
    FAILED,
}

/** Cuántos registros de la finca faltan por sincronizar. */
data class SyncSummary(
    val pending: Int = 0,
    val failed: Int = 0,
) {
    val isSynced: Boolean get() = pending == 0 && failed == 0
}
