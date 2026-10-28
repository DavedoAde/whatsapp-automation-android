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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

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
    
    // --- DRAWER STATE ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val startDestination = if (PreferenceManager.isFirstLaunch(context)) "welcome" else Screen.Dashboard.route

    // Bottom Bar Screens (Reduced to 3)
    val bottomScreens = listOf(
        Screen.Dashboard, 
        Screen.Rules,
        Screen.Contacts
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val isMainScreen = currentRoute in listOf(
        Screen.Dashboard.route, 
        Screen.Contacts.route, 
        Screen.Rules.route,
        Screen.Logs.route,
        Screen.Settings.route
    )

    val isAutomationActive by viewModel.isAutomationActive.collectAsState()
    
    // --- PERMISSIONS & BATTERY STATE ---
    var hasNotifAccess by remember { mutableStateOf(checkNotificationAccess(context)) }
    var hasContactAccess by remember { mutableStateOf(checkContactAccess(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isTurningOnAutomation by remember { mutableStateOf(false) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactAccess = isGranted
        if (isTurningOnAutomation && hasNotifAccess && hasContactAccess) {
            viewModel.toggleAutomation(true)
            showPermissionDialog = false
            isTurningOnAutomation = false
        }
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showBatteryDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifAccess = checkNotificationAccess(context)
                hasContactAccess = checkContactAccess(context)

                if (isTurningOnAutomation && hasNotifAccess && hasContactAccess) {
                    viewModel.toggleAutomation(true)
                    showPermissionDialog = false
                    isTurningOnAutomation = false
                    
                    val isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                    if (isOptimized && !PreferenceManager.isBatteryIgnored(context)) {
                        showBatteryDialog = true
                    }
                } 
                else if (!showPermissionDialog && currentRoute != "welcome") {
                    val isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                    if (isOptimized && !PreferenceManager.isBatteryIgnored(context) && !PreferenceManager.isFirstLaunch(context)) {
                        showBatteryDialog = true
                    } else {
                        showBatteryDialog = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // --- MAIN SCAFFOLD WRAPPED IN DRAWER ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = MaterialTheme.shapes.large
            ) {
                // Drawer Header
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "WhatsApp Automation",
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Version 1.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                // Drawer Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Logs (Reply History)") },
                    selected = currentRoute == Screen.Logs.route,
                    onClick = { 
                        navController.navigate(Screen.Logs.route) 
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentRoute == Screen.Settings.route,
                    onClick = { 
                        navController.navigate(Screen.Settings.route)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isMainScreen) {
                    TopAppBar(
                        title = { Text("WhatsApp Automation", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            // Hamburger Icon
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("simulator") }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat, 
                                    contentDescription = "Simulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Switch(
                                checked = isAutomationActive,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (hasNotifAccess && hasContactAccess) {
                                            viewModel.toggleAutomation(true)
                                            val isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                                            if (isOptimized && !PreferenceManager.isBatteryIgnored(context)) {
                                                showBatteryDialog = true
                                            }
                                        } else {
                                            isTurningOnAutomation = true
                                            showPermissionDialog = true
                                        }
                                    } else {
                                        viewModel.toggleAutomation(false)
                                        isTurningOnAutomation = false
                                    }
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
                        bottomScreens.forEach { screen ->
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
                
                composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
                composable(Screen.Rules.route) { RulesScreen(viewModel, onEditRule = { ruleId -> navController.navigate("rule_editor/$ruleId/0") }) }
                composable(Screen.Contacts.route) { ContactsScreen(viewModel) }
                composable(Screen.Logs.route) { LogsScreen(viewModel) }
                composable(Screen.Settings.route) { SettingsScreen() }
                
                composable("simulator") { SimulatorScreen(viewModel, onBack = { navController.popBackStack() }) }

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

    // --- DIALOGS ---
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false; isTurningOnAutomation = false },
            title = { Text("Permissions Required") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("To turn on the AutoResponder, you must grant the following permissions:")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(imageVector = if (hasNotifAccess) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (hasNotifAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Access", modifier = Modifier.weight(1f))
                        if (!hasNotifAccess) {
                            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Grant") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(imageVector = if (hasContactAccess) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (hasContactAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact Access", modifier = Modifier.weight(1f))
                        if (!hasContactAccess) {
                            TextButton(onClick = { contactPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS) }) { Text("Grant") }
                        }
                    }
                }
            },
            confirmButton = {
                if (hasNotifAccess && hasContactAccess) {
                    Button(onClick = {
                        viewModel.toggleAutomation(true)
                        showPermissionDialog = false
                        isTurningOnAutomation = false
                        val isOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                        if (isOptimized && !PreferenceManager.isBatteryIgnored(context)) { showBatteryDialog = true }
                    }) { Text("Done") }
                } else {
                    TextButton(onClick = { showPermissionDialog = false; isTurningOnAutomation = false }) { Text("Close") }
                }
            }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Battery Optimization is ON") },
            text = { Text("Your phone is restricting background apps. To ensure auto-replies work reliably, please set battery usage to 'Unrestricted' in settings. This is optional, but highly recommended.") },
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
                ) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { PreferenceManager.setBatteryIgnored(context, true); showBatteryDialog = false }) { Text("Ignore", color = Color.Gray) }
            }
        )
    }
}

fun checkNotificationAccess(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

fun checkContactAccess(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
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
