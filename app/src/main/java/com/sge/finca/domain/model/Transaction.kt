package com.sge.finca.domain.model

/**
 * Ingreso o egreso de la finca.
 *
 * - [amountCents]: el monto en centavos (entero) para no perder precisión con Double.
 *   $25.000 COP = 2_500_000.
 * - [dateEpochDay]: la fecha del movimiento (la que el usuario elige), separada de
 *   [createdAt]/[updatedAt], que son marcas de tiempo técnicas. Ver [BusinessDate].
 * - [categoryName]: copia del nombre de la categoría al momento de guardar, para que el
 *   historial se lea igual aunque luego la categoría se renombre o se borre.
 */
data class Transaction(
    val id: String,
    val farmId: String,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: String,
    val categoryName: String,
    val dateEpochDay: Long,
    val note: String? = null,
    val paymentMethod: String? = null,
    val photoUri: String? = null,
    val remotePhotoUrl: String? = null,
    val createdBy: String,
    val createdByName: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Datos que escribe el usuario al crear o editar una transacción. */
data class TransactionInput(
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: String,
    val dateEpochDay: Long,
    val note: String? = null,
    val paymentMethod: String? = null,
    val photoUri: String? = null,
)

/** Filtros del historial. Un campo en null significa "sin filtrar por eso". */
data class TransactionFilter(
    val fromEpochDay: Long? = null,
    val toEpochDay: Long? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null,
)
