package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Detalle de una transacción. Observa Room, así que al volver de editarla se ve el cambio
 * sin recargar, y si se eliminó desde la edición la pantalla se cierra sola.
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    /** Si ya se mostró y luego desaparece, es porque se eliminó. */
    private var wasLoaded = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionDetailUiState> =
        sessionRepository.session
            .filter { !it.loading }
            .flatMapLatest { session ->
                val farmId = session.farmId
                if (farmId == null) {
                    flowOf(TransactionDetailUiState(isLoading = false, notFound = true))
                } else {
                    // Rol leído de la membresía de la finca (#28).
                    val canEdit = session.can(Permission.EDIT_TRANSACTION)
                    transactionRepository.observeTransactions(farmId)
                        .map { list ->
                            val transaction = list.firstOrNull { it.id == transactionId }
                            if (transaction != null) wasLoaded = true
                            TransactionDetailUiState(
                                isLoading = false,
                                transaction = transaction,
                                notFound = transaction == null,
                                canEdit = canEdit,
                                deleted = transaction == null && wasLoaded,
                            )
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState())
}
