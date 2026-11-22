package com.sge.finca.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncTextTest {

    @Test
    fun pendingUsesSingularAndPlural() {
        assertEquals("1 registro pendiente de sincronización", SyncText.pending(1))
        assertEquals("3 registros pendientes de sincronización", SyncText.pending(3))
    }

    @Test
    fun failedUsesSingularAndPlural() {
        assertEquals("1 registro no se pudo sincronizar", SyncText.failed(1))
        assertEquals("2 registros no se pudieron sincronizar", SyncText.failed(2))
    }
}
