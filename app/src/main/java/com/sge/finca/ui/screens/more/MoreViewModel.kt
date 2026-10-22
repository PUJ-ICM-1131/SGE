package com.sge.finca.ui.screens.more

import androidx.lifecycle.ViewModel
import com.sge.finca.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreState())
    val uiState: StateFlow<MoreState> = _uiState

    fun logoutButtonPress() {
        authRepository.signOut()
        _uiState.update { it.copy(navigateLogin = true) }
    }
}
