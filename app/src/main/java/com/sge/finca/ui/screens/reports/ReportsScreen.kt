package com.sge.finca.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.domain.model.ReportCategoryTotal
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.format.BusinessDateFormat
import com.sge.finca.ui.format.Cop
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Balance") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semana", "Mes", "Año").forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { viewModel.onPeriodChanged(period) },
                        label = { Text(period) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }

                state.noFarm -> {
                    Text("Esta cuenta todavía no tiene una finca asociada")
                }

                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.report != null -> {
                    val report = state.report!!

                    Text(
                        text = "Del ${BusinessDateFormat.format(report.range.fromEpochDay)} " +
                                "al ${BusinessDateFormat.format(report.range.toEpochDay)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (report.hasUnsyncedTransactions) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Algunos datos no están sincronizados " +
                                        "(${transactionCountText(report.unsyncedCount)}).",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (report.isEmpty) {
                        Text("No hay transacciones en este período")
                    } else {
                        SummaryCard(
                            title = "Balance",
                            amountCents = report.balanceCents,
                            color = if (report.balanceCents < 0) ExpenseRed else IncomeGreen,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SummaryCard(
                                title = "Ingresos",
                                amountCents = report.incomeCents,
                                color = IncomeGreen,
                                modifier = Modifier.weight(1f),
                                count = report.incomeCount,
                            )
                            SummaryCard(
                                title = "Egresos",
                                amountCents = report.expenseCents,
                                color = ExpenseRed,
                                modifier = Modifier.weight(1f),
                                count = report.expenseCount,
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        CategorySection(
                            title = "Ingresos por categoría",
                            categories = report.categories.filter {
                                it.type == TransactionType.INCOME
                            },
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CategorySection(
                            title = "Egresos por categoría",
                            categories = report.categories.filter {
                                it.type == TransactionType.EXPENSE
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amountCents: Long,
    color: Color,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Cop.format(amountCents),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
            )
            if (count != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transactionCountText(count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    categories: List<ReportCategoryTotal>,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (categories.isEmpty()) {
        Text(
            text = "Sin movimientos",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    categories.forEach { category ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = category.categoryName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = Cop.format(category.amountCents),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatPercentage(category.percentage)} · " +
                            transactionCountText(category.transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun transactionCountText(count: Int): String =
    "$count ${if (count == 1) "transacción" else "transacciones"}"

private fun formatPercentage(value: Double): String =
    String.format(Locale.forLanguageTag("es-CO"), "%.1f %%", value)