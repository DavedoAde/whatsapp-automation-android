package com.example.whatsappautomation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    // FIX: Using .value to get the raw Boolean out of the StateFlow
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

        Spacer(modifier = Modifier.weight(1f))

        // Temporary button to prove the database works
        Button(
            onClick = { viewModel.addMockData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Database (Add Mock Data)")
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
fun RulesScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Automation Rules will go here", fontSize = 18.sp)
    }
}
