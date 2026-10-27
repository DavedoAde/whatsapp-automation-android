package com.example.whatsappautomation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact

// --- BATTERY OPTIMIZATION SCREEN (Legacy, no longer used for blocking) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationScreen(onBack: () -> Unit, onRetry: () -> Unit) {
    // ... (Keep the existing code for this screen, it's not used anymore since we use the dialog) ...
}

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
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = contacts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

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
                    ContactCard(contact)
                }
            }
        }
    }
}

@Composable
fun ContactCard(contact: Contact) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                
                if (contact.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        contact.tags.split(",").forEach { tag ->
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
                                Text(
                                    text = tag.trim(), 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.tertiary, 
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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

// --- SETTINGS (Updated) ---
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    
    // Load saved preferences
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
                    PreferenceManager.setWaOriginalEnabled(context, it) // Save instantly
                }
                SwitchRow("WhatsApp Business", enableWABusiness) { 
                    enableWABusiness = it
                    PreferenceManager.setWaBusinessEnabled(context, it) // Save instantly
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
