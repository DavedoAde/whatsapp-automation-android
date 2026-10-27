package com.example.whatsappautomation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    
    val startDestination = if (PreferenceManager.isFirstLaunch(context)) "welcome" else Screen.Dashboard.route

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
    
    // --- BATTERY REMINDER DIALOG STATE ---
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showBatteryDialog by remember { mutableStateOf(false) }
    
    // We use a flag to ensure it only checks when the app actually opens/resumes
    var isFirstResume by remember { mutableStateOf(true) }

    // Check battery status when app opens/resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Don't show the popup while on the welcome screen
                if (currentRoute != "welcome") {
                    val isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                    val isIgnored = PreferenceManager.isBatteryIgnored(context)
                    
                    if (isOptimized && !isIgnored) {
                        showBatteryDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            if (isMainScreen) {
                TopAppBar(
                    title = { Text("WhatsApp Automation", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        // Master ON/OFF Switch
                        Switch(
                            checked = isAutomationActive,
                            onCheckedChange = { isChecked ->
                                // FIXED: We just turn it on/off. No battery popup here.
                                viewModel.toggleAutomation(isChecked)
                            },
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onGetStartedClick = {
                        PreferenceManager.setFirstLaunchCompleted(context)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            
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

    // --- THE BATTERY REMINDER DIALOG ---
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { 
                // If they tap outside the dialog, treat it as Ignore
                PreferenceManager.setBatteryIgnored(context, true)
                showBatteryDialog = false 
            },
            title = { Text("Battery Optimization is ON") },
            text = { 
                Text("Your phone is restricting background apps. To ensure auto-replies work reliably, please set battery usage to 'Unrestricted' in settings.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            fallbackIntent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(fallbackIntent)
                        }
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Save preference so it never shows again
                        PreferenceManager.setBatteryIgnored(context, true)
                        showBatteryDialog = false
                    }
                ) {
                    Text("Ignore", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun WhatsAppAutomationTheme(content: @Composable () -> Unit) {
    val deepObsidian = Color(0xFF0B141A)  
    val lighterCard = Color(0xFF1F2C34)   
    val accentGreen = Color(0xFF25D366)   
    val textLight = Color(0xFFE9EDEF)     
    val textMuted = Color(0xFF8696A0)     

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentGreen,
            background = deepObsidian,
            surface = lighterCard,
            onBackground = textLight,
            onSurface = textLight,
            tertiary = Color(0xFFBB86FC), 
            onTertiary = Color.Black
        ),
        content = content
    )
}
