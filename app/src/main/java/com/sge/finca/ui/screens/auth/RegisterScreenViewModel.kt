package com.sge.finca.ui.screens.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.sge.finca.data.repository.AuthRepository
import com.sge.finca.domain.model.InviteCode
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.platform.biometric.BiometricSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricSetup: BiometricSetup,
    private val farmRepository: FarmRepository,
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
        register(farmName = _uiState.value.farmName.trim())
    }

    fun joinFarmButtonPress() {
        val code = _uiState.value.inviteCode
        when {
            code.isBlank() -> showError("El código de invitación es obligatorio")
            // El formato se revisa antes de crear la cuenta; si la finca existe se sabe después,
            // porque consultar el código requiere sesión.
            !InviteCode.isValid(code) -> showError("El código debe tener el formato XXXX-XXXX")
            else -> register(farmName = null, inviteCode = InviteCode.normalize(code))
        }
    }

    // ---------- Lógica interna ----------

    /**
     * [farmName] != null: el usuario crea una finca nueva y queda como propietario.
     * [inviteCode] != null: el usuario se une a una finca existente como trabajador (#29).
     */
    private fun register(farmName: String?, inviteCode: String? = null) {
        val s = _uiState.value
        if (s.isLoading) return

        _uiState.update { it.copy(isLoading = true, showErrorMessage = false) }
        viewModelScope.launch {
            try {
                authRepository.signUp(s.email.trim(), s.password)
                if (farmName != null) createFarm(farmName, s.name.trim())
                if (inviteCode != null) joinFarm(inviteCode, s.name.trim())
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

    /**
     * La cuenta ya existe en este punto, así que un fallo aquí no debe bloquear el registro:
     * se registra en el log y el usuario entra sin finca (las pantallas verán farmId = null).
     */
    private suspend fun createFarm(farmName: String, ownerName: String) {
        try {
            // withTimeoutOrNull: si la red se cae justo aquí, el commit de Firestore no termina
            // nunca y la pantalla quedaría cargando. Firestore igual envía el batch cuando vuelva.
            val farm = withTimeoutOrNull(FARM_CREATION_TIMEOUT_MS) {
                farmRepository.createFarm(farmName, ownerName)
            }
            if (farm == null) Log.w("SgeRegister", "La creación de la finca tardó demasiado")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SgeRegister", "No se pudo crear la finca", e)
        }
    }

    /**
     * Igual que [createFarm]: la cuenta ya existe, así que si el código no sirve el usuario entra
     * sin finca y desde Inicio puede volver a intentarlo con "Unirme con un código".
     */
    private suspend fun joinFarm(inviteCode: String, name: String) {
        try {
            withTimeoutOrNull(FARM_CREATION_TIMEOUT_MS) { farmRepository.joinFarm(inviteCode, name) }
                ?: Log.w("SgeRegister", "Unirse a la finca tardó demasiado")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("SgeRegister", "No se pudo unir a la finca con el código", e)
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
        const val FARM_CREATION_TIMEOUT_MS = 15_000L
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}