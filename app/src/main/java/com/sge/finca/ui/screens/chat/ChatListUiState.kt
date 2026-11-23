package com.sge.finca.ui.screens.chat

import com.sge.finca.domain.model.ChatThread

/** Una fila de Mensajes: un trabajador activo y su conversación, si ya existe. */
data class ChatListItem(
    val workerUid: String,
    val name: String,
    val thread: ChatThread?,
)

data class ChatListUiState(
    val isLoading: Boolean = true,
    val items: List<ChatListItem> = emptyList(),
    val myUid: String? = null,
    val errorMessage: String? = null,
)
