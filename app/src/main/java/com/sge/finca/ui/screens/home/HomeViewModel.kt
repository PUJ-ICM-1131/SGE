package com.sge.finca.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.repository.UserRepository
import com.sge.finca.platform.sensors.ShakeDetector
import com.sge.finca.ui.screens.transactions.HistoryFilter
import com.sge.finca.ui.screens.reminders.attentionCount
import com.sge.finca.ui.screens.transactions.HistorySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    userRepository: UserRepository,
    farmRepository: FarmRepository,
    transactionRepository: TransactionRepository,
    reminderRepository: ReminderRepository,
    private val shakeDetector: ShakeDetector,
) : ViewModel() {

    /** Cada minuto, para que un recordatorio que vence mientras se mira el inicio sume en la campana. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> =
        userRepository.observeCurrentFarmId()
            .flatMapLatest { farmId ->
                if (farmId == null) {
                    flowOf(HomeUiState(isLoading = false, noFarm = true))
                } else {
                    // Una sola consulta (más reciente primero): de ahí salen el resumen del mes y las últimas.
                    combine(
                        farmRepository.observeFarm(farmId),
                        transactionRepository.observeTransactions(farmId),
                        reminderRepository.observePending(farmId),
                        clock,
                    ) { farm, transactions, pendingReminders, now ->
                        val monthStart = HistoryFilter.startOfMonth(BusinessDate.today())
                        val month = HistorySummary.of(transactions.filter { it.dateEpochDay >= monthStart })
                        HomeUiState(
                            farmName = farm?.name?.takeIf { it.isNotBlank() } ?: "SGE Finca",
                            balanceCents = month.balanceCents,
                            incomeCents = month.incomeCents,
                            expenseCents = month.expenseCents,
                            recentTransactions = transactions.take(RECENT_COUNT),
                            notificationCount = pendingReminders.attentionCount(endOfDay(now)),
                            isLoading = false,
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun endOfDay(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    fun startShakeDetection(onShake: () -> Unit): Boolean = shakeDetector.start(onShake)

    fun stopShakeDetection() = shakeDetector.stop()

    companion object {
        const val RECENT_COUNT = 5
    }
}
