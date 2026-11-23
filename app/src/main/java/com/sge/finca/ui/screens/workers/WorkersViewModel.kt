package com.sge.finca.ui.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.WorkersRepository
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.SessionRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WorkersViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val farmRepository: FarmRepository,
    private val workersRepository: WorkersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkersUiState())
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    private var currentFarmId: String? = null

    init {
        viewModelScope.launch {
            // El rol sale de la membresía de la finca (#28), no del perfil.
            sessionRepository.session
                .filter { !it.loading }
                .map { it.farmId?.takeIf(String::isNotBlank) to it.isOwner }
                .distinctUntilChanged()
                .collectLatest { (farmId, isOwnerByRole) ->
                currentFarmId = farmId

                if (farmId == null) {
                    _uiState.update {
                        it.copy(
                            inviteCode = "",
                            hasFarm = false,
                            isOwner = false,
                            isLoading = false,
                            isRegenerating = false,
                            workers = emptyList(),
                            isLoadingWorkers = false,
                            errorMessage = null,
                        )
                    }
                    return@collectLatest
                }

                _uiState.update {
                    it.copy(
                        inviteCode = "",
                        hasFarm = true,
                        isOwner = false,
                        isLoading = true,
                        workers = emptyList(),
                        isLoadingWorkers = false,
                        errorMessage = null,
                    )
                }

                // Fincas anteriores al #29: publica el código vigente en el índice para que
                // funcione al unirse. Si falla (sin red), se reintenta la próxima vez.
                if (isOwnerByRole) {
                    try {
                        farmRepository.ensureInviteCodeIndex(farmId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("SgeWorkers", "No se pudo publicar el código en el índice", e)
                    }
                }

                farmRepository.observeFarm(farmId).collectLatest { farm ->
                    val isOwner = isOwnerByRole && farm != null

                    _uiState.update {
                        it.copy(
                            inviteCode = if (isOwner) farm?.inviteCode.orEmpty() else "",
                            hasFarm = true,
                            isOwner = isOwner,
                            isLoading = false,
                            workers = emptyList(),
                            isLoadingWorkers = isOwner,
                            errorMessage = if (farm == null) {
                                "No se pudo cargar la finca"
                            } else {
                                null
                            },
                        )
                    }

                    if (!isOwner) return@collectLatest

                    try {
                        workersRepository.observeWorkers(farmId).collectLatest { workers ->
                            _uiState.update {
                                it.copy(
                                    workers = workers,
                                    isLoadingWorkers = false,
                                )
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                workers = emptyList(),
                                isLoadingWorkers = false,
                                errorMessage = e.message
                                    ?: "No se pudieron cargar los trabajadores",
                            )
                        }
                    }
                }
            }
        }
    }

    fun regenerateInviteCode() {
        val farmId = currentFarmId ?: return
        if (!_uiState.value.isOwner || _uiState.value.isRegenerating) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isRegenerating = true, errorMessage = null)
            }

            try {
                val newCode = farmRepository.regenerateInviteCode(farmId)
                if (currentFarmId == farmId) {
                    _uiState.update { it.copy(inviteCode = newCode) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                            ?: "No se pudo regenerar el código de invitación",
                    )
                }
            } finally {
                _uiState.update { it.copy(isRegenerating = false) }
            }
        }
    }

    /** Otorga o quita a un trabajador un permiso de vista, por ejemplo ver el balance (#67). */
    fun setGrant(workerUid: String, permission: Permission, granted: Boolean) {
        val farmId = currentFarmId ?: return
        if (!_uiState.value.isOwner) return

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            try {
                workersRepository.setGrant(farmId, workerUid, permission, granted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "No se pudo actualizar el permiso")
                }
            }
        }
    }
}