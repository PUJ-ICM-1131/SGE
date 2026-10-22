package com.sge.finca.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState

    fun updateEmail(input: String) {
        _uiState.update { it.copy(email = input) }
    }

    fun updatePassword(input: String) {
        _uiState.update { it.copy(password = input) }
    }

    /**
     * Regla de negocio: campos obligatorios. Luego se autentica con Firebase
     * (signIn es suspend, por eso se lanza dentro de viewModelScope).
     */
    fun loginButtonPress() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.update {
                it.copy(
                    showErrorMessage = true,
                    errorMessage = "Todos los campos son obligatorios",
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                authRepository.signIn(current.email.trim(), current.password)
                _uiState.update { it.copy(showErrorMessage = false, navigate = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showErrorMessage = true,
                        errorMessage = e.message ?: "Error al iniciar sesión",
                    )
                }
            }
        }
    }
}