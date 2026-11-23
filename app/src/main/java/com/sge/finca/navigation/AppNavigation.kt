package com.sge.finca.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sge.finca.domain.model.Permission
import com.sge.finca.ui.components.PermissionGate
import com.sge.finca.ui.screens.animals.AnimalDetailScreen
import com.sge.finca.ui.screens.animals.AnimalDetailViewModel
import com.sge.finca.ui.screens.animals.AnimalListScreen
import com.sge.finca.ui.screens.animals.AnimalListViewModel
import com.sge.finca.ui.screens.alerts.AlertDetailScreen
import com.sge.finca.ui.screens.alerts.AlertDetailViewModel
import com.sge.finca.ui.screens.animals.AnimalMapViewModel
import com.sge.finca.ui.screens.chat.ChatListScreen
import com.sge.finca.ui.screens.chat.ChatListViewModel
import com.sge.finca.ui.screens.chat.ChatScreen
import com.sge.finca.ui.screens.chat.ChatViewModel
import com.sge.finca.ui.screens.animals.RegisterAnimalScreen
import com.sge.finca.ui.screens.animals.RegisterAnimalViewModel
import com.sge.finca.ui.screens.auth.BiometricPromptHandler
import com.sge.finca.ui.screens.auth.LoginScreen
import com.sge.finca.ui.screens.auth.LoginScreenViewModel
import com.sge.finca.ui.screens.auth.RegisterScreen
import com.sge.finca.ui.screens.auth.RegisterViewModel
import com.sge.finca.ui.screens.categories.CategoriesScreen
import com.sge.finca.ui.screens.contacts.ContactsScreen
import com.sge.finca.ui.screens.farm.JoinFarmScreen
import com.sge.finca.ui.screens.home.HomeScreen
import com.sge.finca.ui.screens.inspections.InspectionDetailScreen
import com.sge.finca.ui.screens.inspections.InspectionDetailViewModel
import com.sge.finca.ui.screens.inspections.InspectionHistoryScreen
import com.sge.finca.ui.screens.inspections.InspectionHistoryViewModel
import com.sge.finca.ui.screens.inspections.InspectionMapViewModel
import com.sge.finca.ui.screens.map.FarmMapScreen
import com.sge.finca.ui.screens.more.MoreScreen
import com.sge.finca.ui.screens.more.MoreViewModel
import com.sge.finca.ui.screens.routes.RouteMapScreen
import com.sge.finca.ui.screens.routes.RouteMapViewModel
import com.sge.finca.ui.screens.splash.SplashScreen
import com.sge.finca.ui.screens.splash.SplashViewModel
import com.sge.finca.ui.screens.reminders.RemindersScreen
import com.sge.finca.ui.screens.reports.ReportsScreen
import com.sge.finca.ui.screens.settings.SettingsScreen
import com.sge.finca.ui.screens.transactions.EditTransactionScreen
import com.sge.finca.ui.screens.transactions.NewTransactionScreen
import com.sge.finca.ui.screens.transactions.TransactionDetailScreen
import com.sge.finca.ui.screens.transactions.TransactionHistoryScreen
import com.sge.finca.ui.screens.workers.WorkersScreen
import com.sge.finca.ui.session.SessionViewModel

/**
 * Solo define las rutas y qué pasa al interactuar con cada pantalla.
 * El NavController y el Scaffold los crea FincaApp.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Una sola instancia para toda la app (Activity): rol del usuario para ocultar opciones y
    // proteger rutas (issue #28).
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.session.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier,
    ) {
        // Splash: decide si hay sesión abierta
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val splashState by splashViewModel.uiState.collectAsState()

            LaunchedEffect(splashState.navigateHome, splashState.navigateLogin) {
                when {
                    splashState.navigateHome -> navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    splashState.navigateLogin -> navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            SplashScreen()

            // Si el usuario activó la huella, muestra el BiometricPrompt sobre el splash.
            BiometricPromptHandler(
                show = splashState.showBiometricPrompt,
                onLaunch = { activity -> splashViewModel.startBiometricPrompt(activity) },
                onUnavailable = { splashViewModel.onPromptUnavailable() },
            )
        }

        // Auth
        composable(Screen.Login.route) {
            val loginScreenViewModel: LoginScreenViewModel = hiltViewModel()
            val loginState by loginScreenViewModel.uiState.collectAsState()

            // El ViewModel decide (flag navigate); AppNavigation es quien navega.
            LaunchedEffect(loginState.navigate) {
                if (loginState.navigate) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                loginScreenViewModel = loginScreenViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val registerState by registerViewModel.uiState.collectAsState()

            LaunchedEffect(registerState.navigate) {
                if (registerState.navigate) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                registerViewModel = registerViewModel,
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        // Bottom nav destinations
        composable(Screen.Home.route) {
            HomeScreen(
                // El balance lo ve el propietario y el trabajador al que se lo otorgó (#67).
                showBalance = session.can(Permission.VIEW_BALANCE),
                onNavigateToNewTransaction = { navController.navigate(Screen.NewTransaction.route) },
                // Campana: panel de notificaciones (recordatorios). Reportes sigue en "Más".
                onNavigateToNotifications = { navController.navigate(Screen.Reminders.route) },
                // "Ver todo" va a la pestaña Historial igual que la barra inferior.
                onNavigateToHistory = {
                    navController.navigate(Screen.TransactionHistory.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToTransaction = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onNavigateToJoinFarm = { navController.navigate(Screen.JoinFarm.route) },
            )
        }
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                showSummary = session.can(Permission.VIEW_BALANCE),
                onNavigateToNew = { navController.navigate(Screen.NewTransaction.route) },
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
            )
        }
        // Alerta de vaca fuera de los potreros (#73): se abre desde la notificación push.
        composable(
            route = Screen.AlertDetail.route,
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
        ) {
            val alertDetailViewModel: AlertDetailViewModel = hiltViewModel()
            AlertDetailScreen(
                viewModel = alertDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        // Mapa único (#72): reemplaza a las pestañas de Inspecciones y Ganado.
        composable(Screen.FarmMap.route) {
            val inspectionViewModel: InspectionMapViewModel = hiltViewModel()
            val animalMapViewModel: AnimalMapViewModel = hiltViewModel()
            FarmMapScreen(
                inspectionViewModel = inspectionViewModel,
                animalViewModel = animalMapViewModel,
                onNavigateToInspectionHistory = { navController.navigate(Screen.InspectionHistory.route) },
                onNavigateToRouteMap = { navController.navigate(Screen.RouteMap.route) },
                onNavigateToRegisterAnimal = { navController.navigate(Screen.RegisterAnimal.route) },
                onNavigateToAnimalList = { navController.navigate(Screen.AnimalList.route) },
            )
        }
        composable(Screen.More.route) {
            val moreViewModel: MoreViewModel = hiltViewModel()
            val moreState by moreViewModel.uiState.collectAsState()

            LaunchedEffect(moreState.navigateLogin) {
                if (moreState.navigateLogin) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            MoreScreen(
                moreViewModel = moreViewModel,
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToWorkers = { navController.navigate(Screen.Workers.route) },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                showCategories = session.can(Permission.VIEW_TRANSACTIONS),
                showWorkers = session.can(Permission.MANAGE_WORKERS),
                showReports = session.can(Permission.VIEW_BALANCE),
                // Chat (#74): el propietario elige el trabajador; el trabajador va a la suya.
                onNavigateToMessages = {
                    if (session.isOwner) {
                        navController.navigate(Screen.ChatList.route)
                    } else {
                        session.uid?.let { navController.navigate(Screen.Chat.createRoute(it)) }
                    }
                },
                messagesSubtitle = if (session.isOwner) "Chat con tus trabajadores" else "Chat con el propietario",
                showMessages = session.can(Permission.CHAT),
            )
        }

        // Chat propietario ↔ trabajador (#74).
        composable(Screen.ChatList.route) {
            PermissionGate(
                session = session,
                permission = Permission.MANAGE_WORKERS,
                onNavigateBack = { navController.popBackStack() },
            ) {
                val chatListViewModel: ChatListViewModel = hiltViewModel()
                ChatListScreen(
                    viewModel = chatListViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenChat = { uid -> navController.navigate(Screen.Chat.createRoute(uid)) },
                )
            }
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("workerUid") { type = NavType.StringType }),
        ) {
            PermissionGate(
                session = session,
                permission = Permission.CHAT,
                onNavigateBack = { navController.popBackStack() },
            ) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(viewModel = chatViewModel, onNavigateBack = { navController.popBackStack() })
            }
        }

        // Unirse a una finca con código (#29). Al terminar vuelve al inicio, que ya ve la finca.
        composable(Screen.JoinFarm.route) {
            JoinFarmScreen(
                onNavigateBack = { navController.popBackStack() },
                onJoined = { navController.popBackStack(Screen.Home.route, inclusive = false) },
            )
        }

        // Transaction screens
        composable(Screen.NewTransaction.route) {
            NewTransactionScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            TransactionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
            )
        }
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            // El ViewModel lee "transactionId" del SavedStateHandle de esta entrada.
            // Solo el propietario edita transacciones (issue #28).
            PermissionGate(
                session = session,
                permission = Permission.EDIT_TRANSACTION,
                onNavigateBack = { navController.popBackStack() },
            ) {
                EditTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        // Other screens
        composable(Screen.Reports.route) {
            PermissionGate(
                session = session,
                permission = Permission.VIEW_BALANCE,
                onNavigateBack = { navController.popBackStack() },
            ) {
                ReportsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
        composable(Screen.Categories.route) {
            PermissionGate(
                session = session,
                permission = Permission.VIEW_TRANSACTIONS,
                onNavigateBack = { navController.popBackStack() },
            ) {
                CategoriesScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
        composable(Screen.Workers.route) {
            PermissionGate(
                session = session,
                permission = Permission.MANAGE_WORKERS,
                onNavigateBack = { navController.popBackStack() },
            ) {
                WorkersScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenChat = { uid -> navController.navigate(Screen.Chat.createRoute(uid)) },
                )
            }
        }
        composable(Screen.InspectionHistory.route) {
            val historyViewModel: InspectionHistoryViewModel = hiltViewModel()
            InspectionHistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRouteClick = { routeId ->
                    navController.navigate(Screen.InspectionDetail.createRoute(routeId))
                },
            )
        }
        composable(
            route = Screen.InspectionDetail.route,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) {
            val detailViewModel: InspectionDetailViewModel = hiltViewModel()
            InspectionDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.RouteMap.route) {
            val routeMapViewModel: RouteMapViewModel = hiltViewModel()
            RouteMapScreen(
                viewModel = routeMapViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.AnimalList.route) {
            val animalListViewModel: AnimalListViewModel = hiltViewModel()
            AnimalListScreen(
                viewModel = animalListViewModel,
                onNavigateBack = { navController.popBackStack() },
                onAnimalClick = { id -> navController.navigate(Screen.AnimalDetail.createRoute(id)) },
            )
        }
        composable(
            route = Screen.AnimalDetail.route,
            arguments = listOf(navArgument("animalId") { type = NavType.StringType }),
        ) {
            val animalDetailViewModel: AnimalDetailViewModel = hiltViewModel()
            AnimalDetailScreen(
                viewModel = animalDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.RegisterAnimal.route) {
            val registerAnimalViewModel: RegisterAnimalViewModel = hiltViewModel()
            RegisterAnimalScreen(
                viewModel = registerAnimalViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Contacts.route) {
            ContactsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Reminders.route) {
            RemindersScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}