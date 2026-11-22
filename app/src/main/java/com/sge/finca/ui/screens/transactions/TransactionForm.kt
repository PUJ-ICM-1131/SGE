package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sge.finca.domain.model.BusinessDate
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.components.ThousandsSeparatorTransformation
import com.sge.finca.ui.format.BusinessDateFormat
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen

/**
 * Formulario de transacción compartido por "Nueva" y "Editar".
 * Sin estado propio de negocio: todo llega en [form] y sale por los callbacks.
 */
@Composable
fun TransactionForm(
    form: TransactionFormState,
    enabled: Boolean,
    onTypeChanged: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TypeSelector(selected = form.type, enabled = enabled, onTypeChanged = onTypeChanged)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = form.amountDigits,
            onValueChange = onAmountChanged,
            label = { Text("Monto (COP)") },
            prefix = { Text("$") },
            placeholder = { Text("0") },
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        CategoryPicker(form = form, enabled = enabled, onCategorySelected = onCategorySelected)
        Spacer(Modifier.height(12.dp))

        DateField(dateEpochDay = form.dateEpochDay, enabled = enabled, onDateSelected = onDateSelected)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChanged,
            label = { Text("Nota (opcional)") },
            supportingText = { Text("${form.note.length}/${TransactionFormState.MAX_NOTE_LENGTH}") },
            enabled = enabled,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TypeSelector(
    selected: TransactionType,
    enabled: Boolean,
    onTypeChanged: (TransactionType) -> Unit,
) {
    Row {
        TypeChip("Ingreso", TransactionType.INCOME, selected, enabled, onTypeChanged)
        Spacer(Modifier.width(8.dp))
        TypeChip("Egreso", TransactionType.EXPENSE, selected, enabled, onTypeChanged)
    }
}

@Composable
private fun TypeChip(
    label: String,
    type: TransactionType,
    selected: TransactionType,
    enabled: Boolean,
    onTypeChanged: (TransactionType) -> Unit,
) {
    val color = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
    FilterChip(
        selected = selected == type,
        onClick = { onTypeChanged(type) },
        label = { Text(label) },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color,
        ),
    )
}

/** Campo de solo lectura que abre un menú con las categorías del tipo elegido. */
@Composable
private fun CategoryPicker(
    form: TransactionFormState,
    enabled: Boolean,
    onCategorySelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = form.categoriesForType

    Box {
        OutlinedTextField(
            value = form.selectedCategory?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            placeholder = { Text(if (options.isEmpty()) "Cargando categorías…" else "Selecciona una") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa encima del campo: un TextField de solo lectura no recibe clics por sí mismo.
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = enabled && options.isNotEmpty()) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { category ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    dateEpochDay: Long,
    enabled: Boolean,
    onDateSelected: (Long) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = BusinessDateFormat.format(dateEpochDay),
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha") },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showPicker = true },
        )
    }

    if (showPicker) {
        val todayMillis = BusinessDateFormat.toUtcMillis(BusinessDate.today())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = BusinessDateFormat.toUtcMillis(dateEpochDay),
            selectableDates = object : SelectableDates {
                // No se registran movimientos con fecha futura.
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(BusinessDateFormat.fromUtcMillis(it)) }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
