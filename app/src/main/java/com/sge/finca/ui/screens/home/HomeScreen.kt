package com.sge.finca.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sge.finca.ui.components.ShakeHandler
import com.sge.finca.ui.components.SyncStatusBanner
import com.sge.finca.ui.format.Cop
import com.sge.finca.ui.screens.transactions.TransactionListItem
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTransaction: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Android 13+: se pide una vez el permiso para avisos de movimientos y recordatorios.
    // Si lo niega, la app funciona igual; la campana sigue mostrando los recordatorios.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        var asked by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!permission.status.isGranted && !asked) {
                asked = true
                permission.launchPermissionRequest()
            }
        }
    }

    // Agitar el celular abre "Nueva transacción" (#13).
    ShakeHandler(
        onStart = viewModel::startShakeDetection,
        onStop = viewModel::stopShakeDetection,
        onShake = onNavigateToNewTransaction,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.farmName) },
                actions = {
                    // Panel de notificaciones: recordatorios vencidos o de hoy.
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (state.notificationCount > 0) {
                                    Badge { Text(if (state.notificationCount > 9) "9+" else "${state.notificationCount}") }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones: ${state.notificationCount} pendientes",
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.noFarm) {
                FloatingActionButton(onClick = onNavigateToNewTransaction) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva transacción")
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.noFarm -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Tu cuenta aún no pertenece a una finca.", textAlign = TextAlign.Center)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp, // espacio para el FAB
                ),
            ) {
                // Indicador de sincronización (#32): no se muestra si todo está sincronizado.
                if (state.syncPending > 0 || state.syncFailed > 0) {
                    item(key = "sync") {
                        SyncStatusBanner(
                            pending = state.syncPending,
                            failed = state.syncFailed,
                            onRetry = viewModel::retryFailedSync,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        )
                    }
                }

                item(key = "balance") { MonthSummary(state) }

                item(key = "recent-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Transacciones recientes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.recentTransactions.isNotEmpty()) {
                            TextButton(onClick = onNavigateToHistory) { Text("Ver todo") }
                        }
                    }
                }

                if (state.recentTransactions.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "No hay transacciones aún",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Toca + para registrar la primera",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(state.recentTransactions, key = { it.id }) { transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            onClick = { onNavigateToTransaction(transaction.id) },
                            showDate = true,
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummary(state: HomeUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Balance del mes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    Cop.format(state.balanceCents),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (state.balanceCents < 0) ExpenseRed else MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountCard("Ingresos", "+" + Cop.format(state.incomeCents), IncomeGreen, Modifier.weight(1f))
            AmountCard("Egresos", "-" + Cop.format(state.expenseCents), ExpenseRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AmountCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}
