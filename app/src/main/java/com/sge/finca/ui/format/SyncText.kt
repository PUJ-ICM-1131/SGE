package com.sge.finca.ui.format

/** Textos del indicador de sincronización (issue #32). */
object SyncText {

    /** "1 registro pendiente de sincronización" / "3 registros pendientes de sincronización". */
    fun pending(count: Int): String =
        if (count == 1) "1 registro pendiente de sincronización"
        else "$count registros pendientes de sincronización"

    /** "1 registro no se pudo sincronizar" / "2 registros no se pudieron sincronizar". */
    fun failed(count: Int): String =
        if (count == 1) "1 registro no se pudo sincronizar"
        else "$count registros no se pudieron sincronizar"
}
