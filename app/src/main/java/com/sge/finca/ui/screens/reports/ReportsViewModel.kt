package com.sge.finca.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.FinancialReportCalculator
import com.sge.finca.domain.model.ReportPeriod
import com.sge.finca.domain.model.TransactionFilter
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportsViewModel @Inject constructor(
    userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow("Mes")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsUiState> =
        combine(
            userRepository.observeCurrentFarmId(),
            selectedPeriod,
        ) { farmId, period -> farmId to period }
            .flatMapLatest { (farmId, period) ->
                if (farmId == null) {
                    flowOf(
                        ReportsUiState(
                            selectedPeriod = period,
                            noFarm = true,
                        )
                    )
                } else {
                    val reportPeriod = when (period) {
                        "Semana" -> ReportPeriod.WEEK
                        "Año" -> ReportPeriod.YEAR
                        else -> ReportPeriod.MONTH
                    }
                    val range = reportPeriod.rangeFor()

                    transactionRepository.observeTransactions(
                        farmId = farmId,
                        filter = TransactionFilter(
                            fromEpochDay = range.fromEpochDay,
                            toEpochDay = range.toEpochDay,
                        ),
                    )
                        .map { transactions ->
                            ReportsUiState(
                                selectedPeriod = period,
                                report = FinancialReportCalculator.calculate(transactions, range),
                            )
                        }
                        .onStart {
                            emit(
                                ReportsUiState(
                                    selectedPeriod = period,
                                    isLoading = true,
                                )
                            )
                        }
                        .catch {
                            emit(
                                ReportsUiState(
                                    selectedPeriod = period,
                                    errorMessage = "No se pudo cargar el reporte",
                                )
                            )
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReportsUiState(isLoading = true),
            )

    fun onPeriodChanged(period: String) {
        if (period in listOf("Semana", "Mes", "Año")) {
            selectedPeriod.value = period
        }
    }
}