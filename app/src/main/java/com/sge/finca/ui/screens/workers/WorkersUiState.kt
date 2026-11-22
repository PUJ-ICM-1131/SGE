package com.sge.finca.ui.screens.workers

import com.sge.finca.data.repository.WorkerSummary

data class WorkersUiState(
    val inviteCode: String = "",
    val hasFarm: Boolean = false,
    val isOwner: Boolean = false,
    val isLoading: Boolean = true,
    val isRegenerating: Boolean = false,
    val workers: List<WorkerSummary> = emptyList(),
    val isLoadingWorkers: Boolean = false,
    val errorMessage: String? = null,
)