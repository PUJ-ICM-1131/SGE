package com.sge.finca.domain.repository

import com.sge.finca.domain.model.FarmContact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(farmId: String): Flow<List<FarmContact>>
    suspend fun getContact(farmId: String, id: String): FarmContact?

    /** @throws IllegalArgumentException si falta el nombre. Devuelve el id generado. */
    suspend fun create(
        farmId: String,
        name: String,
        phone: String? = null,
        email: String? = null,
        notes: String? = null,
    ): String

    /** Guarda los cambios de un contacto existente. */
    suspend fun update(contact: FarmContact)

    suspend fun delete(farmId: String, id: String)
}
