package com.example.whatsappautomation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.whatsappautomation.data.ActivityLog
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact
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
    
    // Snackbar for export success
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // CSV Export Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val csvData = CSVExporter.generateCSV(contacts)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Contacts exported successfully!")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error exporting: ${e.message}")
                }
            }
        }
    }

    val filteredContacts = contacts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Export Button
                IconButton(
                    onClick = { 
                        if (contacts.isNotEmpty()) {
                            createDocumentLauncher.launch("WhatsApp_Leads_${System.currentTimeMillis()}.csv") 
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("No contacts to export.") }
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).size(56.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
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
    }

    // Pause Duration Menu
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

    // Ignore Confirmation
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
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Activity Logs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = { viewModel.clearLogs() }) {
                Text("Clear Logs", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

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
    var enableOriginalWA by remember { mutableStateOf(PreferenceManager.isWaOriginalEnabled(context)) }
    var enableWABusiness by remember { mutableStateOf(PreferenceManager.isWaBusinessEnabled(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Version of WA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                SwitchRow("WhatsApp (Original)", enableOriginalWA) { 
                    enableOriginalWA = it
                    PreferenceManager.setWaOriginalEnabled(context, it)
                }
                SwitchRow("WhatsApp Business", enableWABusiness) { 
                    enableWABusiness = it
                    PreferenceManager.setWaBusinessEnabled(context, it)
                }
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
