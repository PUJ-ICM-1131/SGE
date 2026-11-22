package com.sge.finca.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.usecase.ObserveFarmCategoriesUseCase
import com.sge.finca.ui.format.Cop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    observeFarmCategories: ObserveFarmCategoriesUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState.asStateFlow()

    private var farmId: String? = null

    init {
        viewModelScope.launch {
            observeFarmCategories().collect { farm ->
                farmId = farm?.farmId
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        noFarm = farm == null,
                        form = it.form.copy(categories = farm?.categories.orEmpty()),
                    )
                }
            }
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
        // Evita el doble envío: un segundo toque mientras guarda no hace nada.
        if (state.isSaving || state.saveSuccess) return
        val farm = farmId ?: return

        val input = when (val result = state.form.validate()) {
            is TransactionFormValidation.Invalid -> {
                _uiState.update { it.copy(errorMessage = result.message) }
                return
            }
            is TransactionFormValidation.Valid -> result.input
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Room primero (funciona sin internet); Firestore se sincroniza en segundo plano.
                transactionRepository.create(farm, input)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: SecurityException) {
                // Sin finca o membresía inactiva: el repositorio no deja registrar (#28).
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "No se pudo guardar el movimiento") }
            }
        }
    }

    private fun updateForm(change: (TransactionFormState) -> TransactionFormState) {
        _uiState.update { it.copy(form = change(it.form), errorMessage = null) }
    }
}
