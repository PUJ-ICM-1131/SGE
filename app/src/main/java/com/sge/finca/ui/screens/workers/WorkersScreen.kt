package com.sge.finca.ui.screens.workers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.data.repository.WorkerSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(
    viewModel: WorkersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(state.inviteCode) { mutableStateOf(false) }
    var confirmRegeneration by remember { mutableStateOf(false) }

    if (confirmRegeneration) {
        AlertDialog(
            onDismissRequest = { confirmRegeneration = false },
            title = { Text("Regenerar código") },
            text = {
                Text(
                    "El código actual dejará de funcionar. " +
                            "Tendrás que compartir el nuevo código con los trabajadores que quieras invitar.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRegeneration = false
                        viewModel.regenerateInviteCode()
                    },
                ) {
                    Text("Regenerar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRegeneration = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de trabajadores") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }

                !state.hasFarm -> {
                    Text("Esta cuenta todavía no tiene una finca asociada")
                }

                !state.isOwner -> {
                    Text("Solo el propietario puede gestionar los trabajadores")
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Código de invitación",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.inviteCode.ifBlank { "Sin código" },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Comparte este código para invitar trabajadores a la finca",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (state.inviteCode.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(
                                            AnnotatedString(state.inviteCode),
                                        )
                                        copied = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color.Black,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = if (copied) "Copiado" else "Copiar código",
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }

                            TextButton(
                                onClick = { confirmRegeneration = true },
                                enabled = !state.isRegenerating,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Black,
                                ),
                            ) {
                                Text(
                                    if (state.isRegenerating) {
                                        "Regenerando..."
                                    } else {
                                        "Regenerar código"
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Trabajadores",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        state.isLoadingWorkers -> {
                            CircularProgressIndicator()
                        }

                        state.workers.isEmpty() -> {
                            Text(
                                text = "No hay trabajadores registrados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            LazyColumn {
                                items(state.workers, key = { it.uid }) { worker ->
                                    WorkerCard(worker)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WorkerCard(worker: WorkerSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = worker.name.ifBlank { worker.email.ifBlank { "Sin nombre" } },
                style = MaterialTheme.typography.titleMedium,
            )
            if (worker.email.isNotBlank()) {
                Text(worker.email, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (worker.isActive) "Activo" else "Inactivo",
                color = if (worker.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                text = "Se unió: ${formatJoinedAt(worker.joinedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatJoinedAt(timestamp: Long): String {
    if (timestamp <= 0L) return "Fecha no disponible"
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CO"))
    return formatter.format(Date(timestamp))
}