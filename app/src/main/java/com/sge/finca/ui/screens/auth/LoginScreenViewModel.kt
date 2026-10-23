package com.sge.finca.ui.screens.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.AuthRepository
import com.sge.finca.platform.biometric.BiometricSetup
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
    private val biometricSetup: BiometricSetup,
    noticeHolder: AuthNoticeHolder,
) : ViewModel() {

    // Si el Splash mandó al usuario aquí con un aviso (sesión expirada, sin huellas...), se muestra.
    private val _uiState = MutableStateFlow(
        LoginState(notice = noticeHolder.consume()?.message.orEmpty()),
    )
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
     * Tras un login correcto se ofrece activar la huella (si aplica) antes de ir al Home.
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
                val uid = authRepository.currentUser?.uid
                if (uid != null && biometricSetup.shouldOfferActivation(uid)) {
                    _uiState.update {
                        it.copy(showErrorMessage = false, notice = "", showBiometricOffer = true)
                    }
                } else {
                    _uiState.update { it.copy(showErrorMessage = false, navigate = true) }
                }
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

    /** El usuario aceptó el diálogo: se confirma con una huella y se activa. */
    fun onBiometricOfferAccepted(activity: FragmentActivity) {
        val uid = authRepository.currentUser?.uid
        _uiState.update { it.copy(showBiometricOffer = false) }
        if (uid == null) {
            _uiState.update { it.copy(navigate = true) }
            return
        }
        biometricSetup.enableWithConfirmation(activity, uid) { _ ->
            _uiState.update { it.copy(navigate = true) }
        }
    }

    fun onBiometricOfferDeclined() {
        authRepository.currentUser?.uid?.let { biometricSetup.declineOffer(it) }
        _uiState.update { it.copy(showBiometricOffer = false, navigate = true) }
    }
}
