package com.example.whatsappautomation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsappautomation.data.AutomationRule

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val isAutomationActive by viewModel.isAutomationActive.collectAsState()
    val activeRulesCount by viewModel.activeRulesCount.collectAsState()
    val totalContactsCount by viewModel.totalContactsCount.collectAsState()

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
    }
}

@Composable
fun ContactsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Contacts list will go here", fontSize = 18.sp)
    }
}

@Composable
fun RulesScreen(viewModel: AppViewModel) {
    val rules by viewModel.allRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                    RuleCard(rule)
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule")
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, keyword, reply ->
                viewModel.addRule(name, keyword, reply)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleCard(rule: AutomationRule) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("If message contains: '${rule.triggerKeyword}'", fontSize = 14.sp)
            Text("Reply: '${rule.replyMessage}'", fontSize = 14.sp)
        }
    }
}

@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name (e.g., Welcome)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Trigger Keyword (e.g., Hi)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Reply Message") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && keyword.isNotBlank() && reply.isNotBlank()) onSave(name, keyword, reply) },
                enabled = name.isNotBlank() && keyword.isNotBlank() && reply.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
