package com.sge.finca.ui.screens.reminders

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.usecase.ReminderValidation
import com.sge.finca.ui.format.ReminderTimeFormat
import com.sge.finca.ui.theme.AmberSecondary
import com.sge.finca.ui.theme.ExpenseRed
import com.sge.finca.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RemindersScreen(
    onNavigateBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // POST_NOTIFICATIONS solo existe desde Android 13; antes las notificaciones no piden permiso.
    val notificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }
    val notificationsAllowed = notificationPermission?.status?.isGranted ?: true
    var permissionAsked by rememberSaveable { mutableStateOf(false) }

    fun requestNotifications() {
        val permission = notificationPermission ?: return
        if (!permissionAsked || permission.status.shouldShowRationale) {
            permissionAsked = true
            permission.launchPermissionRequest()
        } else {
            // El usuario lo negó definitivamente: solo se puede activar desde Ajustes.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    state.editor?.let { editor ->
        ReminderEditorDialog(
            editor = editor,
            onTitleChanged = viewModel::onTitleChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onDueAtChanged = viewModel::onDueAtChanged,
            onSave = viewModel::onSave,
            onDismiss = viewModel::onEditorDismissed,
        )
    }

    state.pendingDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("\"${reminder.title}\" se eliminará y no volverá a sonar.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismissed) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.noFarm && !state.isLoading) {
                FloatingActionButton(onClick = {
                    // Se pide el permiso al crear el primero; si lo niega, el recordatorio igual se crea.
                    if (!notificationsAllowed && !permissionAsked) requestNotifications()
                    viewModel.onAddClicked()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo recordatorio")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.noFarm -> Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Tu cuenta aún no pertenece a una finca.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                if (!notificationsAllowed) {
                    item(key = "permission") { NotificationsOffCard(onEnable = { requestNotifications() }) }
                }
                if (state.isEmpty) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No hay recordatorios", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Toca + para programar una vacuna, un pago o una compra",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                section("Vencidos", state.overdue, state, viewModel)
                section("Próximos", state.upcoming, state, viewModel)
                section("Completados", state.completed, state, viewModel)
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    reminders: List<Reminder>,
    state: RemindersUiState,
    viewModel: RemindersViewModel,
) {
    if (reminders.isEmpty()) return
    item(key = "header-$title") {
        Text(
            "$title (${reminders.size})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    items(reminders, key = { it.id }) { reminder ->
        ReminderRow(
            reminder = reminder,
            status = reminder.statusAt(state.now),
            canDelete = state.canDelete(reminder),
            onClick = { viewModel.onReminderClicked(reminder) },
            onToggleCompleted = { viewModel.onToggleCompleted(reminder) },
            onDelete = { viewModel.onDeleteClicked(reminder) },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    status: ReminderStatus,
    canDelete: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    val (statusColor, statusLabel) = when (status) {
        ReminderStatus.PENDING -> AmberSecondary to "Pendiente"
        ReminderStatus.OVERDUE -> ExpenseRed to "Vencido"
        ReminderStatus.COMPLETED -> IncomeGreen to "Completado"
    }
    val done = status == ReminderStatus.COMPLETED
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Checkbox(checked = done, onCheckedChange = { onToggleCompleted() }) },
        headlineContent = {
            Text(
                reminder.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = {
            Column {
                reminder.description?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Text(
                    "$statusLabel · ${ReminderTimeFormat.format(reminder.dueAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        },
        trailingContent = if (canDelete) {
            {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun NotificationsOffCard(onEnable: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsOff, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "Las notificaciones están desactivadas: los recordatorios solo se verán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnable) { Text("Activar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorDialog(
    editor: ReminderEditorState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDueAtChanged: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.isNew) "Nuevo recordatorio" else "Editar recordatorio") },
        text = {
            Column {
                OutlinedTextField(
                    value = editor.title,
                    onValueChange = { onTitleChanged(it.take(ReminderValidation.MAX_TITLE)) },
                    label = { Text("Título") },
                    placeholder = { Text("Vacunar terneros") },
                    singleLine = true,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = { onDescriptionChanged(it.take(ReminderValidation.MAX_DESCRIPTION)) },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, enabled = !editor.isSaving, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(ReminderTimeFormat.formatDate(editor.dueAt), maxLines = 1)
                    }
                    OutlinedButton(onClick = { showTimePicker = true }, enabled = !editor.isSaving) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(ReminderTimeFormat.formatTime(editor.dueAt), maxLines = 1)
                    }
                }
                editor.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !editor.isSaving) {
                Text(if (editor.isSaving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !editor.isSaving) { Text("Cancelar") } },
    )

    if (showDatePicker) {
        val todayUtc = ReminderTimeFormat.toPickerDate(System.currentTimeMillis())
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = ReminderTimeFormat.toPickerDate(editor.dueAt),
            selectableDates = object : SelectableDates {
                // No se programan recordatorios en días pasados.
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { date ->
                        onDueAtChanged(
                            ReminderTimeFormat.combine(
                                date,
                                ReminderTimeFormat.hourOf(editor.dueAt),
                                ReminderTimeFormat.minuteOf(editor.dueAt),
                            ),
                        )
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = ReminderTimeFormat.hourOf(editor.dueAt),
            initialMinute = ReminderTimeFormat.minuteOf(editor.dueAt),
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onDueAtChanged(
                        ReminderTimeFormat.combine(
                            ReminderTimeFormat.toPickerDate(editor.dueAt),
                            timeState.hour,
                            timeState.minute,
                        ),
                    )
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
        )
    }
}

