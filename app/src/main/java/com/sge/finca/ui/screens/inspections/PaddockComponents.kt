package com.sge.finca.ui.screens.inspections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberMarkerState
import com.sge.finca.domain.model.Paddock
import com.sge.finca.ui.format.AreaText

private val PaddockFill = Color(0x554CAF50)
private val PaddockStroke = Color(0xFF2E7D32)

/** Potrero sobre el mapa (#71): relleno translúcido y etiqueta con nombre y área. */
@Composable
@GoogleMapComposable
fun PaddockOverlay(paddock: Paddock, onClick: () -> Unit) {
    val points = remember(paddock.points) { paddock.points.map { LatLng(it.latitude, it.longitude) } }
    val center = remember(points) {
        LatLng(points.map { it.latitude }.average(), points.map { it.longitude }.average())
    }
    Polygon(
        points = points,
        fillColor = PaddockFill,
        strokeColor = PaddockStroke,
        strokeWidth = 4f,
        clickable = true,
        onClick = { onClick() },
    )
    MarkerComposable(
        paddock.id, paddock.name, paddock.areaM2,
        state = rememberMarkerState(key = paddock.id, position = center),
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            onClick()
            true
        },
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.9f)) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(paddock.name, style = MaterialTheme.typography.labelMedium, color = PaddockStroke)
                Text(AreaText.format(paddock.areaM2), style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            }
        }
    }
}

/** Al finalizar una inspección que encierra un área (#71). */
@Composable
fun SavePaddockDialog(
    areaM2: Double,
    suggestedName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar como potrero") },
        text = {
            Column {
                Text("El recorrido encerró un área de ${AreaText.format(areaM2)}.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del potrero") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Descartar") } },
    )
}

/** Al tocar un potrero: el propietario lo renombra o lo elimina; los demás solo lo ven. */
@Composable
fun PaddockDetailDialog(
    paddock: Paddock,
    canManage: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(paddock.id) { mutableStateOf(paddock.name) }
    var confirmDelete by rememberSaveable(paddock.id) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar potrero") },
            text = { Text("Se eliminará \"${paddock.name}\" para todos los miembros de la finca.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canManage) "Potrero" else paddock.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canManage) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                    )
                }
                Text("Área estimada: ${AreaText.format(paddock.areaM2)}")
            }
        },
        confirmButton = {
            if (canManage) {
                TextButton(
                    onClick = { onRename(name) },
                    enabled = name.isNotBlank() && name.trim() != paddock.name,
                ) { Text("Guardar") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        },
        dismissButton = {
            if (canManage) {
                Row {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        },
    )
}
