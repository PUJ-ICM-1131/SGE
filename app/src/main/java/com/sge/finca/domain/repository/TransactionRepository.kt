package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionFilter
import com.sge.finca.domain.model.TransactionInput
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    /** Historial de la finca, del más reciente al más antiguo. */
    fun observeTransactions(
        farmId: String,
        filter: TransactionFilter = TransactionFilter(),
    ): Flow<List<Transaction>>

    suspend fun getTransaction(farmId: String, id: String): Transaction?

    /**
     * Valida y guarda. Devuelve el id generado.
     * @throws IllegalArgumentException si los datos no son válidos (el mensaje es apto para la UI).
     */
    suspend fun create(farmId: String, input: TransactionInput): String

    /** @throws IllegalArgumentException si no existe o los datos no son válidos. */
    suspend fun update(farmId: String, id: String, input: TransactionInput)

    suspend fun delete(farmId: String, id: String)
}
