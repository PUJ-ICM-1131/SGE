package com.sge.finca.domain.model

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessDateTest {

    @Test
    fun epochStartsAt1970() {
        assertEquals(0L, BusinessDate.of(1970, 1, 1))
        assertEquals(-1L, BusinessDate.of(1969, 12, 31))
    }

    @Test
    fun roundTripsThroughIsoString() {
        val day = BusinessDate.of(2026, 11, 21)
        assertEquals("2026-11-21", BusinessDate.toIsoString(day))
        assertEquals(day + 1, BusinessDate.of(2026, 11, 22))
    }

    @Test
    fun usesLocalCalendarDayNotUtc() {
        // 21 nov 2026, 22:00 en Bogotá (UTC-5) = 22 nov 03:00 UTC.
        // Para el administrador de la finca sigue siendo el 21.
        val bogota = TimeZone.getTimeZone("America/Bogota")
        val millis = 1_795_316_400_000L // 2026-11-22T03:00:00Z
        assertEquals(BusinessDate.of(2026, 11, 21), BusinessDate.fromMillis(millis, bogota))
    }
}
