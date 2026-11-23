package com.sge.finca.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.messages.isEmpty() -> Text(
                        "Escribe el primer mensaje",
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    // Al revés: el más nuevo queda abajo y la lista arranca ahí.
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        items(state.messages.asReversed(), key = { it.id }) { message ->
                            MessageBubble(message, mine = message.senderUid == state.myUid)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(ChatMessage.MAX_LENGTH) },
                    placeholder = { Text("Escribe un mensaje") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        viewModel.onSend(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, mine: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatChatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/** Hoy: solo la hora; otros días: fecha y hora. */
internal fun formatChatTime(timestamp: Long): String {
    val locale = Locale.forLanguageTag("es-CO")
    val today = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = today.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "dd/MM HH:mm"
    return SimpleDateFormat(pattern, locale).format(Date(timestamp))
}
