package com.sge.finca.domain.model

/** Categoría económica de una finca. [type] indica a qué transacciones aplica. */
data class Category(
    val id: String,
    val farmId: String,
    val name: String,
    val type: TransactionType,
    val isDefault: Boolean,
)
