package com.sge.finca.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddockGeometryTest {

    // Cuadrado de 100 m x 100 m cerca de Bogotá: 1° de latitud ≈ 111 195 m.
    private val lat0 = 4.6
    private val lng0 = -74.1
    private val dLat = 100.0 / 111_195.0
    private val dLng = 100.0 / (111_195.0 * Math.cos(Math.toRadians(lat0)))

    private val square = listOf(
        Coordinate(lat0, lng0),
        Coordinate(lat0, lng0 + dLng),
        Coordinate(lat0 + dLat, lng0 + dLng),
        Coordinate(lat0 + dLat, lng0),
    )

    @Test
    fun squareOf100mHasAbout10000m2() {
        assertEquals(10_000.0, PaddockGeometry.areaM2(square), 10_000.0 * 0.05)
    }

    @Test
    fun areaIgnoresOrientation() {
        assertEquals(PaddockGeometry.areaM2(square), PaddockGeometry.areaM2(square.reversed()), 0.001)
    }

    @Test
    fun lessThanThreePointsHasNoArea() {
        assertEquals(0.0, PaddockGeometry.areaM2(square.take(2)), 0.0)
    }

    @Test
    fun containsPointsInsideButNotOutside() {
        val center = Coordinate(lat0 + dLat / 2, lng0 + dLng / 2)
        val east = Coordinate(lat0 + dLat / 2, lng0 + dLng * 1.5)
        val north = Coordinate(lat0 + dLat * 1.5, lng0 + dLng / 2)
        assertTrue(PaddockGeometry.contains(square, center))
        assertFalse(PaddockGeometry.contains(square, east))
        assertFalse(PaddockGeometry.contains(square, north))
    }

    @Test
    fun containsNearTheBorder() {
        val justInside = Coordinate(lat0 + dLat / 2, lng0 + dLng * 0.99)
        val justOutside = Coordinate(lat0 + dLat / 2, lng0 + dLng * 1.01)
        assertTrue(PaddockGeometry.contains(square, justInside))
        assertFalse(PaddockGeometry.contains(square, justOutside))
    }

    @Test
    fun containsWorksForNonConvexShapes() {
        // "L": el hueco de la esquina superior derecha queda fuera.
        val l = listOf(
            Coordinate(lat0, lng0),
            Coordinate(lat0, lng0 + 2 * dLng),
            Coordinate(lat0 + dLat, lng0 + 2 * dLng),
            Coordinate(lat0 + dLat, lng0 + dLng),
            Coordinate(lat0 + 2 * dLat, lng0 + dLng),
            Coordinate(lat0 + 2 * dLat, lng0),
        )
        assertTrue(PaddockGeometry.contains(l, Coordinate(lat0 + dLat * 1.5, lng0 + dLng * 0.5)))
        assertFalse(PaddockGeometry.contains(l, Coordinate(lat0 + dLat * 1.5, lng0 + dLng * 1.5)))
    }

    @Test
    fun routeThatEndsNearItsStartIsClosed() {
        val walked = square + Coordinate(lat0 + dLat * 0.1, lng0)  // vuelve a ~11 m del inicio
        assertTrue(PaddockGeometry.isClosedArea(walked))
    }

    @Test
    fun routeThatEndsFarAwayIsNotClosed() {
        assertFalse(PaddockGeometry.isClosedArea(square.take(3)))
    }

    @Test
    fun goingAndComingBackOnTheSamePathIsNotAnArea() {
        val outAndBack = listOf(
            Coordinate(lat0, lng0),
            Coordinate(lat0 + dLat, lng0),
            Coordinate(lat0 + dLat * 2, lng0),
            Coordinate(lat0 + dLat, lng0),
            Coordinate(lat0, lng0),
        )
        assertFalse(PaddockGeometry.isClosedArea(outAndBack))
    }
}
