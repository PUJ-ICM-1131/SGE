package com.sge.finca.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    moreViewModel: MoreViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Más") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text("Categorías") },
                supportingContent = { Text("Gestionar categorías de ingresos y egresos") },
                leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToCategories),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Trabajadores") },
                supportingContent = { Text("Invitar y gestionar trabajadores de la finca") },
                leadingContent = { Icon(Icons.Default.Group, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToWorkers),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Contactos") },
                supportingContent = { Text("Directorio de contactos de la finca") },
                leadingContent = { Icon(Icons.Default.Contacts, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToContacts),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Recordatorios") },
                supportingContent = { Text("Crear y gestionar recordatorios") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToReminders),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Reportes y Balance") },
                supportingContent = { Text("Ver balance, gráficos y exportar reportes") },
                leadingContent = { Icon(Icons.Default.InsertChart, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToReports),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Configuración") },
                supportingContent = { Text("Perfil y preferencias") },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToSettings),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                colors = ListItemDefaults.colors(),
                modifier = Modifier.clickable { moreViewModel.logoutButtonPress() },
            )
        }
    }
}