package com.sge.finca.ui.screens.chat

import com.sge.finca.domain.model.ChatMessage

data class ChatUiState(
    val isLoading: Boolean = true,
    /** Nombre del trabajador (para el propietario) o "Propietario" (para el trabajador). */
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val myUid: String? = null,
    val errorMessage: String? = null,
)
