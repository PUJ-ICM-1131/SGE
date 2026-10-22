package com.sge.finca.domain.model

data class Animal(
    val id: String = "",
    val name: String = "",
    val notes: String = "",
    /** URI local (FileProvider) de la foto guardada en almacenamiento privado. */
    val photoUri: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAccuracy: Float? = null,
    val createdAt: Long = 0L,
    /** Apariencia de la vaca (id de CowVariant). */
    val variant: Int = 0,
)
