package com.sge.finca.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * Mide la altura de algo que flota sobre un mapa (botones, tarjetas, avisos) para pasársela al
 * mapa como contentPadding (#60). Así el mapa corre sus controles de zoom, el botón de mi
 * ubicación y el logo (que por términos de uso debe verse) fuera de lo que los tapa.
 *
 * Ponerlo antes del padding exterior del elemento para que ese margen también cuente.
 */
fun Modifier.onHeightChanged(density: Density, onHeight: (Dp) -> Unit): Modifier =
    onSizeChanged { size -> onHeight(with(density) { size.height.toDp() }) }
