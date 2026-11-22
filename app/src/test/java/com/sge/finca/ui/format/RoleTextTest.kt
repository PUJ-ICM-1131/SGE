package com.sge.finca.ui.format

import com.sge.finca.domain.model.FarmRole
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleTextTest {

    @Test
    fun rolesHaveSpanishLabels() {
        assertEquals("Propietario", FarmRole.OWNER.label())
        assertEquals("Trabajador", FarmRole.WORKER.label())
    }

    @Test
    fun movementsUsesSingularAndPlural() {
        assertEquals("0 movimientos", movementsLabel(0))
        assertEquals("1 movimiento", movementsLabel(1))
        assertEquals("4 movimientos", movementsLabel(4))
    }
}
