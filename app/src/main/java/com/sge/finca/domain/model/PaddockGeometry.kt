package com.sge.finca.domain.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Cálculos de los potreros (#71) en Kotlin puro, sin librerías de mapas. */
object PaddockGeometry {

    /** Distancia máxima entre el inicio y el fin de un recorrido para considerarlo cerrado. */
    const val CLOSE_TOLERANCE_M = 20.0

    /** Un recorrido de ida y vuelta por el mismo camino cierra, pero no encierra un potrero. */
    const val MIN_AREA_M2 = 100.0

    private const val EARTH_RADIUS_M = 6_371_009.0

    /**
     * Área geodésica en m² del polígono (el mismo método que SphericalUtil.computeArea de
     * Google): suma de triángulos sobre la esfera, válido aunque el polígono no sea convexo.
     */
    fun areaM2(points: List<Coordinate>): Double {
        if (points.size < 3) return 0.0
        var total = 0.0
        val last = points.last()
        var prevTanLat = tan((PI / 2 - Math.toRadians(last.latitude)) / 2)
        var prevLng = Math.toRadians(last.longitude)
        for (point in points) {
            val tanLat = tan((PI / 2 - Math.toRadians(point.latitude)) / 2)
            val lng = Math.toRadians(point.longitude)
            val deltaLng = lng - prevLng
            val t = tanLat * prevTanLat
            total += 2 * atan2(t * sin(deltaLng), 1 + t * cos(deltaLng))
            prevTanLat = tanLat
            prevLng = lng
        }
        return abs(total * EARTH_RADIUS_M * EARTH_RADIUS_M)
    }

    /**
     * ¿[point] está dentro del polígono? Cruce de rayos sobre lat/lng: exacto para el tamaño de
     * un potrero (no cruza el antimeridiano ni los polos).
     */
    fun contains(points: List<Coordinate>, point: Coordinate): Boolean {
        if (points.size < 3) return false
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val a = points[i]
            val b = points[j]
            if ((a.latitude > point.latitude) != (b.latitude > point.latitude)) {
                val crossLng = (b.longitude - a.longitude) *
                    (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude
                if (point.longitude < crossLng) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** El recorrido terminó cerca de donde empezó y encierra un área de verdad. */
    fun isClosedArea(points: List<Coordinate>): Boolean =
        points.size >= 3 &&
            distanceM(points.first(), points.last()) <= CLOSE_TOLERANCE_M &&
            areaM2(points) >= MIN_AREA_M2

    /** Distancia sobre la superficie (haversine), en metros. */
    fun distanceM(a: Coordinate, b: Coordinate): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}
