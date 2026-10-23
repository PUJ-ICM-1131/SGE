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
import com.sge.finca.ui.screens.animals.AnimalDetailScreen
import com.sge.finca.ui.screens.animals.AnimalDetailViewModel
import com.sge.finca.ui.screens.animals.AnimalListScreen
import com.sge.finca.ui.screens.animals.AnimalListViewModel
import com.sge.finca.ui.screens.animals.AnimalMapScreen
import com.sge.finca.ui.screens.animals.AnimalMapViewModel
import com.sge.finca.ui.screens.animals.RegisterAnimalScreen
import com.sge.finca.ui.screens.animals.RegisterAnimalViewModel
import com.sge.finca.ui.screens.auth.LoginScreen
import com.sge.finca.ui.screens.auth.LoginScreenViewModel
import com.sge.finca.ui.screens.auth.RegisterScreen
import com.sge.finca.ui.screens.auth.RegisterViewModel
import com.sge.finca.ui.screens.categories.CategoriesScreen
import com.sge.finca.ui.screens.contacts.ContactsScreen
import com.sge.finca.ui.screens.home.HomeScreen
import com.sge.finca.ui.screens.inspections.InspectionDetailScreen
import com.sge.finca.ui.screens.inspections.InspectionDetailViewModel
import com.sge.finca.ui.screens.inspections.InspectionHistoryScreen
import com.sge.finca.ui.screens.inspections.InspectionHistoryViewModel
import com.sge.finca.ui.screens.inspections.InspectionMapScreen
import com.sge.finca.ui.screens.inspections.InspectionMapViewModel
import com.sge.finca.ui.screens.more.MoreScreen
import com.sge.finca.ui.screens.more.MoreViewModel
import com.sge.finca.ui.screens.splash.SplashScreen
import com.sge.finca.ui.screens.splash.SplashViewModel
import com.sge.finca.ui.screens.reminders.RemindersScreen
import com.sge.finca.ui.screens.reports.ReportsScreen
import com.sge.finca.ui.screens.settings.SettingsScreen
import com.sge.finca.ui.screens.transactions.EditTransactionScreen
import com.sge.finca.ui.screens.transactions.NewTransactionScreen
import com.sge.finca.ui.screens.transactions.TransactionHistoryScreen
import com.sge.finca.ui.screens.workers.WorkersScreen

/**
 * Solo define las rutas y qué pasa al interactuar con cada pantalla.
 * El NavController y el Scaffold los crea FincaApp.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
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
                onNavigateToNewTransaction = { navController.navigate(Screen.NewTransaction.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
            )
        }
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                onNavigateToNew = { navController.navigate(Screen.NewTransaction.route) },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
            )
        }
        composable(Screen.InspectionMap.route) {
            val inspectionViewModel: InspectionMapViewModel = hiltViewModel()
            InspectionMapScreen(
                viewModel = inspectionViewModel,
                onNavigateToHistory = { navController.navigate(Screen.InspectionHistory.route) },
            )
        }
        composable(Screen.AnimalMap.route) {
            val animalMapViewModel: AnimalMapViewModel = hiltViewModel()
            AnimalMapScreen(
                viewModel = animalMapViewModel,
                onNavigateToRegister = { navController.navigate(Screen.RegisterAnimal.route) },
                onNavigateToList = { navController.navigate(Screen.AnimalList.route) },
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
            )
        }

        // Transaction screens
        composable(Screen.NewTransaction.route) {
            NewTransactionScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            EditTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Other screens
        composable(Screen.Reports.route) {
            ReportsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Categories.route) {
            CategoriesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Workers.route) {
            WorkersScreen(onNavigateBack = { navController.popBackStack() })
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