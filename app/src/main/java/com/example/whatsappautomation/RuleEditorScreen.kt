package com.example.whatsappautomation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var trigger by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.triggerKeyword ?: "") }
    var matchType by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.matchType ?: "SIMILARITY") }
    var targetChat by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.targetChat ?: "ALL") }
    var replyMode by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.replyMode ?: "SINGLE") }
    
    var delayMin by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.delayMin?.toString() ?: "0") }
    var delayMax by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.delayMax?.toString() ?: "0") }

    var askForName by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.askForName ?: false) }
    var promptQuestion by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.promptQuestion ?: "What's your name?") }
    var confirmQuestion by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.confirmQuestion ?: "Is your name {name}? Reply YES or NO.") }
    var retryQuestion by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.retryQuestion ?: "No problem. What's your name again?") }
    var successMessage by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.successMessage ?: "Thank you {name}! Your details have been saved.") }
    var assignTag by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.assignTag ?: "") }
    
    var label by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.label ?: "") }
    var targetVersion by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.targetVersion ?: "ANY") }
    
    var screenOff by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireScreenOff ?: false) }
    var charging by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireCharging ?: false) }
    var dnd by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.requireDnd ?: false) }
    var startTime by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.startTime ?: "") }
    var endTime by rememberSaveable(existingRule?.id) { mutableStateOf(existingRule?.endTime ?: "") }

    var expandedCard by rememberSaveable { mutableStateOf("TRIGGER") }
    
    var showMenu by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showVersionsDialog by remember { mutableStateOf(false) }
    var tempLabel by remember { mutableStateOf(label) }

    val showOtherCards = existingRule != null || trigger.isNotBlank() || matchType == "WELCOME"

    val replies = remember { mutableStateListOf<String>() }
    LaunchedEffect(existingRule) {
        if (existingRule != null) {
            replies.addAll(existingRule.replyMessage.split("|").filter { it.isNotBlank() })
        } else {
            replies.add("")
        }
    }

    var showTargetMenu by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    fun insertVariable(varName: String, isLeadField: Boolean = false) {
        if (isLeadField) successMessage += " {$varName}"
        else if (replies.isNotEmpty()) replies[replies.size - 1] += " {$varName}"
    }

    val canSave = (trigger.isNotBlank() || matchType == "WELCOME") && (askForName || replies.any { it.isNotBlank() })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // REMOVED TOP BAR HERE - IT IS HANDLED BY MainActivity.kt
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (canSave) {
                        val combinedReplies = replies.filter { it.isNotBlank() }.joinToString("|")
                        val finalName = if (matchType == "WELCOME") "Welcome Message" else if (trigger.isBlank()) "New Rule" else trigger
                        
                        val newRule = existingRule?.copy(
                            name = finalName, triggerKeyword = trigger, replyMessage = combinedReplies,
                            matchType = matchType, targetChat = targetChat, replyMode = replyMode,
                            delayMin = delayMin.toIntOrNull() ?: 0, delayMax = delayMax.toIntOrNull() ?: 0,
                            askForName = askForName, promptQuestion = promptQuestion, confirmQuestion = confirmQuestion,
                            retryQuestion = retryQuestion, successMessage = successMessage, assignTag = assignTag,
                            requireScreenOff = screenOff, requireCharging = charging, requireDnd = dnd,
                            startTime = startTime, endTime = endTime, label = label, targetVersion = targetVersion
                        ) ?: AutomationRule(
                            name = finalName, triggerKeyword = trigger, replyMessage = combinedReplies,
                            matchType = matchType, targetChat = targetChat, replyMode = replyMode,
                            delayMin = delayMin.toIntOrNull() ?: 0, delayMax = delayMax.toIntOrNull() ?: 0,
                            askForName = askForName, promptQuestion = promptQuestion, confirmQuestion = confirmQuestion,
                            retryQuestion = retryQuestion, successMessage = successMessage, assignTag = assignTag,
                            requireScreenOff = screenOff, requireCharging = charging, requireDnd = dnd,
                            startTime = startTime, endTime = endTime, parentRuleId = parentId, label = label, targetVersion = targetVersion
                        )
                        onSave(newRule)
                    } else {
                        showValidationError = true
                    }
                },
                containerColor = if (canSave) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.imePadding()
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Rule", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorCard(
                title = "Received message", 
                icon = Icons.Default.Message, 
                isExpanded = expandedCard == "TRIGGER",
                onToggle = { expandedCard = if (expandedCard == "TRIGGER") "" else "TRIGGER" }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it },
                        label = { Text("Trigger words") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = matchType == "WELCOME",
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (matchType != "WELCOME") {
                        Button(
                            onClick = { trigger = "*"; matchType = "EXACT" }, 
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (trigger == "*") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (trigger == "*") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("ALL (*)")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Send to:", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    Box {
                        OutlinedButton(onClick = { showTargetMenu = true }, shape = RoundedCornerShape(8.dp)) {
                            Text(if (targetChat == "ALL") "All chats" else if (targetChat == "INDIVIDUALS") "Individual chats" else if (targetChat == "GROUPS") "Groups only" else "Specific contacts")
                        }
                        DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                            DropdownMenuItem(text = { Text("All chats") }, onClick = { targetChat = "ALL"; showTargetMenu = false })
                            DropdownMenuItem(text = { Text("Individual chats only") }, onClick = { targetChat = "INDIVIDUALS"; showTargetMenu = false })
                            DropdownMenuItem(text = { Text("Groups only") }, onClick = { targetChat = "GROUPS"; showTargetMenu = false })
                            DropdownMenuItem(text = { Text("Specific contacts") }, onClick = { targetChat = "SPECIFIC"; showTargetMenu = false })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MatchOptionRow("Exact match", "EXACT", matchType) { 
                    matchType = it 
                    if (it != "WELCOME" && trigger == "%Welcome_message%") trigger = ""
                }
                MatchOptionRow("Similarity match", "SIMILARITY", matchType) { 
                    matchType = it 
                    if (it != "WELCOME" && trigger == "%Welcome_message%") trigger = ""
                }
                MatchOptionRow("Contains (pattern matching)", "PATTERN", matchType) { 
                    matchType = it 
                    if (it != "WELCOME" && trigger == "%Welcome_message%") trigger = ""
                }
                MatchOptionRow("RegEx", "REGEX", matchType) { 
                    matchType = it 
                    if (it != "WELCOME" && trigger == "%Welcome_message%") trigger = ""
                }
                MatchOptionRow("Welcome message", "WELCOME", matchType) { 
                    matchType = it 
                    if (it == "WELCOME") trigger = "%Welcome_message%"
                    else if (trigger == "%Welcome_message%") trigger = ""
                }
            }

            AnimatedVisibility(
                visible = showOtherCards,
                enter = fadeIn(tween(500)) + expandVertically(tween(500)),
                exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    
                    EditorCard(
                        title = "Automatic reply", 
                        icon = Icons.Default.Reply,
                        isExpanded = expandedCard == "REPLY",
                        onToggle = { expandedCard = if (expandedCard == "REPLY") "" else "REPLY" }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AssistChip(onClick = { insertVariable("name") }, label = { Text("{name}", fontSize = 10.sp) }, modifier = Modifier.padding(end = 4.dp))
                            AssistChip(onClick = { insertVariable("number") }, label = { Text("{number}", fontSize = 10.sp) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        replies.forEachIndexed { index, reply ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                OutlinedTextField(
                                    value = reply,
                                    onValueChange = { replies[index] = it },
                                    label = { Text("Should be sent (${index + 1})") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )
                                if (replies.size > 1) {
                                    IconButton(onClick = { replies.removeAt(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Multiple replies", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode = "SINGLE" }) {
                                RadioButton(selected = replyMode == "SINGLE", onClick = { replyMode = "SINGLE" })
                                Text("1")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode = "ALL" }) {
                                RadioButton(selected = replyMode == "ALL", onClick = { replyMode = "ALL" })
                                Text("All")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { replyMode = "RANDOM" }) {
                                RadioButton(selected = replyMode == "RANDOM", onClick = { replyMode = "RANDOM" })
                                Text("Random")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { replies.add("") }, 
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Reply", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Delay in seconds", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = delayMin,
                                onValueChange = { delayMin = it },
                                label = { Text("min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = delayMax,
                                onValueChange = { delayMax = it },
                                label = { Text("max") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    EditorCard(
                        title = "Save lead", 
                        icon = Icons.Default.PersonAdd, 
                        titleColor = MaterialTheme.colorScheme.tertiary,
                        isExpanded = expandedCard == "LEAD",
                        onToggle = { expandedCard = if (expandedCard == "LEAD") "" else "LEAD" }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ask their name and save the number", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Switch(checked = askForName, onCheckedChange = { askForName = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.tertiary))
                        }
                        
                        if (askForName) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = promptQuestion, onValueChange = { promptQuestion = it }, label = { Text("Question to ask name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = confirmQuestion, onValueChange = { confirmQuestion = it }, label = { Text("Confirmation (YES/NO)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = retryQuestion, onValueChange = { retryQuestion = it }, label = { Text("Retry if NO") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = successMessage, onValueChange = { successMessage = it }, label = { Text("Success Message") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                IconButton(onClick = { insertVariable("name", isLeadField = true) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Insert {name}", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = assignTag, onValueChange = { assignTag = it }, label = { Text("Assign Tag (e.g. TikTok, Facebook)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        }
                    }

                    EditorCard(
                        title = "Conditions (optional)", 
                        icon = Icons.Default.Tune,
                        isExpanded = expandedCard == "CONDITIONS",
                        onToggle = { expandedCard = if (expandedCard == "CONDITIONS") "" else "CONDITIONS" }
                    ) {
                        ConditionSwitchRow("Only reply if screen is off", screenOff) { screenOff = it }
                        ConditionSwitchRow("Only reply if device is charging", charging) { charging = it }
                        ConditionSwitchRow("Only reply if Do Not Disturb is on", dnd) { dnd = it }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Time Conditions (Business Hours)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Leave blank to reply 24/7", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { if (it.length <= 5) startTime = it },
                                label = { Text("Start Time") },
                                placeholder = { Text("09:00") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { if (it.length <= 5) endTime = it },
                                label = { Text("End Time") },
                                placeholder = { Text("17:00") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    if (existingRule != null) {
                        EditorCard(
                            title = "Submenu / Conversation Flow", 
                            icon = Icons.Default.AccountTree,
                            isExpanded = expandedCard == "SUBMENU",
                            onToggle = { expandedCard = if (expandedCard == "SUBMENU") "" else "SUBMENU" }
                        ) {
                            Text("Rules added here will trigger after the contact is saved, or if they choose an option.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            childRules.forEach { child ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onAddChildRule(existingRule.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Submenu Rule")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Submenu Rule")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) 
        }
    }

    // --- DIALOGS ---
    if (showLabelDialog) {
        AlertDialog(
            onDismissRequest = { showLabelDialog = false },
            title = { Text("Add Label") },
            text = {
                OutlinedTextField(
                    value = tempLabel,
                    onValueChange = { tempLabel = it },
                    label = { Text("Label name (e.g. Sales, Support)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { 
                    label = tempLabel
                    showLabelDialog = false 
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLabelDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showVersionsDialog) {
        AlertDialog(
            onDismissRequest = { showVersionsDialog = false },
            title = { Text("Specific versions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { targetVersion = "ANY" }) {
                        RadioButton(selected = targetVersion == "ANY", onClick = { targetVersion = "ANY" })
                        Text("Any (Default)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { targetVersion = "ORIGINAL" }) {
                        RadioButton(selected = targetVersion == "ORIGINAL", onClick = { targetVersion = "ORIGINAL" })
                        Text("WhatsApp (Original)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { targetVersion = "BUSINESS" }) {
                        RadioButton(selected = targetVersion == "BUSINESS", onClick = { targetVersion = "BUSINESS" })
                        Text("WhatsApp Business")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showVersionsDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showVersionsDialog = false }) { Text("CANCEL") }
            }
        )
    }

    LaunchedEffect(showValidationError) {
        if (showValidationError) {
            snackbarHostState.showSnackbar("Please fill in the trigger and a reply.")
            showValidationError = false
        }
    }
}

@Composable
fun EditorCard(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    titleColor: Color = MaterialTheme.colorScheme.primary, 
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, animationSpec = tween(300))
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }, 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, color = titleColor, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Expand", 
                    tint = titleColor.copy(alpha = 0.7f),
                    modifier = Modifier.rotate(rotation)
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun MatchOptionRow(title: String, type: String, currentType: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect(type) }) {
        RadioButton(selected = currentType == type, onClick = { onSelect(type) })
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f))
        var showDialog by remember { mutableStateOf(false) }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(title) },
                text = { Text(getInfoText(type)) },
                confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } }
            )
        }
    }
}

@Composable
fun ConditionSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun getInfoText(type: String): String {
    return when (type) {
        "EXACT" -> "Ignores accents, upper and lower case, and redundant spaces. Also ignores emojis and punctuation if they are not used in your rule."
        "SIMILARITY" -> "Replies to different variations of a message. The app uses an algorithm to compute a confidence score. It can only match messages with a similar length."
        "PATTERN" -> "Reply if... message starts with 'Test': test*\n...message ends with 'Test': *test\n...message contains 'Test': *test*\n\n* stands for [anything].\n// stands for [or]."
        "REGEX" -> "This option allows complex conditions using regular expressions. Search the internet for 'java regular expression' for a reference."
        "WELCOME" -> "Replies to the first message received from a NON-CONTACT since the app is active. You can activate it for groups, too."
        else -> ""
    }
}
