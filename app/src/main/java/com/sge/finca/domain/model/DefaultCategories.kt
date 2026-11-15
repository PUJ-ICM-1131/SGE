package com.sge.finca.domain.model

data class DefaultCategory(
    val name: String,
    val type: String
)

object DefaultCategories {
    val items = listOf(
        DefaultCategory("Venta de leche", "INCOME"),
        DefaultCategory("Venta de ganado", "INCOME"),
        DefaultCategory("Otros ingresos", "INCOME"),
        DefaultCategory("Alimentación", "EXPENSE"),
        DefaultCategory("Sanidad", "EXPENSE"),
        DefaultCategory("Mano de obra", "EXPENSE"),
        DefaultCategory("Mantenimiento", "EXPENSE"),
        DefaultCategory("Servicios", "EXPENSE"),
        DefaultCategory("Transporte", "EXPENSE"),
        DefaultCategory("Otros egresos", "EXPENSE")
    )
}