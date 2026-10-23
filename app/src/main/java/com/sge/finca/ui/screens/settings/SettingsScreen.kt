package com.sge.finca.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.platform.biometric.BiometricAvailability
import com.sge.finca.ui.screens.auth.findFragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra el mensaje una vez y lo limpia del estado.
    LaunchedEffect(state.message) {
        state.message?.let { text ->
            snackbarHostState.showSnackbar(text)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text("Perfil") },
                supportingContent = { Text("Nombre, correo electrónico") },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.clickable { /* TODO: edit profile */ },
            )

            ListItem(
                headlineContent = { Text("Inicio rápido con huella") },
                supportingContent = { Text(biometricDescription(state.biometricAvailability)) },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = state.biometricEnabled,
                        // Sin sensor no se puede activar; si ya estaba activa siempre se puede apagar.
                        enabled = state.biometricEnabled ||
                            state.biometricAvailability != BiometricAvailability.NO_HARDWARE,
                        onCheckedChange = { checked ->
                            if (checked) {
                                context.findFragmentActivity()
                                    ?.let { viewModel.onBiometricEnableRequested(it) }
                            } else {
                                viewModel.onBiometricDisabled()
                            }
                        },
                    )
                },
            )
        }
    }
}

private fun biometricDescription(availability: BiometricAvailability): String =
    when (availability) {
        BiometricAvailability.AVAILABLE -> "Entra con tu huella al abrir la app"
        BiometricAvailability.NO_HARDWARE -> "Este dispositivo no tiene sensor de huella"
        BiometricAvailability.NONE_ENROLLED -> "No hay huellas registradas en el dispositivo"
        BiometricAvailability.UNAVAILABLE -> "No disponible en este momento"
    }
