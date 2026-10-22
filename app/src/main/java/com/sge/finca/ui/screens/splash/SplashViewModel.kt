package com.sge.finca.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.sge.finca.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState: StateFlow<SplashState> = _uiState

    init {
        // Firebase guarda la sesión en el dispositivo: si hay usuario, ya inició sesión antes.
        if (authRepository.currentUser != null) {
            _uiState.update { it.copy(navigateHome = true) }
        } else {
            _uiState.update { it.copy(navigateLogin = true) }
        }
    }
}
