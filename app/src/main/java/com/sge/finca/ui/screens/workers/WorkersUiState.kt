package com.sge.finca.ui.screens.workers

data class WorkersUiState(
    val inviteCode: String = "XXXX-XXXX",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
