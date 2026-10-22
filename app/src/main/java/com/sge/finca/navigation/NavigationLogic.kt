package com.sge.finca.navigation

/**
 * Reglas de qué barras mostrar según la ruta actual.
 * Si mañana se agrega una TopBar, su lógica también va aquí.
 */
object NavigationLogic {
    private val bottomBarRoutes = BottomNavItem.entries.map { it.screen.route }

    fun shouldShowBottomBar(route: String?): Boolean = route in bottomBarRoutes
}
