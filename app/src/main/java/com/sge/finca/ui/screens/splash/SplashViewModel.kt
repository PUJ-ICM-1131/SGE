package com.sge.finca.ui.screens.splash

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.datasource.SessionStatus
import com.sge.finca.data.repository.AuthRepository
import com.sge.finca.platform.biometric.BiometricHelper
import com.sge.finca.platform.biometric.BiometricPreferences
import com.sge.finca.platform.biometric.BiometricResult
import com.sge.finca.ui.screens.auth.AuthNotice
import com.sge.finca.ui.screens.auth.AuthNoticeHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val biometricPreferences: BiometricPreferences,
    private val noticeHolder: AuthNoticeHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState: StateFlow<SplashState> = _uiState

    init {
        // Firebase guarda la sesión en el dispositivo: si hay usuario, ya inició sesión antes.
        val user = authRepository.currentUser
        val plan = StartupDecision.decide(
            hasSession = user != null,
            biometricEnabled = user != null && biometricPreferences.isEnabledFor(user.uid),
            availability = biometricHelper.availability(),
        )
        plan.notice?.let { noticeHolder.post(it) }
        _uiState.update {
            when (plan.destination) {
                StartupDestination.HOME -> it.copy(navigateHome = true)
                StartupDestination.LOGIN -> it.copy(navigateLogin = true)
                StartupDestination.BIOMETRIC_PROMPT -> it.copy(showBiometricPrompt = true)
            }
        }
    }

    /** La pantalla llama a esto cuando ya tiene la actividad para mostrar el prompt. */
    fun startBiometricPrompt(activity: FragmentActivity) {
        biometricHelper.authenticate(activity) { result -> onBiometricResult(result) }
    }

    /** No se pudo mostrar el prompt (sin actividad válida): se cae al login normal. */
    fun onPromptUnavailable() {
        goLogin(AuthNotice.BIOMETRIC_UNAVAILABLE)
    }

    private fun onBiometricResult(result: BiometricResult) {
        when (result) {
            // Éxito: la huella sólo confirma al dueño del dispositivo; antes de entrar se
            // comprueba con Firebase que la sesión siga válida.
            BiometricResult.Success -> viewModelScope.launch {
                when (authRepository.validateSession()) {
                    SessionStatus.VALID, SessionStatus.UNVERIFIED -> goHome()
                    SessionStatus.EXPIRED -> {
                        authRepository.signOut()
                        goLogin(AuthNotice.SESSION_EXPIRED)
                    }
                }
            }

            // Cancelación o botón "Usar contraseña": formulario de email/contraseña.
            BiometricResult.Cancelled -> goLogin()

            is BiometricResult.Failed -> goLogin(AuthNotice.BIOMETRIC_FAILED)
        }
    }

    private fun goHome() {
        _uiState.update { it.copy(showBiometricPrompt = false, navigateHome = true) }
    }

    private fun goLogin(notice: AuthNotice? = null) {
        notice?.let { noticeHolder.post(it) }
        _uiState.update { it.copy(showBiometricPrompt = false, navigateLogin = true) }
    }
}
