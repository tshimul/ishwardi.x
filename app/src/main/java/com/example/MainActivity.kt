package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.IshwardiBottomBar
import com.example.ui.components.IshwardiTopAppBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.admin.AdminScreen
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.detail.ProviderDetailScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.IshwardiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as IshwardiApp
        val providerRepository = app.repository
        val adminRepository = app.adminRepository

        // Start Firestore real-time synchronization in public mode
        providerRepository.startRealtimeSync(isAdmin = false)

        setContent {
            IshwardiTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(providerRepository) as T
                        }
                    }
                )

                val adminViewModel: AdminViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return AdminViewModel(adminRepository, providerRepository) as T
                        }
                    }
                )

                IshwardiAppNavigation(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    adminViewModel = adminViewModel,
                    providerRepository = providerRepository,
                    snackbarHostState = snackbarHostState,
                    onShowSnackbar = { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IshwardiAppNavigation(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    providerRepository: com.example.data.repository.ProviderRepository,
    snackbarHostState: SnackbarHostState,
    onShowSnackbar: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val syncStatus by homeViewModel.syncStatus.collectAsState()

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Categories.route,
        Screen.Favorites.route,
        Screen.About.route
    )
    val isTopLevel = currentRoute in topLevelRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopLevel) {
                IshwardiTopAppBar(
                    syncStatus = syncStatus,
                    onNavigateToAdmin = { navController.navigate(Screen.AdminPortal.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onTriggerSync = {
                        homeViewModel.triggerSync()
                        onShowSnackbar("Directory synchronized successfully")
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                IshwardiBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ProviderDetail.createRoute(id))
                    },
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route)
                    },
                    onShowSnackbar = onShowSnackbar
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = homeViewModel,
                    onCategorySelected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ProviderDetail.createRoute(id))
                    },
                    onShowSnackbar = onShowSnackbar
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdmin = { navController.navigate(Screen.AdminPortal.route) }
                )
            }

            composable(Screen.AdminPortal.route) {
                AdminScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onShowSnackbar = onShowSnackbar
                )
            }

            composable(
                route = Screen.ProviderDetail.route,
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                ProviderDetailScreen(
                    providerId = providerId,
                    repository = providerRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onShowSnackbar = onShowSnackbar
                )
            }
        }
    }
}
