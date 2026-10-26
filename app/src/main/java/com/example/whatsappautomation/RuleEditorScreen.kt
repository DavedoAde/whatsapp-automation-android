package com.example.whatsappautomation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    parentId: Long?,
    childRules: List<AutomationRule>,
    onBack: () -> Unit,
    onSave: (AutomationRule) -> Unit,
    onAddChildRule: (Long) -> Unit,
    onEditChildRule: (Long) -> Unit
) {
    var name by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.name ?: "") }
    var trigger by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.triggerKeyword ?: "") }
    var matchType by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.matchType ?: "SIMILARITY") }
    var actionType by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.actionType ?: "AUTO_REPLY") }
    var replyMode by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.replyMode ?: "SINGLE") }
    var delay by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.delaySeconds?.toString() ?: "0") }
    var recipient by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.recipientType ?: "BOTH") }
    var screenOff by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireScreenOff ?: false) }
    var charging by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireCharging ?: false) }
    var dnd by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireDnd ?: false) }

    val replies = remember { mutableStateListOf<String>() }
    LaunchedEffect(existingRule) {
        if (existingRule != null) {
            replies.addAll(existingRule.replyMessage.split("|").filter { it.isNotBlank() })
        } else {
            replies.add("")
        }
    }

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
                            val combinedReplies = replies.filter { it.isNotBlank() }.joinToString("|")
                            
                            val newRule = existingRule?.copy(
                                name = name,
                                triggerKeyword = trigger,
                                replyMessage = combinedReplies,
                                matchType = matchType,
                                actionType = actionType,
                                replyMode = replyMode,
                                delaySeconds = delay.toIntOrNull() ?: 0,
                                recipientType = recipient,
                                requireScreenOff = screenOff,
                                requireCharging = charging,
                                requireDnd = dnd
                            ) ?: AutomationRule(
                                name = name,
                                triggerKeyword = trigger,
                                replyMessage = combinedReplies,
                                matchType = matchType,
                                actionType = actionType,
                                replyMode = replyMode,
                                delaySeconds = delay.toIntOrNull() ?: 0,
                                recipientType = recipient,
                                requireScreenOff = screenOff,
                                requireCharging = charging,
                                requireDnd = dnd,
                                parentRuleId = parentId
                            )
                            onSave(newRule)
                        },
                        enabled = name.isNotBlank() && trigger.isNotBlank() && replies.any { it.isNotBlank() },
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
                    Column {
                        Text("Exact Match")
                        Text("Ignores case and punctuation", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "SIMILARITY" }) {
                    RadioButton(selected = matchType == "SIMILARITY", onClick = { matchType = "SIMILARITY" })
                    Column {
                        Text("Similarity Match")
                        Text("Triggers if spelling is close (e.g., 'helo')", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "PATTERN" }) {
                    RadioButton(selected = matchType == "PATTERN", onClick = { matchType = "PATTERN" })
                    Column {
                        Text("Pattern Match")
                        Text("Use * for anything, // for OR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { matchType = "REGEX" }) {
                    RadioButton(selected = matchType == "REGEX", onClick = { matchType = "REGEX" })
                    Column {
                        Text("RegEx Pattern")
                        Text("Advanced: use standard Java Regex", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            // SECTION 2: ACTION & MULTIPLE REPLIES
            EditorSection(title = "Automatic Action") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { actionType = "AUTO_REPLY" }) {
                    RadioButton(selected = actionType == "AUTO_REPLY", onClick = { actionType = "AUTO_REPLY" })
                    Text("Standard Auto Reply")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { actionType = "CAPTURE_CONTACT" }) {
                    RadioButton(selected = actionType == "CAPTURE_CONTACT", onClick = { actionType = "CAPTURE_CONTACT" })
                    Column {
                        Text("Capture Contact Flow", color = MaterialTheme.colorScheme.primary)
                        Text("Saves the user's NEXT message as their name.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Reply Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode = "SINGLE" }) {
                        RadioButton(selected = replyMode == "SINGLE", onClick = { replyMode = "SINGLE" })
                        Text("Single")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode = "ALL" }) {
                        RadioButton(selected = replyMode == "ALL", onClick = { replyMode = "ALL" })
                        Text("All")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode == "RANDOM" }) {
                        RadioButton(selected = replyMode == "RANDOM", onClick = { replyMode = "RANDOM" })
                        Text("Random")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (actionType == "CAPTURE_CONTACT") "Message to prompt name" else "Reply Messages", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                replies.forEachIndexed { index, reply ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = reply,
                            onValueChange = { replies[index] = it },
                            label = { Text(if (actionType == "CAPTURE_CONTACT") "Ask for name (e.g., What is your name?)" else "Reply ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            minLines = 2
                        )
                        if (replies.size > 1) {
                            IconButton(onClick = { replies.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Reply", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Button(
                    onClick = { replies.add("") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reply")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Another Reply")
                }
            }

            // SECTION 3: SUBMENU
            if (existingRule != null) {
                EditorSection(title = "Submenu / Conversation Flow") {
                    Text("Add a new rule inside this menu. You will be taken to a new screen to configure it.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (childRules.isEmpty()) {
                        Text("No submenu rules yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        childRules.forEach { child ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(child.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Trigger: ${child.triggerKeyword}", fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { onEditChildRule(child.id) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAddChildRule(existingRule.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Submenu Rule")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Submenu Rule")
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
