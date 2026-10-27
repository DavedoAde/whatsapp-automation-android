package com.example.whatsappautomation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
        enableEdgeToEdge() 
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
    val screens = listOf(
        Screen.Dashboard, 
        Screen.Rules,
        Screen.Contacts, 
        Screen.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val isMainScreen = currentRoute in listOf(
        Screen.Dashboard.route, 
        Screen.Contacts.route, 
        Screen.Rules.route,
        Screen.Settings.route
    )

    val isAutomationActive by viewModel.isAutomationActive.collectAsState()

    Scaffold(
        topBar = {
            if (isMainScreen) {
                TopAppBar(
                    title = { Text("WhatsApp Automation", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        // Master ON/OFF Switch
                        Switch(
                            checked = isAutomationActive,
                            onCheckedChange = { viewModel.toggleAutomation(it) },
                            modifier = Modifier.padding(end = 16.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
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
            composable(Screen.Rules.route) { 
                RulesScreen(
                    viewModel = viewModel,
                    onEditRule = { ruleId -> navController.navigate("rule_editor/$ruleId/0") }
                ) 
            }
            composable(Screen.Contacts.route) { ContactsScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen() }
            
            composable("simulator") { 
                SimulatorScreen(viewModel, onBack = { navController.popBackStack() }) 
            }

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

// --- NEW PREMIUM THEME COLORS ---
@Composable
fun WhatsAppAutomationTheme(content: @Composable () -> Unit) {
    val deepObsidian = Color(0xFF0B141A)  // Very dark background
    val lighterCard = Color(0xFF1F2C34)   // Card surface color
    val accentGreen = Color(0xFF25D366)   // Vibrant green
    val textLight = Color(0xFFE9EDEF)     // White-ish text
    val textMuted = Color(0xFF8696A0)     // Grey text

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentGreen,
            background = deepObsidian,
            surface = lighterCard,
            onBackground = textLight,
            onSurface = textLight,
            tertiary = Color(0xFFBB86FC), // Soft purple for Tags
            onTertiary = Color.Black
        ),
        content = content
    )
}
