package com.sge.finca.domain.model

data class DefaultCategory(
    val name: String,
    val type: TransactionType
)

object DefaultCategories {
    val items = listOf(
        DefaultCategory("Venta de leche", TransactionType.INCOME),
        DefaultCategory("Venta de ganado", TransactionType.INCOME),
        DefaultCategory("Otros ingresos", TransactionType.INCOME),
        DefaultCategory("Alimentación", TransactionType.EXPENSE),
        DefaultCategory("Sanidad", TransactionType.EXPENSE),
        DefaultCategory("Mano de obra", TransactionType.EXPENSE),
        DefaultCategory("Mantenimiento", TransactionType.EXPENSE),
        DefaultCategory("Servicios", TransactionType.EXPENSE),
        DefaultCategory("Transporte", TransactionType.EXPENSE),
        DefaultCategory("Otros egresos", TransactionType.EXPENSE)
    )
}
