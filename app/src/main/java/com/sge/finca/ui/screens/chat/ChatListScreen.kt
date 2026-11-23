package com.sge.finca.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onNavigateBack: () -> Unit,
    onOpenChat: (workerUid: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        val centered = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp)
        when {
            state.isLoading -> Box(centered, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null -> Box(centered, contentAlignment = Alignment.Center) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            state.items.isEmpty() -> Box(centered, contentAlignment = Alignment.Center) {
                Text(
                    "Aún no hay trabajadores activos. Invítalos desde Trabajadores con el código de la finca.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(state.items, key = { it.workerUid }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = {
                            Text(
                                text = item.thread?.let { thread ->
                                    (if (thread.lastSenderUid == state.myUid) "Tú: " else "") + thread.lastMessage
                                } ?: "Sin mensajes todavía",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            item.thread?.let {
                                Text(formatChatTime(it.lastMessageAt), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.clickable { onOpenChat(item.workerUid) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
