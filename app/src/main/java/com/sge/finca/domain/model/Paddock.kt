package com.sge.finca.domain.model

/** Punto geográfico en grados, sin depender del SDK de mapas. */
data class Coordinate(val latitude: Double, val longitude: Double)

/**
 * Potrero de la finca (#71): el área que se encerró al recorrer su lindero en una inspección.
 * Vive en Firestore en farms/{farmId}/paddocks/{id}; el notifier de alertas (#73) lee ese esquema.
 */
data class Paddock(
    val id: String,
    val farmId: String,
    val name: String,
    /** Vértices en orden; el último se une con el primero. */
    val points: List<Coordinate>,
    val areaM2: Double,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
)
