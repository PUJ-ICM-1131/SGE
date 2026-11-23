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
    /** Mapa único (#72): vacas, potreros e inspección. */
    data object FarmMap : Screen("farm_map")

    // Chat propietario ↔ trabajador (#74). La conversación se identifica con el uid del trabajador.
    data object ChatList : Screen("chats")
    data object Chat : Screen("chat/{workerUid}") {
        fun createRoute(workerUid: String) = "chat/$workerUid"
    }

    // Alertas de vacas fuera de los potreros (#73); se abre desde la notificación.
    data object AlertDetail : Screen("alert_detail/{alertId}") {
        fun createRoute(alertId: String) = "alert_detail/$alertId"
    }
    data object More : Screen("more")

    // Transactions
    data object NewTransaction : Screen("new_transaction")
    data object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    // Reports
    data object Reports : Screen("reports")

    // Categories
    data object Categories : Screen("categories")

    // Workers
    data object Workers : Screen("workers")
    data object JoinFarm : Screen("join_farm")

    // Inspections
    data object InspectionHistory : Screen("inspection_history")
    data object InspectionDetail : Screen("inspection_detail/{routeId}") {
        fun createRoute(routeId: String) = "inspection_detail/$routeId"
    }

    // Routes
    data object RouteMap : Screen("route_map")

    // Animals
    data object RegisterAnimal : Screen("register_animal")
    data object AnimalList : Screen("animal_list")
    data object AnimalDetail : Screen("animal_detail/{animalId}") {
        fun createRoute(animalId: String) = "animal_detail/$animalId"
    }

    // Contacts
    data object Contacts : Screen("contacts")

    // Reminders
    data object Reminders : Screen("reminders")

    // Settings
    data object Settings : Screen("settings")
}
