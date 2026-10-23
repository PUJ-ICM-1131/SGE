package com.sge.finca.domain.model

/**
 * Posición en vivo de un animal (la entregará la API de rastreo en el futuro).
 * Hoy no hay fuente real, así que los animales se muestran donde fueron registrados.
 */
data class AnimalLocation(
    val animalId: String,
    val latitude: Double,
    val longitude: Double,
    /** Velocidad en metros por segundo; 0 si está quieto. */
    val speedMetersPerSecond: Float = 0f,
    val timestamp: Long = 0L,
)
