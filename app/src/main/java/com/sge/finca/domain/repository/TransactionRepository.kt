package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionFilter
import com.sge.finca.domain.model.TransactionInput
import kotlinx.coroutines.flow.Flow

/**
 * Todas las escrituras verifican el rol del usuario (issue #28) y lanzan [SecurityException]
 * si no tiene permiso; ver [com.sge.finca.domain.model.Permission].
 */
interface TransactionRepository {
    /** Historial de la finca, del más reciente al más antiguo. */
    fun observeTransactions(
        farmId: String,
        filter: TransactionFilter = TransactionFilter(),
    ): Flow<List<Transaction>>

    suspend fun getTransaction(farmId: String, id: String): Transaction?

    /** Una sola transacción; emite null mientras no esté en este celular. */
    fun observeTransaction(farmId: String, id: String): Flow<Transaction?>

    /**
     * Trae de Firestore una transacción que todavía no está en Room (p. ej. la registró otro
     * miembro y se abrió desde el aviso push antes de que el listener de #32 la bajara).
     * No pisa una copia local ni revive una borrada aquí que aún no se ha enviado.
     * @return true si quedó en Room; false si no existe en el servidor.
     * @throws Exception sin red, sin permiso o si el servidor no responde a tiempo.
     */
    suspend fun pullFromRemote(farmId: String, id: String): Boolean

    /**
     * Valida y guarda. Devuelve el id generado.
     * @throws IllegalArgumentException si los datos no son válidos (el mensaje es apto para la UI).
     */
    suspend fun create(farmId: String, input: TransactionInput): String

    /** @throws IllegalArgumentException si no existe o los datos no son válidos. */
    suspend fun update(farmId: String, id: String, input: TransactionInput)

    suspend fun delete(farmId: String, id: String)
}
