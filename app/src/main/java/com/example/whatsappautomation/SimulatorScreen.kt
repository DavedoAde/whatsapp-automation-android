package com.example.whatsappautomation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsappautomation.data.AutomationRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var inputMessage by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var matchedRule by remember { mutableStateOf<AutomationRule?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Simulator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Type a fake incoming message below to see which rule triggers.", fontSize = 14.sp)
            
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { 
                    inputMessage = it 
                    resultText = null // Clear previous result on typing
                    matchedRule = null
                },
                label = { Text("Incoming Message (e.g., Hello there)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        val rule = viewModel.simulateMessage(inputMessage)
                        if (rule != null) {
                            matchedRule = rule
                            resultText = "Rule matched: ${rule.name}\n\nReply sent: '${rule.replyMessage}'"
                        } else {
                            matchedRule = null
                            resultText = "No rules matched this message."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputMessage.isNotBlank()
            ) {
                Text("Simulate Receive")
            }

            resultText?.let { text ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (matchedRule != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (matchedRule != null) "Match Found!" else "No Match",
                            fontWeight = FontWeight.Bold,
                            color = if (matchedRule != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text, color = if (matchedRule != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
