package com.sge.finca.ui.screens.inspections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Estadísticas y controles de la inspección en curso, dentro del mapa de la finca (#72).

@Composable
internal fun TrackingStatsOverlay(
    elapsedSeconds: Long,
    distanceMeters: Double,
    hasGpsSignal: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isPaused) {
            Text(
                text = "PAUSADO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatItem("Tiempo", formatDuration(elapsedSeconds))
            StatItem("Distancia", formatDistance(distanceMeters))
            GpsIndicator(hasSignal = hasGpsSignal)
        }
    }
}

@Composable
private fun GpsIndicator(hasSignal: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (hasSignal) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = CircleShape,
                ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "GPS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Sin inspección en curso: iniciar o registrar un animal. Durante la inspección: pausar o finalizar. */
@Composable
internal fun TrackingControls(
    isTracking: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRegisterAnimal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!isTracking) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Text("Iniciar Inspección")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onRegisterAnimal) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Animal")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (isPaused) {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                        Text("Reanudar")
                    }
                } else {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                        Text("Pausar")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Finalizar")
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000)
    else "%.0f m".format(meters)
