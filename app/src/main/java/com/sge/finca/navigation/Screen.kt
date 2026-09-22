package com.sge.finca.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // Main (bottom nav)
    object Home : Screen("home")
    object TransactionHistory : Screen("transaction_history")
    object InspectionMap : Screen("inspection_map")
    object AnimalMap : Screen("animal_map")
    object More : Screen("more")

    // Transactions
    object NewTransaction : Screen("new_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    // Reports
    object Reports : Screen("reports")

    // Categories
    object Categories : Screen("categories")

    // Workers
    object Workers : Screen("workers")

    // Inspections
    object InspectionHistory : Screen("inspection_history")

    // Animals
    object RegisterAnimal : Screen("register_animal")

    // Contacts
    object Contacts : Screen("contacts")

    // Reminders
    object Reminders : Screen("reminders")

    // Settings
    object Settings : Screen("settings")
}

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
