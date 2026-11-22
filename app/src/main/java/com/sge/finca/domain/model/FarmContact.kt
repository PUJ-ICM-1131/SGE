package com.sge.finca.domain.model

/** Contacto del directorio de la finca (proveedor, veterinario, comprador...). */
data class FarmContact(
    val id: String,
    val farmId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
)
