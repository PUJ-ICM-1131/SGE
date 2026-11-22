package com.sge.finca.ui.screens.transactions

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Detalle de una transacción. Observa Room, así que al volver de editarla se ve el cambio
 * sin recargar, y si se eliminó desde la edición la pantalla se cierra sola.
 *
 * Desde el aviso push puede abrirse una transacción de otro miembro que aún no está en este
 * celular (el listener de #32 todavía no la bajó, o la app venía cerrada). Por eso, si no está
 * en Room, se busca una vez en Firestore y se muestra "cargando" hasta tener respuesta; solo
 * entonces se dice que no existe. Si llega después por el listener, aparece sola.
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    /** Si ya se mostró y luego desaparece, es porque se eliminó. */
    private var wasLoaded = false

    private val remoteLookup = MutableStateFlow(RemoteLookup.RUNNING)

    init {
        lookUpIfMissing()
    }

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
                    combine(
                        transactionRepository.observeTransaction(farmId, transactionId),
                        remoteLookup,
                    ) { transaction, lookup ->
                        if (transaction != null) wasLoaded = true
                        val missing = transaction == null && !wasLoaded
                        TransactionDetailUiState(
                            // FOUND sin fila todavía: Room está por emitir la recién guardada.
                            isLoading = missing &&
                                (lookup == RemoteLookup.RUNNING || lookup == RemoteLookup.FOUND),
                            transaction = transaction,
                            notFound = missing && lookup == RemoteLookup.NOT_FOUND,
                            unreachable = missing && lookup == RemoteLookup.FAILED,
                            canEdit = canEdit,
                            deleted = transaction == null && wasLoaded,
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState())

    private fun lookUpIfMissing() {
        viewModelScope.launch {
            val farmId = sessionRepository.session.first { !it.loading }.farmId
            remoteLookup.value = when {
                farmId == null -> RemoteLookup.NOT_FOUND
                transactionRepository.getTransaction(farmId, transactionId) != null -> RemoteLookup.FOUND
                else -> try {
                    if (transactionRepository.pullFromRemote(farmId, transactionId)) {
                        RemoteLookup.FOUND
                    } else {
                        RemoteLookup.NOT_FOUND
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Sin red, sin permiso o sin respuesta: no se puede afirmar que no exista.
                    Log.w(TAG, "No se pudo traer la transacción $transactionId", e)
                    RemoteLookup.FAILED
                }
            }
        }
    }

    private enum class RemoteLookup { RUNNING, FOUND, NOT_FOUND, FAILED }

    private companion object {
        const val TAG = "SgeTransactions"
    }
}
