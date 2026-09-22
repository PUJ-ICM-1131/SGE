package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar transacción?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Transacción") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
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
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoría") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { onNavigateBack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
