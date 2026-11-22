package com.sge.finca.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.usecase.ManageRemindersUseCase
import com.sge.finca.ui.format.ReminderTimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    reminderRepository: ReminderRepository,
    private val manageReminders: ManageRemindersUseCase,
) : ViewModel() {

    private data class Data(
        val farmId: String?,
        val userId: String?,
        val isOwner: Boolean,
        val reminders: List<Reminder>,
    )

    /**
     * Datos de Room: se actualizan solos al crear, editar, completar o borrar.
     * El rol sale de la membresía de la finca (#28), no del perfil.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val data: Flow<Data> = sessionRepository.session
        .filter { !it.loading }
        .flatMapLatest { session ->
            val farmId = session.farmId
            if (farmId == null || session.role == null) {
                // Sin finca o con la membresía inactiva no hay recordatorios que mostrar.
                flowOf(Data(null, session.uid, false, emptyList()))
            } else {
                reminderRepository.observeReminders(farmId).map { reminders ->
                    Data(farmId, session.uid, session.isOwner, reminders)
                }
            }
        }

    /** Cada minuto se recalcula qué está vencido, aunque nada cambie en la base. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    /** Parte de la pantalla que no viene de la base: diálogos y mensajes. */
    private data class Ui(
        val editor: ReminderEditorState? = null,
        val pendingDelete: Reminder? = null,
        val message: String? = null,
    )

    private val ui = MutableStateFlow(Ui())
    private var farmId: String? = null

    val uiState: StateFlow<RemindersUiState> = combine(data, clock, ui) { data, now, ui ->
        farmId = data.farmId
        val pending = data.reminders.filter { !it.isCompleted }
        RemindersUiState(
            isLoading = false,
            noFarm = data.farmId == null,
            overdue = pending.filter { it.dueAt <= now }.sortedBy { it.dueAt },
            upcoming = pending.filter { it.dueAt > now }.sortedBy { it.dueAt },
            completed = data.reminders.filter { it.isCompleted }.sortedByDescending { it.updatedAt },
            now = now,
            currentUserId = data.userId,
            isOwner = data.isOwner,
            editor = ui.editor,
            pendingDelete = ui.pendingDelete,
            message = ui.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    // ---------- Crear / editar ----------

    fun onAddClicked() = ui.update {
        it.copy(editor = ReminderEditorState(dueAt = ReminderTimeFormat.nextRoundHour(System.currentTimeMillis())))
    }

    fun onReminderClicked(reminder: Reminder) = ui.update {
        it.copy(
            editor = ReminderEditorState(
                id = reminder.id,
                title = reminder.title,
                description = reminder.description.orEmpty(),
                dueAt = reminder.dueAt,
            ),
        )
    }

    fun onTitleChanged(value: String) = updateEditor { it.copy(title = value, errorMessage = null) }

    fun onDescriptionChanged(value: String) = updateEditor { it.copy(description = value, errorMessage = null) }

    fun onDueAtChanged(dueAt: Long) = updateEditor { it.copy(dueAt = dueAt, errorMessage = null) }

    fun onEditorDismissed() = ui.update { if (it.editor?.isSaving == true) it else it.copy(editor = null) }

    fun onSave() {
        val editor = ui.value.editor ?: return
        val farm = farmId ?: return
        if (editor.isSaving) return // evita doble guardado

        updateEditor { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val description = editor.description.ifBlank { null }
                if (editor.id == null) {
                    manageReminders.create(farm, editor.title, description, editor.dueAt)
                } else {
                    val current = uiState.value.let { it.overdue + it.upcoming + it.completed }
                        .firstOrNull { it.id == editor.id } ?: throw IllegalArgumentException("Recordatorio no encontrado")
                    manageReminders.update(
                        current.copy(title = editor.title, description = description, dueAt = editor.dueAt),
                    )
                }
                ui.update { it.copy(editor = null, message = if (editor.isNew) "Recordatorio creado" else "Cambios guardados") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                updateEditor { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: SecurityException) {
                // El repositorio rechazó la escritura por permisos (#28).
                updateEditor { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: Exception) {
                updateEditor { it.copy(isSaving = false, errorMessage = "No se pudo guardar el recordatorio") }
            }
        }
    }

    // ---------- Completar / eliminar ----------

    fun onToggleCompleted(reminder: Reminder) {
        val farm = farmId ?: return
        viewModelScope.launch {
            runCatchingUi("No se pudo actualizar el recordatorio") {
                manageReminders.setCompleted(farm, reminder.id, !reminder.isCompleted)
            }
        }
    }

    fun onDeleteClicked(reminder: Reminder) {
        if (uiState.value.canDelete(reminder)) ui.update { it.copy(pendingDelete = reminder) }
    }

    fun onDeleteDismissed() = ui.update { it.copy(pendingDelete = null) }

    fun onDeleteConfirmed() {
        val reminder = ui.value.pendingDelete ?: return
        val farm = farmId ?: return
        ui.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            runCatchingUi("No se pudo eliminar el recordatorio") {
                manageReminders.delete(farm, reminder.id)
                ui.update { it.copy(message = "Recordatorio eliminado") }
            }
        }
    }

    fun onMessageShown() = ui.update { it.copy(message = null) }

    private suspend fun runCatchingUi(errorMessage: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            ui.update { it.copy(message = e.message ?: errorMessage) }
        } catch (e: Exception) {
            ui.update { it.copy(message = errorMessage) }
        }
    }

    private fun updateEditor(change: (ReminderEditorState) -> ReminderEditorState) =
        ui.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }
}
