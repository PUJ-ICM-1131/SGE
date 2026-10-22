package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.ViewModel
import com.sge.finca.platform.sensors.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val shakeDetector: ShakeDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    fun startShakeDetection(onShake: () -> Unit): Boolean {
        return shakeDetector.start(onShake)
    }

    fun stopShakeDetection() {
        shakeDetector.stop()
    }

    // TODO: observe transactions from repository
}