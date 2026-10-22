package com.sge.finca.domain.model

/**
 * Animal listo para mostrar: su ficha + dónde está AHORA y si se está moviendo.
 * Es lo que consumen el mapa y la lista; así ambas pantallas muestran lo mismo.
 */
data class TrackedAnimal(
    val animal: Animal,
    val latitude: Double,
    val longitude: Double,
    val isMoving: Boolean = false,
)
