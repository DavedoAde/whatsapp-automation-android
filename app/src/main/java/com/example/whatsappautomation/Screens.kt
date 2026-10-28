package com.example.whatsappautomation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whatsappautomation.data.ActivityLog
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- DASHBOARD ---
@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val activeRulesCount by viewModel.activeRulesCount.collectAsState()
    val totalContactsCount by viewModel.totalContactsCount.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Active Rules", value = "$activeRulesCount", icon = Icons.Default.Rule, modifier = Modifier.weight(1f))
            StatCard(title = "Contacts", value = "$totalContactsCount", icon = Icons.Default.People, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

// --- CONTACTS ---
@Composable
fun ContactsScreen(viewModel: AppViewModel) {
    val contacts by viewModel.allContacts.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    var showPauseMenuForContact by remember { mutableStateOf<Contact?>(null) }
    var showIgnoreConfirmForContact by remember { mutableStateOf<Contact?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val csvData = CSVExporter.generateCSV(contacts)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                }
                scope.launch { snackbarHostState.showSnackbar("Contacts exported successfully!") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Error exporting: ${e.message}") }
            }
        }
    }

    val triggerExport by viewModel.triggerCsvExport.collectAsState()
    LaunchedEffect(triggerExport) {
        if (triggerExport) {
            if (contacts.isNotEmpty()) {
                createDocumentLauncher.launch("WAcopilot_Leads_${System.currentTimeMillis()}.csv")
            } else {
                scope.launch { snackbarHostState.showSnackbar("No contacts to export.") }
            }
            viewModel.resetCsvExportTrigger()
        }
    }

    val filteredContacts = contacts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search contacts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No contacts found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredContacts) { contact ->
                        ContactCard(
                            contact = contact, 
                            onPauseClick = { showPauseMenuForContact = contact },
                            onIgnoreClick = { showIgnoreConfirmForContact = contact }
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showPauseMenuForContact != null) {
        AlertDialog(
            onDismissRequest = { showPauseMenuForContact = null },
            title = { Text("Pause Bot for ${showPauseMenuForContact?.name}") },
            text = { Text("Choose how long the bot should stay silent for this contact.") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.pauseContact(showPauseMenuForContact!!.name, 30); showPauseMenuForContact = null }, modifier = Modifier.fillMaxWidth()) { Text("30 Minutes") }
                    Button(onClick = { viewModel.pauseContact(showPauseMenuForContact!!.name, 60); showPauseMenuForContact = null }, modifier = Modifier.fillMaxWidth()) { Text("1 Hour") }
                    Button(onClick = { viewModel.pauseContact(showPauseMenuForContact!!.name, 120); showPauseMenuForContact = null }, modifier = Modifier.fillMaxWidth()) { Text("2 Hours") }
                }
            },
            dismissButton = { TextButton(onClick = { showPauseMenuForContact = null }) { Text("Cancel") } }
        )
    }

    if (showIgnoreConfirmForContact != null) {
        val isCurrentlyIgnored = showIgnoreConfirmForContact!!.isIgnored
        AlertDialog(
            onDismissRequest = { showIgnoreConfirmForContact = null },
            title = { Text(if (isCurrentlyIgnored) "Unignore Contact?" else "Ignore Contact?") },
            text = { Text(if (isCurrentlyIgnored) "The bot will start replying to ${showIgnoreConfirmForContact?.name} again." else "The bot will NEVER reply to ${showIgnoreConfirmForContact?.name} again.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleIgnoreContact(showIgnoreConfirmForContact!!.name, !isCurrentlyIgnored); showIgnoreConfirmForContact = null },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCurrentlyIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                ) { Text(if (isCurrentlyIgnored) "Unignore" else "Ignore") }
            },
            dismissButton = { TextButton(onClick = { showIgnoreConfirmForContact = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ContactCard(contact: Contact, onPauseClick: () -> Unit, onIgnoreClick: () -> Unit) {
    val isPaused = contact.pausedUntil > System.currentTimeMillis()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (contact.isIgnored) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (contact.name.isNotBlank()) contact.name.first().uppercase() else "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(contact.phoneNumber, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (contact.tags.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
                                Text(contact.tags, fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (contact.isIgnored) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) {
                                Text("Ignored", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else if (isPaused) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF9800).copy(alpha = 0.2f)) {
                                Text("Paused", fontSize = 10.sp, color = Color(0xFFFF9800), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPauseClick) {
                    Icon(Icons.Default.PauseCircle, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause", fontSize = 12.sp)
                }
                TextButton(onClick = onIgnoreClick) {
                    Icon(Icons.Default.Block, contentDescription = "Ignore", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (contact.isIgnored) "Unignore" else "Ignore", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// --- RULES ---
@Composable
fun RulesScreen(viewModel: AppViewModel, onEditRule: (Long) -> Unit) {
    val allRules by viewModel.allRules.collectAsState()
    val topLevelRules = allRules.filter { it.parentRuleId == null }
    var ruleToDelete by remember { mutableStateOf<AutomationRule?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRules = topLevelRules.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.triggerKeyword.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search rules...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredRules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rules found.\nTap + to create one.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredRules) { rule ->
                        RuleCard(
                            rule = rule, 
                            onToggleClick = { viewModel.toggleRuleActiveState(rule) },
                            onEditClick = { onEditRule(rule.id) }, 
                            onDeleteClick = { ruleToDelete = rule }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onEditRule(0L) }, 
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 8.dp), 
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule")
        }
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Rule?") },
            text = { Text("Are you sure you want to delete '${ruleToDelete?.name}'?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRule(ruleToDelete!!); ruleToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { ruleToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RuleCard(rule: AutomationRule, onToggleClick: () -> Unit, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = rule.isActive, 
                    onCheckedChange = { onToggleClick() },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }
            
            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rule.askForName) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
                        Text("Lead Capture", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (rule.assignTag.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                        Text("Tag: ${rule.assignTag}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Text("Trigger: '${rule.triggerKeyword}' (${rule.matchType})", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

// --- LOGS ---
@Composable
fun LogsScreen(viewModel: AppViewModel) {
    val logs by viewModel.allLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No activity yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@Composable
fun LogCard(log: ActivityLog) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(log.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (log.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(log.actionTaken, fontWeight = FontWeight.Bold, color = if (log.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("From: ${log.senderName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Msg: \"${log.incomingMessage}\"", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            
            if (log.replyText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("Reply: ${log.replyText}", fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

// --- SETTINGS ---
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    
    var showWaDialog by remember { mutableStateOf(false) }
    var currentWaVersion by remember { mutableStateOf(PreferenceManager.getWaVersion(context)) }
    var isMultiUser by remember { mutableStateOf(PreferenceManager.isMultiUserEnabled(context)) }
    var isNotificationsEnabled by remember { mutableStateOf(PreferenceManager.isNotificationsEnabled(context)) }
    
    var showSaveAccountDialog by remember { mutableStateOf(false) }
    var currentSaveAccount by remember { mutableStateOf(PreferenceManager.getSaveAccount(context)) }
    var currentGoogleAccount by remember { mutableStateOf(PreferenceManager.getGoogleAccountName(context)) }
    
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

    // FIXED: Ask for READ_CONTACTS and WRITE_CONTACTS instead of GET_ACCOUNTS
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] == true
        
        if (readGranted && writeGranted) {
            showSaveAccountDialog = true
        } else {
            // Optional: Show a message if they deny
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Version of WA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showWaDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Choose WA version", fontSize = 16.sp)
                    Text(
                        text = if (currentWaVersion == "ANY") "Any" else if (currentWaVersion == "ORIGINAL") "WA (Original)" else "WA Business",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Multi-User support", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Reply to WA versions of other users on this device (Dual App).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Switch(
                        checked = isMultiUser, 
                        onCheckedChange = { 
                            isMultiUser = it
                            PreferenceManager.setMultiUserEnabled(context, it)
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Get notified when the app has replied to a chat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Switch(
                        checked = isNotificationsEnabled, 
                        onCheckedChange = { 
                            isNotificationsEnabled = it
                            PreferenceManager.setNotificationsEnabled(context, it)
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Battery Optimization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isBatteryOptimized) "Your phone is restricting background apps." else "Unrestricted. The app will work reliably.", 
                    fontSize = 12.sp, 
                    color = if (isBatteryOptimized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isBatteryOptimized) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                fallbackIntent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(fallbackIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Disable Optimization")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Save Contacts To", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Where should WAcopilot save new leads?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        // FIXED: Check for READ_CONTACTS and WRITE_CONTACTS, not GET_ACCOUNTS
                        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        
                        if (hasRead && hasWrite) {
                            showSaveAccountDialog = true
                        } else {
                            permLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                        }
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (currentSaveAccount == "GOOGLE") "Google Account" else "Phone Memory", fontSize = 16.sp)
                        if (currentSaveAccount == "GOOGLE" && currentGoogleAccount.isNotBlank()) {
                            Text(currentGoogleAccount, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showWaDialog) {
        AlertDialog(
            onDismissRequest = { showWaDialog = false },
            title = { Text("Choose WA version") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaVersionOption("Any", "ANY", currentWaVersion) { 
                        currentWaVersion = it; PreferenceManager.setWaVersion(context, it); showWaDialog = false 
                    }
                    WaVersionOption("WA (Original)", "ORIGINAL", currentWaVersion) { 
                        currentWaVersion = it; PreferenceManager.setWaVersion(context, it); showWaDialog = false 
                    }
                    WaVersionOption("WA Business", "BUSINESS", currentWaVersion) { 
                        currentWaVersion = it; PreferenceManager.setWaVersion(context, it); showWaDialog = false 
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWaDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // --- SAVE ACCOUNT DIALOG ---
    if (showSaveAccountDialog) {
        val accounts = remember { PhoneContactManager.getAccounts(context) }

        AlertDialog(
            onDismissRequest = { showSaveAccountDialog = false },
            title = { Text("Save Contacts To") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { 
                                if (account.name == null) {
                                    currentSaveAccount = "LOCAL"
                                    currentGoogleAccount = ""
                                    PreferenceManager.setSaveAccount(context, "LOCAL")
                                    PreferenceManager.setGoogleAccountName(context, "")
                                } else {
                                    currentSaveAccount = "GOOGLE"
                                    currentGoogleAccount = account.name
                                    PreferenceManager.setSaveAccount(context, "GOOGLE")
                                    PreferenceManager.setGoogleAccountName(context, account.name)
                                }
                                showSaveAccountDialog = false 
                            }.padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (account.name == null && currentSaveAccount == "LOCAL") || (account.name != null && currentGoogleAccount == account.name),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (account.name == null) "Phone Memory" else account.name, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSaveAccountDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun WaVersionOption(title: String, value: String, currentValue: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = currentValue == value,
            onClick = { onSelect(value) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 16.sp)
    }
}
