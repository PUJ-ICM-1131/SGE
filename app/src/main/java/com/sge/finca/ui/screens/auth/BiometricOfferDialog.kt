package com.sge.finca.ui.screens.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

/**
 * Diálogo que ofrece activar el "Inicio rápido con huella" después del primer login o registro.
 * Lo usan LoginScreen y RegisterScreen.
 */
@Composable
fun BiometricOfferDialog(
    onAccept: (FragmentActivity) -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Inicio rápido con huella") },
        text = {
            Text(
                "¿Quieres usar tu huella para entrar más rápido la próxima vez? " +
                    "Podrás cambiarlo cuando quieras en Configuración.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val activity = context.findFragmentActivity()
                    if (activity != null) onAccept(activity) else onDecline()
                },
            ) {
                Text("Activar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Ahora no")
            }
        },
    )
}
