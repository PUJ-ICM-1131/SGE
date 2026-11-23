package com.sge.finca.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    var name by remember { mutableStateOf("") }

    val selectedType = if (state.showIncome) {
        TransactionType.INCOME
    } else {
        TransactionType.EXPENSE
    }

    val visibleCategories = state.categories.filter { it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.hasFarm && state.isOwner) {
                FloatingActionButton(
                    onClick = {
                        viewModel.clearError()
                        name = ""
                        creating = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Crear categoría",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.showIncome,
                    onClick = { viewModel.onFilterChanged(true) },
                    label = { Text("Ingresos") },
                )
                FilterChip(
                    selected = !state.showIncome,
                    onClick = { viewModel.onFilterChanged(false) },
                    label = { Text("Egresos") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (
                state.errorMessage != null &&
                !creating &&
                editing == null &&
                deleting == null
            ) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                state.isLoading -> CircularProgressIndicator()

                !state.hasFarm -> Text(
                    text = "Esta cuenta todavía no tiene una finca asociada",
                )

                visibleCategories.isEmpty() -> Text(
                    text = "No hay categorías de este tipo",
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = visibleCategories,
                        key = { it.id },
                    ) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (category.isDefault) {
                                    Text(
                                        text = "Predeterminada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (state.isOwner) {
                                IconButton(
                                    onClick = {
                                        viewModel.clearError()
                                        name = category.name
                                        editing = category
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renombrar ${category.name}",
                                    )
                                }

                                if (!category.isDefault) {
                                    IconButton(
                                        onClick = {
                                            viewModel.clearError()
                                            deleting = category
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar ${category.name}",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    creating = false
                    editing = null
                    viewModel.clearError()
                }
            },
            title = {
                Text(
                    if (creating) "Nueva categoría" else "Renombrar categoría"
                )
            },
            text = {
                Column {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSaving && name.isNotBlank(),
                    onClick = {
                        if (creating) {
                            viewModel.create(name, selectedType) {
                                creating = false
                            }
                        } else {
                            editing?.let { category ->
                                viewModel.rename(category.id, name) {
                                    editing = null
                                }
                            }
                        }
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        creating = false
                        editing = null
                        viewModel.clearError()
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    deleting = null
                    viewModel.clearError()
                }
            },
            title = { Text("Eliminar categoría") },
            text = {
                Column {
                    Text("¿Eliminar «${category.name}»?")

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        viewModel.delete(category.id) {
                            deleting = null
                        }
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        deleting = null
                        viewModel.clearError()
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}