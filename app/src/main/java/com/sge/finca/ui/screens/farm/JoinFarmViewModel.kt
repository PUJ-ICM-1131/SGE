package com.sge.finca.ui.screens.farm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.InviteCode
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class JoinFarmUiState(
    val code: String = "",
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
    /** Nombre de la finca a la que se unió; la pantalla vuelve al inicio. */
    val joinedFarmName: String? = null,
)

/** Unirse a una finca existente con el código que da el propietario (#29). */
@HiltViewModel
class JoinFarmViewModel @Inject constructor(
    private val farmRepository: FarmRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinFarmUiState())
    val uiState: StateFlow<JoinFarmUiState> = _uiState.asStateFlow()

    fun onCodeChanged(value: String) =
        _uiState.update { it.copy(code = value.uppercase().take(MAX_INPUT), errorMessage = null) }

    fun onJoin() {
        val state = _uiState.value
        if (state.isJoining || state.joinedFarmName != null) return // evita doble envío
        if (!InviteCode.isValid(state.code)) {
            _uiState.update { it.copy(errorMessage = "El código debe tener el formato XXXX-XXXX") }
            return
        }

        _uiState.update { it.copy(isJoining = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // El nombre del perfil, para que el propietario lo vea en Trabajadores.
                val name = withTimeoutOrNull(3_000) { userRepository.observeCurrentUser().first() }?.name.orEmpty()
                val joined = withTimeoutOrNull(JOIN_TIMEOUT_MS) { farmRepository.joinFarm(state.code, name) }
                    ?: throw IllegalStateException("Necesitas conexión a internet para unirte a una finca")
                _uiState.update { it.copy(isJoining = false, joinedFarmName = joined.farmName) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isJoining = false, errorMessage = e.message) }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(isJoining = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, errorMessage = "No se pudo unir a la finca. Inténtalo de nuevo") }
            }
        }
    }

    private companion object {
        const val MAX_INPUT = 12
        const val JOIN_TIMEOUT_MS = 15_000L
    }
}
