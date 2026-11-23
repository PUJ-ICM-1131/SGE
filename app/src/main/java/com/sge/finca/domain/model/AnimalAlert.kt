package com.sge.finca.domain.model

enum class AlertStatus { OPEN, RESOLVED }

/**
 * Alerta de una vaca fuera de todos los potreros (#73). La crea y la resuelve el servidor
 * (sge-notifier) en farms/{farmId}/alerts/{id}; la app solo la lee y marca el reenvío.
 */
data class AnimalAlert(
    val id: String,
    val farmId: String,
    val animalId: String,
    val animalName: String,
    /** Dónde estaba la vaca cuando se detectó la salida. */
    val position: Coordinate,
    val status: AlertStatus,
    val createdAt: Long,
    val forwardedAt: Long? = null,
    val resolvedAt: Long? = null,
)
