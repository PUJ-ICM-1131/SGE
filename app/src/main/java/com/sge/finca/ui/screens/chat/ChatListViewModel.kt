package com.sge.finca.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.data.repository.WorkersRepository
import com.sge.finca.domain.repository.ChatRepository
import com.sge.finca.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Mensajes del propietario (#74): una conversación por trabajador activo, la más reciente arriba. */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    chatRepository: ChatRepository,
    workersRepository: WorkersRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChatListUiState> = sessionRepository.session
        .filter { !it.loading }
        .flatMapLatest { session ->
            val farmId = session.farmId ?: return@flatMapLatest flowOf(ChatListUiState(isLoading = false))
            combine(workersRepository.observeWorkers(farmId), chatRepository.observeThreads(farmId)) { workers, threads ->
                val threadByWorker = threads.associateBy { it.workerUid }
                val items = workers
                    .filter { it.isActive }
                    .map { worker ->
                        ChatListItem(
                            workerUid = worker.uid,
                            name = worker.name.ifBlank { worker.email.ifBlank { "Trabajador" } },
                            thread = threadByWorker[worker.uid],
                        )
                    }
                    .sortedWith(
                        compareByDescending<ChatListItem> { it.thread?.lastMessageAt ?: 0L }
                            .thenBy { it.name.lowercase() },
                    )
                ChatListUiState(isLoading = false, items = items, myUid = session.uid)
            }
        }
        .catch { emit(ChatListUiState(isLoading = false, errorMessage = "No se pudieron cargar las conversaciones")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())
}
