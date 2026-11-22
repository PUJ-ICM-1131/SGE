package com.sge.finca

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sge.finca.navigation.AppNavigation
import com.sge.finca.navigation.NavigationLogic
import com.sge.finca.navigation.Screen
import com.sge.finca.ui.components.BottomNavBar

/**
 * Raíz de la app: crea el NavController y el único Scaffold.
 * MainActivity solo debe llamar a FincaApp().
 */
private val authRoutes = setOf(Screen.Splash.route, Screen.Login.route, Screen.Register.route)

@Composable
fun FincaApp(
    /** Pantalla pedida por una notificación (por ejemplo, Recordatorios). */
    pendingRoute: String? = null,
    onPendingRouteHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Se espera a que haya sesión (fuera de splash/login/registro): así, si no la hay,
    // primero se inicia sesión y luego se abre la pantalla pedida.
    LaunchedEffect(pendingRoute, currentRoute) {
        if (pendingRoute != null && currentRoute != null && currentRoute !in authRoutes) {
            navController.navigate(pendingRoute) { launchSingleTop = true }
            onPendingRouteHandled()
        }
    }

    Scaffold(
        bottomBar = {
            if (NavigationLogic.shouldShowBottomBar(currentRoute)) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
