package com.sge.finca.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.ui.format.BusinessDateFormat
import com.sge.finca.ui.format.Cop
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen

/** Fila de una transacción: la usan el historial y el inicio para que se vean igual. */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** En el inicio no hay encabezados por día, así que se muestra la fecha en la fila. */
    showDate: Boolean = false,
) {
    val color = transaction.type.color()
    val note = transaction.note
    val date = if (showDate) dayLabel(transaction.dateEpochDay) else null
    val supporting = listOfNotNull(date, note).joinToString(" · ").ifEmpty { null }

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { TransactionTypeIcon(transaction.type) },
        headlineContent = { Text(transaction.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = supporting?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        trailingContent = {
            Text(
                transaction.signedAmount(),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        },
    )
}

/** Círculo con flecha: verde hacia arriba para ingreso, roja hacia abajo para egreso. */
@Composable
fun TransactionTypeIcon(type: TransactionType, size: Int = 40) {
    val color = type.color()
    val isIncome = type == TransactionType.INCOME
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = if (isIncome) "Ingreso" else "Egreso",
            tint = color,
        )
    }
}

fun TransactionType.color(): Color = if (this == TransactionType.INCOME) IncomeGreen else ExpenseRed

/** "+$50.000" para ingresos, "-$30.000" para egresos. */
fun Transaction.signedAmount(): String =
    (if (type == TransactionType.INCOME) "+" else "-") + Cop.format(amountCents)

/** "Hoy", "Ayer" o "20 oct 2026". */
fun dayLabel(day: Long, today: Long = com.sge.finca.domain.model.BusinessDate.today()): String = when (day) {
    today -> "Hoy"
    today - 1 -> "Ayer"
    else -> BusinessDateFormat.format(day)
}
