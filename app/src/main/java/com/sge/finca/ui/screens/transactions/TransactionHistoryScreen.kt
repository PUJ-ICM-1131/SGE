package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.components.ShakeHandler
import com.sge.finca.ui.format.BusinessDateFormat
import com.sge.finca.ui.format.Cop
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
    onNavigateToNew: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    // Balance y totales del periodo: ocultos sin VIEW_BALANCE (#67).
    showSummary: Boolean = true,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRangePicker by rememberSaveable { mutableStateOf(false) }

    // Agitar el celular abre "Nueva transacción" (#13).
    ShakeHandler(
        onStart = viewModel::startShakeDetection,
        onStop = viewModel::stopShakeDetection,
        onShake = onNavigateToNew,
    )

    if (showRangePicker) {
        RangePickerDialog(
            initialFrom = state.filter.customFrom,
            initialTo = state.filter.customTo,
            onDismiss = { showRangePicker = false },
            onConfirm = { from, to ->
                viewModel.onCustomRangeSelected(from, to)
                showRangePicker = false
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de transacciones") }) },
        floatingActionButton = {
            if (!state.noFarm) {
                FloatingActionButton(onClick = onNavigateToNew) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva transacción")
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> CenteredBox(Modifier.padding(innerPadding)) { CircularProgressIndicator() }
            state.noFarm -> CenteredBox(Modifier.padding(innerPadding)) {
                Text("Tu cuenta aún no pertenece a una finca.", textAlign = TextAlign.Center)
            }
            state.errorMessage != null -> CenteredBox(Modifier.padding(innerPadding)) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            else -> HistoryContent(
                state = state,
                contentPadding = innerPadding,
                onTypeSelected = viewModel::onTypeSelected,
                onPeriodSelected = { period ->
                    if (period == HistoryPeriod.CUSTOM) showRangePicker = true else viewModel.onPeriodSelected(period)
                },
                onCategorySelected = viewModel::onCategorySelected,
                onClearFilters = viewModel::onClearFilters,
                onTransactionClick = onNavigateToDetail,
                showSummary = showSummary,
            )
        }
    }
}

@Composable
private fun HistoryContent(
    state: TransactionHistoryUiState,
    contentPadding: PaddingValues,
    showSummary: Boolean,
    onTypeSelected: (TransactionType?) -> Unit,
    onPeriodSelected: (HistoryPeriod) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    val today = BusinessDate.today()
    // Agrupadas por día, conservando el orden (más reciente primero) que entrega Room.
    val byDay = remember(state.transactions) { state.transactions.groupBy { it.dateEpochDay } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp, // espacio para el FAB
        ),
    ) {
        item(key = "filters") {
            Filters(
                state = state,
                onTypeSelected = onTypeSelected,
                onPeriodSelected = onPeriodSelected,
                onCategorySelected = onCategorySelected,
                onClearFilters = onClearFilters,
            )
        }
        if (showSummary) {
            item(key = "summary") { SummaryCard(state.filter.periodDescription(), state.summary) }
        }

        if (state.transactions.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (state.filter.isDefault) "No hay transacciones" else "No hay transacciones con estos filtros",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.filter.isDefault) {
                        TextButton(onClick = onClearFilters) { Text("Quitar filtros") }
                    }
                }
            }
        } else {
            byDay.forEach { (day, transactions) ->
                item(key = "day-$day") { DayHeader(dayLabel(day, today)) }
                items(transactions, key = { it.id }) { transaction ->
                    TransactionListItem(transaction, onClick = { onTransactionClick(transaction.id) })
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun Filters(
    state: TransactionHistoryUiState,
    onTypeSelected: (TransactionType?) -> Unit,
    onPeriodSelected: (HistoryPeriod) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    val filter = state.filter
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ChipRow {
            FilterChip(filter.type == null, { onTypeSelected(null) }, label = { Text("Todos") })
            FilterChip(filter.type == TransactionType.INCOME, { onTypeSelected(TransactionType.INCOME) }, label = { Text("Ingresos") })
            FilterChip(filter.type == TransactionType.EXPENSE, { onTypeSelected(TransactionType.EXPENSE) }, label = { Text("Egresos") })
        }
        ChipRow {
            HistoryPeriod.entries.forEach { period ->
                FilterChip(
                    selected = filter.period == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(period.label) },
                    leadingIcon = if (period == HistoryPeriod.CUSTOM) {
                        { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryFilterChip(state, onCategorySelected)
            Spacer(Modifier.weight(1f))
            if (!filter.isDefault) TextButton(onClick = onClearFilters) { Text("Quitar filtros") }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun CategoryFilterChip(state: TransactionHistoryUiState, onCategorySelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = state.filter.categoryId != null,
            onClick = { expanded = true },
            label = { Text("Categoría: ${state.selectedCategory?.name ?: "Todas"}") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todas") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                },
            )
            state.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(periodDescription: String, summary: HistorySummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(periodDescription, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                Cop.format(summary.balanceCents),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (summary.balanceCents < 0) ExpenseRed else IncomeGreen,
            )
            Text("Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row {
                SummaryValue("Ingresos", "+" + Cop.format(summary.incomeCents), IncomeGreen, Modifier.weight(1f))
                SummaryValue("Egresos", "-" + Cop.format(summary.expenseCents), ExpenseRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    initialFrom: Long?,
    initialTo: Long?,
    onDismiss: () -> Unit,
    onConfirm: (from: Long, to: Long) -> Unit,
) {
    val todayMillis = BusinessDateFormat.toUtcMillis(BusinessDate.today())
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.let(BusinessDateFormat::toUtcMillis),
        initialSelectedEndDateMillis = initialTo?.let(BusinessDateFormat::toUtcMillis),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = pickerState.selectedStartDateMillis
            TextButton(
                enabled = start != null,
                onClick = {
                    // Si solo eligió un día, el rango es ese día.
                    val end = pickerState.selectedEndDateMillis ?: start!!
                    onConfirm(BusinessDateFormat.fromUtcMillis(start!!), BusinessDateFormat.fromUtcMillis(end))
                },
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DateRangePicker(
            state = pickerState,
            title = { Text("Elige el rango", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            modifier = Modifier.weight(1f),
        )
    }
}
