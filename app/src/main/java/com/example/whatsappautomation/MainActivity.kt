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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hide top and bottom bars on secondary screens (Simulator, Rule Editor)
    val isMainScreen = currentRoute in listOf(Screen.Dashboard.route, Screen.Contacts.route, Screen.Rules.route)

    Scaffold(
        topBar = {
            if (isMainScreen) {
                TopAppBar(
                    title = { Text("WhatsApp Automation", color = MaterialTheme.colorScheme.primary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            if (isMainScreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
            composable(Screen.Contacts.route) { ContactsScreen(viewModel) }
            
            composable(Screen.Rules.route) { 
                RulesScreen(
                    viewModel = viewModel,
                    onEditRule = { ruleId -> navController.navigate("rule_editor/$ruleId/0") }
                ) 
            }
            
            composable("simulator") { 
                SimulatorScreen(viewModel, onBack = { navController.popBackStack() }) 
            }

            // Route handles both top-level rules (parentId = 0) and submenu rules (parentId != 0)
            composable(
                route = "rule_editor/{ruleId}/{parentId}",
                arguments = listOf(
                    navArgument("ruleId") { type = NavType.LongType },
                    navArgument("parentId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: 0L
                val parentId = backStackEntry.arguments?.getLong("parentId") ?: 0L
                
                val existingRule = if (ruleId == 0L) null else viewModel.allRules.value.find { it.id == ruleId }
                val childRules = viewModel.allRules.value.filter { it.parentRuleId == ruleId }
                
                RuleEditorScreen(
                    existingRule = existingRule,
                    parentId = if (parentId == 0L) null else parentId,
                    childRules = childRules,
                    onBack = { navController.popBackStack() },
                    onSave = { updatedRule ->
                        viewModel.saveRule(updatedRule)
                        navController.popBackStack()
                    },
                    onAddChildRule = { parentRuleId ->
                        navController.navigate("rule_editor/0/$parentRuleId")
                    },
                    onEditChildRule = { childId ->
                        navController.navigate("rule_editor/$childId/0") 
                    }
                )
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
