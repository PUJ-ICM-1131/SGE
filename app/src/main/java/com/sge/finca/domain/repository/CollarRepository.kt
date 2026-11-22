package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Animal

/**
 * Collares GPS del proveedor externo. Cada animal lleva un collar cuyo centro de pastoreo
 * (anclaje) es el punto donde se registró.
 */
interface CollarRepository {

    /**
     * Crea el collar de los animales que aún no tienen uno y actualiza nombre y anclaje
     * de los que cambiaron. Es idempotente: llamarla varias veces no duplica collares.
     * @throws java.io.IOException o retrofit2.HttpException si el proveedor no responde.
     */
    suspend fun ensureCollars(animals: List<Animal>)

    /**
     * Elimina en segundo plano el collar del animal (al borrar el animal).
     * No bloquea la pantalla; si no hay red, el collar queda huérfano en el proveedor.
     */
    fun releaseCollar(animalId: String)
}
