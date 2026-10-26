package com.example.whatsappautomation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppAutomationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Dashboard, Screen.Contacts, Screen.Rules)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Automation", color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { 
                DashboardScreen(viewModel, onNavigateToSimulator = { navController.navigate("simulator") }) 
            }
            composable(Screen.Contacts.route) { ContactsScreen() }
            composable(Screen.Rules.route) { RulesScreen(viewModel) }
            composable("simulator") { 
                SimulatorScreen(viewModel, onBack = { navController.popBackStack() }) 
            }
        }
    }
}

@Composable
fun WhatsAppAutomationTheme(content: @Composable () -> Unit) {
    val darkNavy = Color(0xFF0A1929)
    val charcoal = Color(0xFF1E2A3A)
    val accentGreen = Color(0xFF4CAF50)
    val textLight = Color(0xFFE0E0E0)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentGreen,
            background = darkNavy,
            surface = charcoal,
            onBackground = textLight,
            onSurface = textLight
        ),
        content = content
    )
}
