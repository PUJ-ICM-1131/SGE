package com.sge.finca.ui.screens.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricSetup: BiometricSetup,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterState())
    val uiState: StateFlow<RegisterState> = _uiState

    // ---------- Actualización de campos ----------

    fun updateName(input: String) = _uiState.update { it.copy(name = input) }

    fun updateEmail(input: String) = _uiState.update { it.copy(email = input) }

    fun updatePassword(input: String) = _uiState.update { it.copy(password = input) }

    fun updateConfirmPassword(input: String) = _uiState.update { it.copy(confirmPassword = input) }

    fun updateFarmName(input: String) = _uiState.update { it.copy(farmName = input) }

    fun updateInviteCode(input: String) = _uiState.update { it.copy(inviteCode = input) }

    // ---------- Eventos de la pantalla ----------

    /** Paso 1 -> Paso 2: valida los datos de la cuenta (sin llamar a Firebase todavía). */
    fun continueButtonPress() {
        val s = _uiState.value
        val error = when {
            s.name.isBlank() || s.email.isBlank() ||
                    s.password.isBlank() || s.confirmPassword.isBlank() ->
                "Todos los campos son obligatorios"
            !EMAIL_REGEX.matches(s.email.trim()) ->
                "El correo electrónico no es válido"
            s.password.length < MIN_PASSWORD_LENGTH ->
                "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
            s.password != s.confirmPassword ->
                "Las contraseñas no coinciden"
            else -> null
        }

        if (error != null) {
            showError(error)
        } else {
            _uiState.update { it.copy(step = 1, showErrorMessage = false) }
        }
    }

    /** Volver al paso 1 (por ejemplo, para corregir un correo ya registrado). */
    fun backToAccountStep() {
        _uiState.update { it.copy(step = 0, showErrorMessage = false) }
    }

    fun createFarmButtonPress() {
        if (_uiState.value.farmName.isBlank()) {
            showError("El nombre de la finca es obligatorio")
            return
        }
        // TODO: guardar la finca (nombre) cuando exista la base de datos (Firestore)
        register()
    }

    fun joinFarmButtonPress() {
        if (_uiState.value.inviteCode.isBlank()) {
            showError("El código de invitación es obligatorio")
            return
        }
        // TODO: validar el código de invitación cuando exista la base de datos (Firestore)
        register()
    }

    // ---------- Lógica interna ----------

    private fun register() {
        val s = _uiState.value
        if (s.isLoading) return

        _uiState.update { it.copy(isLoading = true, showErrorMessage = false) }
        viewModelScope.launch {
            try {
                authRepository.signUp(s.email.trim(), s.password)
                // Cuenta creada: se ofrece activar la huella (si aplica) antes de ir al Home.
                val uid = authRepository.currentUser?.uid
                if (uid != null && biometricSetup.shouldOfferActivation(uid)) {
                    _uiState.update { it.copy(isLoading = false, showBiometricOffer = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, navigate = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showErrorMessage = true,
                        errorMessage = e.toFriendlyMessage(),
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

    private fun showError(message: String) {
        _uiState.update { it.copy(showErrorMessage = true, errorMessage = message) }
    }

    // El orden importa: WeakPassword hereda de InvalidCredentials.
    private fun Exception.toFriendlyMessage(): String = when (this) {
        is FirebaseAuthUserCollisionException -> "Este correo ya está registrado"
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
        is FirebaseAuthInvalidCredentialsException -> "El correo electrónico no es válido"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> message ?: "Error al crear la cuenta"
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}