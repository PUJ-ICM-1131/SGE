package com.sge.finca.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.usecase.ObserveFarmCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    observeFarmCategories: ObserveFarmCategoriesUseCase,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var currentFarmId: String? = null

    init {
        viewModelScope.launch {
            combine(
                observeFarmCategories(),
                sessionRepository.session,
            ) { farm, session -> farm to session }
                .collectLatest { (farm, session) ->
                    currentFarmId = farm?.farmId

                    _uiState.update {
                        it.copy(
                            categories = farm?.categories.orEmpty(),
                            hasFarm = farm != null,
                            isOwner = farm != null &&
                                    session.farmId == farm.farmId &&
                                    session.can(Permission.MANAGE_CATEGORIES),
                            isLoading = session.loading,
                        )
                    }
                }
        }
    }

    fun onFilterChanged(showIncome: Boolean) {
        _uiState.update { it.copy(showIncome = showIncome) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun create(
        name: String,
        type: TransactionType,
        onSuccess: () -> Unit,
    ) {
        runMutation(
            errorMessage = "No se pudo crear la categoría",
            onSuccess = onSuccess,
        ) { farmId ->
            categoryRepository.create(farmId, name, type)
        }
    }

    fun rename(
        id: String,
        newName: String,
        onSuccess: () -> Unit,
    ) {
        runMutation(
            errorMessage = "No se pudo renombrar la categoría",
            onSuccess = onSuccess,
        ) { farmId ->
            categoryRepository.rename(farmId, id, newName)
        }
    }

    fun delete(
        id: String,
        onSuccess: () -> Unit,
    ) {
        runMutation(
            errorMessage = "No se pudo eliminar la categoría",
            onSuccess = onSuccess,
        ) { farmId ->
            categoryRepository.delete(farmId, id)
        }
    }

    private fun runMutation(
        errorMessage: String,
        onSuccess: () -> Unit,
        action: suspend (farmId: String) -> Unit,
    ) {
        val farmId = currentFarmId ?: return
        if (!_uiState.value.isOwner || _uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                action(farmId)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: errorMessage)
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}