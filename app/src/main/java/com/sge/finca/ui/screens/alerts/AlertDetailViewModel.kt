package com.sge.finca.ui.screens.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.AlertRepository
import com.sge.finca.domain.repository.PaddockRepository
import com.sge.finca.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Detalle de la alerta de una vaca fuera de los potreros (#73), abierto desde la notificación. */
@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val alertRepository: AlertRepository,
    paddockRepository: PaddockRepository,
) : ViewModel() {

    private val alertId: String = checkNotNull(savedStateHandle["alertId"])
    private val message = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AlertDetailUiState> = sessionRepository.session
        .filter { !it.loading }
        .flatMapLatest { session ->
            val farmId = session.farmId
                ?: return@flatMapLatest flowOf(AlertDetailUiState(isLoading = false))
            combine(
                alertRepository.observeAlert(farmId, alertId),
                paddockRepository.observePaddocks(farmId),
                message,
            ) { alert, paddocks, pendingMessage ->
                AlertDetailUiState(
                    isLoading = false,
                    alert = alert,
                    paddocks = paddocks,
                    canForward = session.can(Permission.FORWARD_ALERTS),
                    message = pendingMessage,
                )
            }
        }
        .catch { emit(AlertDetailUiState(isLoading = false, message = "No se pudo cargar la alerta")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertDetailUiState())

    fun onForward() {
        val farmId = sessionRepository.session.value.farmId ?: return
        viewModelScope.launch {
            message.value = try {
                alertRepository.forward(farmId, alertId)
                "Se avisó a los trabajadores"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "No se pudo reenviar la alerta"
            }
        }
    }

    fun onMessageShown() {
        message.value = null
    }
}
