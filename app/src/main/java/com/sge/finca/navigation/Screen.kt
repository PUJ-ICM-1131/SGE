package com.sge.finca.navigation

/**
 * Todas las rutas de la app. Nunca usar strings sueltos: usar Screen.X.route
 */
sealed class Screen(val route: String) {
    // Auth
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main (bottom nav)
    data object Home : Screen("home")
    data object TransactionHistory : Screen("transaction_history")
    data object InspectionMap : Screen("inspection_map")
    data object AnimalMap : Screen("animal_map")
    data object More : Screen("more")

    // Transactions
    data object NewTransaction : Screen("new_transaction")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    // Reports
    data object Reports : Screen("reports")

    // Categories
    data object Categories : Screen("categories")

    // Workers
    data object Workers : Screen("workers")

    // Inspections
    data object InspectionHistory : Screen("inspection_history")
    data object InspectionDetail : Screen("inspection_detail/{routeId}") {
        fun createRoute(routeId: String) = "inspection_detail/$routeId"
    }

    // Animals
    data object RegisterAnimal : Screen("register_animal")
    data object AnimalList : Screen("animal_list")

    // Contacts
    data object Contacts : Screen("contacts")

    // Reminders
    data object Reminders : Screen("reminders")

    // Settings
    data object Settings : Screen("settings")
}
