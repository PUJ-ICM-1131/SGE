package com.sge.finca.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Maneja el permiso CAMERA en runtime (Accompanist):
 *  - concedido            -> ejecuta onGranted
 *  - denegado antes       -> explica por qué se necesita (rationale) y vuelve a pedir
 *  - denegado permanente  -> ofrece abrir Ajustes
 *
 * [content] recibe la función que hay que llamar cuando el usuario toca "Tomar foto".
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionGate(
    onGranted: () -> Unit,
    content: @Composable (requestCamera: () -> Unit) -> Unit,
) {
    val context = LocalContext.current

    // true cuando ya se lanzó al menos una vez la solicitud del sistema
    var alreadyRequested by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val permissionState = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) onGranted()
    }

    content {
        val status = permissionState.status
        when {
            status.isGranted -> onGranted()
            status.shouldShowRationale -> showRationale = true
            alreadyRequested -> showSettings = true // denegado permanentemente
            else -> {
                alreadyRequested = true
                permissionState.launchPermissionRequest()
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permiso de cámara") },
            text = { Text("Necesitamos la cámara para tomar la foto del animal. También puedes elegir una imagen de la galería.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    alreadyRequested = true
                    permissionState.launchPermissionRequest()
                }) { Text("Conceder permiso") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Ahora no") }
            },
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Cámara desactivada") },
            text = { Text("El permiso de cámara fue denegado. Puedes habilitarlo desde Ajustes, o elegir una imagen de la galería.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }) { Text("Abrir Ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("Cancelar") }
            },
        )
    }
}
