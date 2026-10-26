package com.example.whatsappautomation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact

@Composable
fun DashboardScreen(viewModel: AppViewModel, onNavigateToSimulator: () -> Unit) {
    val isAutomationActive by viewModel.isAutomationActive.collectAsState()
    val activeRulesCount by viewModel.activeRulesCount.collectAsState()
    val totalContactsCount by viewModel.totalContactsCount.collectAsState()
    
    val context = LocalContext.current
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAutomationActive) "Automation is ON" else "Automation is OFF", 
                        fontWeight = FontWeight.Bold, 
                        color = if (isAutomationActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text("Monitoring WhatsApp", fontSize = 12.sp)
                }
                Switch(
                    checked = isAutomationActive, 
                    onCheckedChange = { viewModel.toggleAutomation(it) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active Rules", fontSize = 12.sp)
                    Text("$activeRulesCount", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contacts", fontSize = 12.sp)
                    Text("$totalContactsCount", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Permission Card
        if (!hasContactPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Grant contact access to automatically save new leads to your phone's address book.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToSimulator, modifier = Modifier.fillMaxWidth()) {
            Text("Open Chat Simulator")
        }
    }
}

// ... The rest of the file (ContactsScreen, RulesScreen, RuleDialog, etc.) remains exactly the same ...
// (I am including them below so you can safely copy/paste the whole file)

@Composable
fun ContactsScreen(viewModel: AppViewModel) {
    val contacts by viewModel.allContacts.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            Text(
                "No contacts captured yet.\nUse the Simulator to test the flow.", 
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts) { contact ->
                    ContactCard(contact)
                }
            }
        }
    }
}

@Composable
fun ContactCard(contact: Contact) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Phone: ${contact.phoneNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Source: ${contact.sourceApp}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RulesScreen(viewModel: AppViewModel) {
    val rules by viewModel.allRules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<AutomationRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<AutomationRule?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            Text(
                "No rules created yet.\nTap + to add one.", 
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules) { rule ->
                    RuleCard(
                        rule = rule,
                        onEditClick = { 
                            ruleToEdit = rule
                            showDialog = true 
                        },
                        onDeleteClick = { ruleToDelete = rule }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { 
                ruleToEdit = null
                showDialog = true 
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule")
        }
    }

    if (showDialog) {
        RuleDialog(
            existingRule = ruleToEdit,
            onDismiss = { showDialog = false },
            onSave = { name, keyword, reply, actionType ->
                if (ruleToEdit == null) {
                    viewModel.addRule(name, keyword, reply, actionType)
                } else {
                    viewModel.updateRule(ruleToEdit!!.copy(name = name, triggerKeyword = keyword, replyMessage = reply, actionType = actionType))
                }
                showDialog = false
            }
        )
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Rule?") },
            text = { Text("Are you sure you want to delete '${ruleToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRule(ruleToDelete!!)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RuleCard(rule: AutomationRule, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text("Type: ${if (rule.actionType == "CAPTURE_CONTACT") "Capture Contact" else "Auto Reply"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Trigger: '${rule.triggerKeyword}'", fontSize = 14.sp)
                Text("Reply: '${rule.replyMessage}'", fontSize = 14.sp, maxLines = 1)
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RuleDialog(existingRule: AutomationRule?, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var keyword by remember { mutableStateOf(existingRule?.triggerKeyword ?: "") }
    var reply by remember { mutableStateOf(existingRule?.replyMessage ?: "") }
    var actionType by remember { mutableStateOf(existingRule?.actionType ?: "AUTO_REPLY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRule == null) "Create New Rule" else "Edit Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Trigger Keyword") },
                    singleLine = true
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = actionType == "AUTO_REPLY",
                        onClick = { actionType = "AUTO_REPLY" }
                    )
                    Text("Auto Reply")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = actionType == "CAPTURE_CONTACT",
                        onClick = { actionType = "CAPTURE_CONTACT" }
                    )
                    Text("Capture Contact")
                }

                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text(if (actionType == "CAPTURE_CONTACT") "Welcome Message (e.g., What is your name?)" else "Reply Message") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && keyword.isNotBlank() && reply.isNotBlank()) onSave(name, keyword, reply, actionType) },
                enabled = name.isNotBlank() && keyword.isNotBlank() && reply.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
