package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ViewModelFactory
import com.example.ui.billing.BillingScreen
import com.example.ui.billing.BillingViewModel
import com.example.ui.cart.CartScreen
import com.example.ui.cart.CartViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.kot.KotScreen
import com.example.ui.kot.KotViewModel
import com.example.ui.login.LoginScreen
import com.example.ui.login.LoginViewModel
import com.example.ui.menu.MenuScreen
import com.example.ui.menu.MenuViewModel
import com.example.ui.navigation.Screen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.settlement.SettlementScreen
import com.example.ui.settlement.SettlementViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.splash.SplashViewModel
import com.example.ui.tables.OpenTableScreen
import com.example.ui.tables.TablesScreen
import com.example.ui.tables.TablesViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CaptainApplication).container
        val factory = ViewModelFactory(appContainer.repository)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Splash.route) {
                            val splashViewModel: SplashViewModel = viewModel(factory = factory)
                            SplashScreen(
                                viewModel = splashViewModel,
                                onNavigateToLogin = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                },
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Login.route) {
                            val loginViewModel: LoginViewModel = viewModel(factory = factory)
                            LoginScreen(
                                viewModel = loginViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToTables = { navController.navigate(Screen.Tables.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                            )
                        }

                        composable(Screen.Tables.route) {
                            val tablesViewModel: TablesViewModel = viewModel(factory = factory)
                            TablesScreen(
                                viewModel = tablesViewModel,
                                onNavigateToOpenTable = { tableId -> navController.navigate(Screen.OpenTable.createRoute(tableId)) },
                                onNavigateToMenu = { tableId -> navController.navigate(Screen.Menu.createRoute(tableId)) },
                                onNavigateToCart = { tableId -> navController.navigate(Screen.Cart.createRoute(tableId)) },
                                onNavigateToKot = { tableId -> navController.navigate(Screen.Kot.createRoute(tableId)) },
                                onNavigateToBilling = { tableId -> navController.navigate(Screen.Billing.createRoute(tableId)) },
                                onNavigateToSettlement = { tableId, billId, total ->
                                    navController.navigate(Screen.Settlement.createRoute(tableId, billId, total))
                                },
                                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                            )
                        }

                        composable(
                            route = Screen.OpenTable.route,
                            arguments = listOf(navArgument("tableNumber") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tableNumber = backStackEntry.arguments?.getString("tableNumber") ?: ""
                            val tablesViewModel: TablesViewModel = viewModel(factory = factory)
                            OpenTableScreen(
                                tableNumber = tableNumber,
                                viewModel = tablesViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToMenu = { number ->
                                    navController.navigate(Screen.Menu.createRoute(number)) {
                                        popUpTo(Screen.OpenTable.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Screen.Menu.route,
                            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
                            val menuViewModel: MenuViewModel = viewModel(factory = factory)
                            MenuScreen(
                                tableId = tableId,
                                viewModel = menuViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCart = { id -> navController.navigate(Screen.Cart.createRoute(id)) }
                            )
                        }

                        composable(
                            route = Screen.Cart.route,
                            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
                            val cartViewModel: CartViewModel = viewModel(factory = factory)
                            CartScreen(
                                tableId = tableId,
                                viewModel = cartViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToKot = { id -> navController.navigate(Screen.Kot.createRoute(id)) },
                                onNavigateToBilling = { id -> navController.navigate(Screen.Billing.createRoute(id)) }
                            )
                        }

                        composable(
                            route = Screen.Kot.route,
                            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
                            val kotViewModel: KotViewModel = viewModel(factory = factory)
                            KotScreen(
                                tableId = tableId,
                                viewModel = kotViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.Billing.route,
                            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
                            val billingViewModel: BillingViewModel = viewModel(factory = factory)
                            BillingScreen(
                                tableId = tableId,
                                viewModel = billingViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSettlement = { tId, bId, total ->
                                    navController.navigate(Screen.Settlement.createRoute(tId, bId, total))
                                }
                            )
                        }

                        composable(
                            route = Screen.Settlement.route,
                            arguments = listOf(
                                navArgument("tableId") { type = NavType.StringType },
                                navArgument("billId") { type = NavType.StringType },
                                navArgument("grandTotal") { type = NavType.FloatType }
                            )
                        ) { backStackEntry ->
                            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
                            val billId = backStackEntry.arguments?.getString("billId") ?: ""
                            val grandTotal = backStackEntry.arguments?.getFloat("grandTotal")?.toDouble() ?: 0.0
                            val settlementViewModel: SettlementViewModel = viewModel(factory = factory)
                            SettlementScreen(
                                tableId = tableId,
                                billId = billId,
                                grandTotal = grandTotal,
                                viewModel = settlementViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToTables = {
                                    navController.navigate(Screen.Tables.route) {
                                        popUpTo(Screen.Tables.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.History.route) {
                            val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToTables = { navController.navigate(Screen.Tables.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                            )
                        }

                        composable(Screen.Profile.route) {
                            val profileViewModel: ProfileViewModel = viewModel(factory = factory)
                            ProfileScreen(
                                viewModel = profileViewModel,
                                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToTables = { navController.navigate(Screen.Tables.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToLogin = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
