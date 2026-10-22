package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.sge.finca.ui.components.ShakeHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
    onNavigateToNew: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ShakeHandler(
        onStart = viewModel::startShakeDetection,
        onStop = viewModel::stopShakeDetection,
        onShake = onNavigateToNew,
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Historial de Transacciones") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNew) {
                Icon(Icons.Default.Add, contentDescription = "Nueva transacción")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No hay transacciones registradas",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
