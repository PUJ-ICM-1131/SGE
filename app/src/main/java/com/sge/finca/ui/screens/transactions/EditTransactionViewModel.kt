package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.usecase.ObserveFarmCategoriesUseCase
import com.sge.finca.ui.format.Cop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeFarmCategories: ObserveFarmCategoriesUseCase,
    sessionRepository: SessionRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private var farmId: String? = null
    private var original: Transaction? = null

    init {
        viewModelScope.launch {
            combine(observeFarmCategories(), sessionRepository.session) { farm, session ->
                farm to session
            }.collect { (farm, session) ->
                if (farm == null) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@collect
                }
                farmId = farm.farmId
                // Editar y eliminar: solo el propietario (#28). El rol sale de la membresía.
                val canManage = session.can(Permission.EDIT_TRANSACTION) &&
                    session.can(Permission.DELETE_TRANSACTION)
                _uiState.update {
                    it.copy(canManage = canManage, form = it.form.copy(categories = farm.categories))
                }
                if (original == null) load(farm.farmId)
            }
        }
    }

    private suspend fun load(farmId: String) {
        val transaction = transactionRepository.getTransaction(farmId, transactionId)
        if (transaction == null) {
            _uiState.update { it.copy(isLoading = false, notFound = true) }
            return
        }
        original = transaction
        _uiState.update { state ->
            val categoryExists = state.form.categories.any { it.id == transaction.categoryId }
            state.copy(
                isLoading = false,
                notFound = false,
                createdByName = transaction.createdByName,
                originalCategoryName = transaction.categoryName.takeUnless { categoryExists },
                form = state.form.copy(
                    type = transaction.type,
                    amountDigits = Cop.centsToInput(transaction.amountCents),
                    categoryId = transaction.categoryId.takeIf { categoryExists },
                    dateEpochDay = transaction.dateEpochDay,
                    note = transaction.note.orEmpty(),
                ),
            )
        }
    }

    fun onTypeChanged(type: TransactionType) = updateForm { it.withType(type) }

    fun onAmountChanged(raw: String) = updateForm { it.copy(amountDigits = Cop.sanitizeInput(raw)) }

    fun onCategorySelected(categoryId: String) = updateForm { it.copy(categoryId = categoryId) }

    fun onDateSelected(epochDay: Long) = updateForm { it.copy(dateEpochDay = epochDay) }

    fun onNoteChanged(value: String) =
        updateForm { it.copy(note = value.take(TransactionFormState.MAX_NOTE_LENGTH)) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canManage || state.isSaving || state.finished) return
        val farm = farmId ?: return
        val current = original ?: return

        val input = when (val result = state.form.validate()) {
            is TransactionFormValidation.Invalid -> {
                _uiState.update { it.copy(errorMessage = result.message) }
                return
            }
            // Conserva lo que este formulario no edita (método de pago y foto).
            is TransactionFormValidation.Valid ->
                result.input.copy(paymentMethod = current.paymentMethod, photoUri = current.photoUri)
        }

        runAction(errorMessage = "No se pudieron guardar los cambios") {
            transactionRepository.update(farm, transactionId, input)
        }
    }

    fun onDeleteClicked() {
        if (_uiState.value.canManage) _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun onDeleteDismissed() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun onDeleteConfirmed() {
        val state = _uiState.value
        _uiState.update { it.copy(showDeleteDialog = false) }
        if (!state.canManage || state.isSaving || state.finished) return
        val farm = farmId ?: return
        runAction(errorMessage = "No se pudo eliminar el movimiento") {
            transactionRepository.delete(farm, transactionId)
        }
    }

    private fun runAction(errorMessage: String, action: suspend () -> Unit) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                action()
                _uiState.update { it.copy(isSaving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: SecurityException) {
                // El repositorio rechazó la escritura por permisos (#28).
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = errorMessage) }
            }
        }
    }

    private fun updateForm(change: (TransactionFormState) -> TransactionFormState) {
        _uiState.update { it.copy(form = change(it.form), errorMessage = null) }
    }
}
