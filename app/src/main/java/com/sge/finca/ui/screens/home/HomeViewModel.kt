package com.sge.finca.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.SyncRepository
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.platform.sensors.ShakeDetector
import com.sge.finca.ui.screens.reminders.attentionCount
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    farmRepository: FarmRepository,
    transactionRepository: TransactionRepository,
    reminderRepository: ReminderRepository,
    private val syncRepository: SyncRepository,
    private val shakeDetector: ShakeDetector,
) : ViewModel() {

    /** Cada minuto, para que un recordatorio que vence mientras se mira el inicio sume en la campana. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    /** Finca mostrada; la usa "Reintentar" del indicador de sincronización. */
    private var currentFarmId: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> =
        sessionRepository.session
            .filter { !it.loading }
            .onEach { currentFarmId = it.farmId }
            .flatMapLatest { session ->
                val farmId = session.farmId
                // Rol de la membresía (#28); mientras no se conozca, el más restringido.
                val role = session.role ?: FarmRole.WORKER
                if (farmId == null) {
                    flowOf(HomeUiState(isLoading = false, noFarm = true))
                } else {
                    // Una sola consulta (más reciente primero): de ahí salen el resumen del mes y las últimas.
                    combine(
                        farmRepository.observeFarm(farmId),
                        transactionRepository.observeTransactions(farmId),
                        reminderRepository.observePending(farmId),
                        clock,
                        syncRepository.observeSummary(farmId),
                    ) { farm, transactions, pendingReminders, now, sync ->
                        val summary = HomeSummary.of(transactions, today = BusinessDate.fromMillis(now))
                        HomeUiState(
                            farmName = farm?.name.orEmpty(),
                            userRole = role,
                            monthlyBalance = summary.monthlyBalance,
                            monthlyIncome = summary.monthlyIncome,
                            monthlyExpense = summary.monthlyExpense,
                            incomeCount = summary.incomeCount,
                            expenseCount = summary.expenseCount,
                            recentTransactions = summary.recentTransactions,
                            pendingSyncCount = sync.pending,
                            failedSyncCount = sync.failed,
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

    /** "Reintentar": vuelve a poner en cola lo que Firestore rechazó (issue #32). */
    fun retryFailedSync() {
        val farmId = currentFarmId ?: return
        viewModelScope.launch { syncRepository.retryFailed(farmId) }
    }

    fun startShakeDetection(onShake: () -> Unit): Boolean = shakeDetector.start(onShake)

    fun stopShakeDetection() = shakeDetector.stop()
}
