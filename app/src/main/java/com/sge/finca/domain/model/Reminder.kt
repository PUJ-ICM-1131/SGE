package com.sge.finca.domain.model

/** Recordatorio de la finca (vacuna, pago, visita...). [dueAt] es fecha y hora en milisegundos. */
data class Reminder(
    val id: String,
    val farmId: String,
    val title: String,
    val description: String? = null,
    val dueAt: Long,
    val isCompleted: Boolean = false,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
)
