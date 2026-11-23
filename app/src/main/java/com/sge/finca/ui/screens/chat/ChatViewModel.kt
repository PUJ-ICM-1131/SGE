package com.sge.finca.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.WorkersRepository
import com.sge.finca.domain.repository.ChatRepository
import com.sge.finca.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Conversación entre el propietario y el trabajador [workerUid] (#74). */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val chatRepository: ChatRepository,
    workersRepository: WorkersRepository,
) : ViewModel() {

    private val workerUid: String = checkNotNull(savedStateHandle["workerUid"])
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChatUiState> = sessionRepository.session
        .filter { !it.loading }
        .flatMapLatest { session ->
            val farmId = session.farmId ?: return@flatMapLatest flowOf(ChatUiState(isLoading = false))
            val title: Flow<String> = if (session.isOwner) {
                workersRepository.observeWorkers(farmId).map { workers ->
                    workers.firstOrNull { it.uid == workerUid }?.name?.ifBlank { null } ?: "Trabajador"
                }
            } else {
                flowOf("Propietario")
            }
            combine(chatRepository.observeMessages(farmId, workerUid), title, error) { messages, name, pendingError ->
                ChatUiState(
                    isLoading = false,
                    title = name,
                    messages = messages,
                    myUid = session.uid,
                    errorMessage = pendingError,
                )
            }
        }
        .catch { emit(ChatUiState(isLoading = false, errorMessage = "No se pudo cargar la conversación")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun onSend(text: String) {
        val farmId = sessionRepository.session.value.farmId ?: return
        viewModelScope.launch {
            try {
                chatRepository.send(farmId, workerUid, text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = e.message ?: "No se pudo enviar el mensaje"
            }
        }
    }

    fun onErrorShown() {
        error.value = null
    }
}
