package com.sge.finca.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ítems de la barra inferior (pantalla + icono + etiqueta).
 */
enum class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
) {
    HOME(Screen.Home, Icons.Default.Home, "Inicio"),
    TRANSACTIONS(Screen.TransactionHistory, Icons.Default.Receipt, "Transacciones"),
    INSPECTIONS(Screen.InspectionMap, Icons.Default.Map, "Inspecciones"),
    ANIMALS(Screen.AnimalMap, Icons.Default.Agriculture, "Ganado"),
    MORE(Screen.More, Icons.Default.Menu, "Más"),
}
