package com.sge.finca.ui.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.WorkersRepository
import com.sge.finca.domain.model.UserRole
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WorkersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val farmRepository: FarmRepository,
    private val workersRepository: WorkersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkersUiState())
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    private var currentFarmId: String? = null

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collectLatest { user ->
                val farmId = user?.farmId?.takeIf { it.isNotBlank() }
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

                farmRepository.observeFarm(farmId).collectLatest { farm ->
                    val isOwner =
                        user.role == UserRole.OWNER && farm?.ownerUid == user.uid

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
}