package com.sge.finca.ui.format

import com.sge.finca.domain.model.FarmRole

/** Nombre del rol para mostrar en la UI. */
fun FarmRole.label(): String = when (this) {
    FarmRole.OWNER -> "Propietario"
    FarmRole.WORKER -> "Trabajador"
}

/** "1 movimiento" / "3 movimientos". */
fun movementsLabel(count: Int): String = if (count == 1) "1 movimiento" else "$count movimientos"
