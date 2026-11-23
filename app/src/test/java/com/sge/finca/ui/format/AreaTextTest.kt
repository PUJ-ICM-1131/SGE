package com.sge.finca.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class AreaTextTest {

    @Test
    fun smallAreasOnlyInSquareMeters() {
        assertEquals("0 m²", AreaText.format(0.0))
        assertEquals("850 m²", AreaText.format(849.6))
        assertEquals("1.250 m²", AreaText.format(1250.4))
    }

    @Test
    fun fromOneHectareAddsHectares() {
        assertEquals("10.000 m² (1 ha)", AreaText.format(10_000.0))
        assertEquals("23.456 m² (2,35 ha)", AreaText.format(23_456.0))
    }
}
