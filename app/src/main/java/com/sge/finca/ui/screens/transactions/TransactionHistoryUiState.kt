package com.sge.finca.ui.screens.transactions

import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.Transaction

data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val noFarm: Boolean = false,
    val errorMessage: String? = null,
    val filter: HistoryFilter = HistoryFilter(),
    /** Categorías que se pueden elegir en el filtro (si hay tipo elegido, solo las de ese tipo). */
    val categories: List<Category> = emptyList(),
    /** Ya filtradas, de la más reciente a la más antigua. */
    val transactions: List<Transaction> = emptyList(),
    val summary: HistorySummary = HistorySummary(),
) {
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == filter.categoryId }
}
