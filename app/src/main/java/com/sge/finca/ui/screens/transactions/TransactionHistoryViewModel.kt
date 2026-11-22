package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.usecase.ObserveFarmCategoriesUseCase
import com.sge.finca.platform.sensors.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeFarmCategories: ObserveFarmCategoriesUseCase,
    private val transactionRepository: TransactionRepository,
    private val shakeDetector: ShakeDetector,
) : ViewModel() {

    /** Los filtros se guardan en el SavedStateHandle: sobreviven a la rotación y a que el sistema cierre la app. */
    private val filter = MutableStateFlow(restoreFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionHistoryUiState> =
        combine(observeFarmCategories(), filter) { farm, filter -> farm to filter }
            .flatMapLatest { (farm, filter) ->
                if (farm == null) {
                    flowOf(TransactionHistoryUiState(isLoading = false, noFarm = true, filter = filter))
                } else {
                    // Room aplica tipo, fechas y categoría en la consulta y avisa cada vez que algo cambia.
                    transactionRepository.observeTransactions(farm.farmId, filter.toTransactionFilter())
                        .map { transactions ->
                            TransactionHistoryUiState(
                                isLoading = false,
                                filter = filter,
                                categories = farm.categories.filter { filter.type == null || it.type == filter.type },
                                transactions = transactions,
                                summary = HistorySummary.of(transactions),
                            )
                        }
                }
            }
            .catch { emit(TransactionHistoryUiState(isLoading = false, errorMessage = "No se pudo cargar el historial")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionHistoryUiState())

    // ---------- Filtros ----------

    fun onTypeSelected(type: TransactionType?) = updateFilter { current ->
        // Si la categoría elegida no es del nuevo tipo, se quita para no dejar un filtro sin resultados.
        val category = uiState.value.selectedCategory
        val keepCategory = type == null || category == null || category.type == type
        current.copy(type = type, categoryId = current.categoryId.takeIf { keepCategory })
    }

    fun onPeriodSelected(period: HistoryPeriod) = updateFilter { it.copy(period = period) }

    /** Rango personalizado elegido en el calendario (epoch days, inclusivos). */
    fun onCustomRangeSelected(from: Long, to: Long) = updateFilter {
        it.copy(period = HistoryPeriod.CUSTOM, customFrom = minOf(from, to), customTo = maxOf(from, to))
    }

    fun onCategorySelected(categoryId: String?) = updateFilter { it.copy(categoryId = categoryId) }

    fun onClearFilters() = updateFilter { HistoryFilter() }

    private fun updateFilter(change: (HistoryFilter) -> HistoryFilter) {
        val updated = change(filter.value)
        filter.value = updated
        savedStateHandle[KEY_TYPE] = updated.type?.name
        savedStateHandle[KEY_PERIOD] = updated.period.name
        savedStateHandle[KEY_FROM] = updated.customFrom
        savedStateHandle[KEY_TO] = updated.customTo
        savedStateHandle[KEY_CATEGORY] = updated.categoryId
    }

    private fun restoreFilter() = HistoryFilter(
        type = savedStateHandle.get<String>(KEY_TYPE)?.let { name -> TransactionType.entries.firstOrNull { it.name == name } },
        period = savedStateHandle.get<String>(KEY_PERIOD)
            ?.let { name -> HistoryPeriod.entries.firstOrNull { it.name == name } }
            ?: HistoryPeriod.MONTH,
        customFrom = savedStateHandle.get<Long>(KEY_FROM),
        customTo = savedStateHandle.get<Long>(KEY_TO),
        categoryId = savedStateHandle.get<String>(KEY_CATEGORY),
    )

    // ---------- Agitar para registrar (#13) ----------

    fun startShakeDetection(onShake: () -> Unit): Boolean = shakeDetector.start(onShake)

    fun stopShakeDetection() = shakeDetector.stop()

    private companion object {
        const val KEY_TYPE = "history_type"
        const val KEY_PERIOD = "history_period"
        const val KEY_FROM = "history_from"
        const val KEY_TO = "history_to"
        const val KEY_CATEGORY = "history_category"
    }
}
