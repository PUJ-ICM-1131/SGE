package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.format.BusinessDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Se eliminó desde la edición: no tiene sentido quedarse en un detalle vacío.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val transaction = state.transaction
                    if (state.canEdit && transaction != null) {
                        IconButton(onClick = { onEdit(transaction.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val transaction = state.transaction
        when {
            state.isLoading -> CenteredBox(Modifier.padding(innerPadding)) { CircularProgressIndicator() }
            transaction == null -> CenteredBox(Modifier.padding(innerPadding)) {
                Text("Este movimiento ya no existe.")
            }
            else -> DetailContent(transaction, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun DetailContent(transaction: Transaction, modifier: Modifier) {
    val isIncome = transaction.type == TransactionType.INCOME
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Encabezado: tipo y monto grande.
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TransactionTypeIcon(transaction.type, size = 56)
            Spacer(Modifier.height(12.dp))
            Text(
                if (isIncome) "Ingreso" else "Egreso",
                style = MaterialTheme.typography.labelLarge,
                color = transaction.type.color(),
            )
            Text(
                transaction.signedAmount(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = transaction.type.color(),
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                DetailRow(Icons.Default.Category, "Categoría", transaction.categoryName)
                RowDivider()
                DetailRow(Icons.Default.CalendarMonth, "Fecha", BusinessDateFormat.format(transaction.dateEpochDay))
                transaction.paymentMethod?.let {
                    RowDivider()
                    DetailRow(Icons.Default.Payments, "Método de pago", it)
                }
                transaction.createdByName?.let {
                    RowDivider()
                    DetailRow(Icons.Default.Person, "Registrado por", it)
                }
                RowDivider()
                DetailRow(Icons.AutoMirrored.Filled.Notes, "Nota", transaction.note ?: "Sin nota")
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
