package com.sge.finca.ui.screens.alerts

import com.sge.finca.domain.model.AlertStatus
import com.sge.finca.domain.model.AnimalAlert
import com.sge.finca.domain.model.Paddock

data class AlertDetailUiState(
    val isLoading: Boolean = true,
    /** null cuando ya cargó: la alerta no existe en la finca activa. */
    val alert: AnimalAlert? = null,
    val paddocks: List<Paddock> = emptyList(),
    /** Solo el propietario reenvía (FORWARD_ALERTS). */
    val canForward: Boolean = false,
    /** Aviso pendiente del snackbar. */
    val message: String? = null,
) {
    /** Se reenvía una sola vez y solo mientras la vaca sigue fuera. */
    val showForward: Boolean
        get() = canForward && alert != null && alert.status == AlertStatus.OPEN && alert.forwardedAt == null
}
