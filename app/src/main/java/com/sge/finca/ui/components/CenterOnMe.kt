package com.sge.finca.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Botón "centrar en mi ubicación" de los mapas (#59). Lo usan el mapa de ganado y la inspección
 * de campo; reemplaza al botón nativo de Google Maps, que no avisa si el GPS está apagado.
 */
@Composable
fun CenterOnMeButton(
    isLocating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(onClick = onClick, modifier = modifier) {
        if (isLocating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicación")
        }
    }
}

/** Avisos del botón "centrar en mi ubicación" (#59). */
enum class GpsMessage {
    /** La ubicación del celular está apagada. */
    LOCATION_OFF,

    /** La ubicación está encendida, pero no llegó una posición a tiempo (p. ej. bajo techo). */
    NO_SIGNAL,
}

/**
 * Muestra [message] en un snackbar (nunca un diálogo) y avisa con [onShown] al terminar.
 * Con la ubicación apagada ofrece "Activar", que abre los ajustes de ubicación.
 */
@Composable
fun GpsMessageEffect(
    message: GpsMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnShown = rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        try {
            when (message) {
                GpsMessage.LOCATION_OFF -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "No hay señal GPS: activa la ubicación del celular para centrar el mapa.",
                        actionLabel = "Activar",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
                GpsMessage.NO_SIGNAL -> snackbarHostState.showSnackbar(
                    "Aún no hay señal GPS. Sal a cielo abierto e intenta de nuevo.",
                )
            }
        } finally {
            // Después de mostrarlo: limpiarlo antes reiniciaría este efecto y cortaría el aviso.
            currentOnShown.value()
        }
    }
}
