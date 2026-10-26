package com.example.whatsappautomation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun RuleEditorScreen(
    existingRule: AutomationRule?,
    onBack: () -> Unit,
    onSave: (AutomationRule) -> Unit
) {
    // State variables for all the rule settings
    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var trigger by remember { mutableStateOf(existingRule?.triggerKeyword ?: "") }
    var reply by remember { mutableStateOf(existingRule?.replyMessage ?: "") }
    var matchType by remember { mutableStateOf(existingRule?.matchType ?: "SIMILARITY") }
    var actionType by remember { mutableStateOf(existingRule?.actionType ?: "AUTO_REPLY") }
    var delay by remember { mutableStateOf(existingRule?.delaySeconds?.toString() ?: "0") }
    var recipient by remember { mutableStateOf(existingRule?.recipientType ?: "BOTH") }
    var screenOff by remember { mutableStateOf(existingRule?.requireScreenOff ?: false) }
    var charging by remember { mutableStateOf(existingRule?.requireCharging ?: false) }
    var dnd by remember { mutableStateOf(existingRule?.requireDnd ?: false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingRule == null) "Create Rule" else "Edit Rule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val newRule = existingRule?.copy(
                                name = name,
                                triggerKeyword = trigger,
                                replyMessage = reply,
                                matchType = matchType,
                                actionType = actionType,
                                delaySeconds = delay.toIntOrNull() ?: 0,
                                recipientType = recipient,
                                requireScreenOff = screenOff,
                                requireCharging = charging,
                                requireDnd = dnd
                            ) ?: AutomationRule(
                                name = name,
                                triggerKeyword = trigger,
                                replyMessage = reply,
                                matchType = matchType,
                                actionType = actionType,
                                delaySeconds = delay.toIntOrNull() ?: 0,
                                recipientType = recipient,
                                requireScreenOff = screenOff,
                                requireCharging = charging,
                                requireDnd = dnd
                            )
                            onSave(newRule)
                        },
                        enabled = name.isNotBlank() && trigger.isNotBlank() && reply.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: TRIGGER
            EditorSection(title = "Received Message") {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger Keyword or Phrase") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Match Type", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "EXACT" }) {
                    RadioButton(selected = matchType == "EXACT", onClick = { matchType = "EXACT" })
                    Text("Exact Match")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "SIMILARITY" }) {
                    RadioButton(selected = matchType == "SIMILARITY", onClick = { matchType = "SIMILARITY" })
                    Text("Similarity / Contains")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "REGEX" }) {
                    RadioButton(selected = matchType == "REGEX", onClick = { matchType = "REGEX" })
                    Text("RegEx Pattern")
                }
            }

            // SECTION 2: ACTION & REPLY
            EditorSection(title = "Automatic Action") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { actionType = "AUTO_REPLY" }) {
                    RadioButton(selected = actionType == "AUTO_REPLY", onClick = { actionType = "AUTO_REPLY" })
                    Text("Standard Auto Reply")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { actionType = "CAPTURE_CONTACT" }) {
                    RadioButton(selected = actionType == "CAPTURE_CONTACT", onClick = { actionType = "CAPTURE_CONTACT" })
                    Text("Capture Contact Flow", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text(if (actionType == "CAPTURE_CONTACT") "Welcome Message (e.g. What is your name?)" else "Reply Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = delay,
                    onValueChange = { delay = it },
                    label = { Text("Delay in seconds (0 for instant)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // SECTION 3: RECIPIENTS
            EditorSection(title = "Recipients") {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { recipient = "INDIVIDUALS" }) {
                        RadioButton(selected = recipient == "INDIVIDUALS", onClick = { recipient = "INDIVIDUALS" })
                        Text("Individuals")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { recipient = "GROUPS" }) {
                        RadioButton(selected = recipient == "GROUPS", onClick = { recipient = "GROUPS" })
                        Text("Groups")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { recipient = "BOTH" }) {
                        RadioButton(selected = recipient == "BOTH", onClick = { recipient = "BOTH" })
                        Text("Both")
                    }
                }
            }

            // SECTION 4: CONDITIONS
            EditorSection(title = "Conditions (Optional)") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = screenOff, onCheckedChange = { screenOff = it })
                    Text("Only reply if screen is off/locked")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = charging, onCheckedChange = { charging = it })
                    Text("Only reply if device is charging")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = dnd, onCheckedChange = { dnd = it })
                    Text("Only reply if Do Not Disturb is active")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
