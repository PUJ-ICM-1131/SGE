package com.sge.finca.ui.screens.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.sge.finca.data.repository.AuthRepository
import com.sge.finca.platform.biometric.BiometricAvailability
import com.sge.finca.platform.biometric.BiometricSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricSetup: BiometricSetup,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshBiometricState()
    }

    fun onSignOut() {
        // TODO: sign out via Firebase Auth
    }

    /** El usuario encendió el switch: se valida el sensor y se confirma con una huella. */
    fun onBiometricEnableRequested(activity: FragmentActivity) {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            showMessage("No hay una sesión activa.")
            return
        }
        when (biometricSetup.availability()) {
            BiometricAvailability.AVAILABLE ->
                biometricSetup.enableWithConfirmation(activity, uid) { enabled ->
                    refreshBiometricState()
                    if (!enabled) showMessage("No se activó el inicio rápido con huella.")
                }

            BiometricAvailability.NONE_ENROLLED ->
                showMessage(
                    "No hay huellas registradas en este dispositivo. " +
                        "Regístrala en los ajustes del teléfono y vuelve a intentarlo.",
                )

            BiometricAvailability.NO_HARDWARE ->
                showMessage("Este dispositivo no tiene sensor de huella.")

            BiometricAvailability.UNAVAILABLE ->
                showMessage("El sensor de huella no está disponible en este momento.")
        }
        refreshBiometricState()
    }

    /** El usuario apagó el switch. */
    fun onBiometricDisabled() {
        biometricSetup.disable()
        refreshBiometricState()
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshBiometricState() {
        val uid = authRepository.currentUser?.uid
        _uiState.update {
            it.copy(
                biometricAvailability = biometricSetup.availability(),
                biometricEnabled = uid != null && biometricSetup.isEnabledFor(uid),
            )
        }
    }

    private fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }
}
