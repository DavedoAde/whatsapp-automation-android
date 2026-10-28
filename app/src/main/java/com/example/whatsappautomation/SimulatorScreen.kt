package com.example.whatsappautomation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Added timestamp to the data class
data class ChatMessage(
    val text: String, 
    val isUser: Boolean, 
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var inputMessage by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var isBotTyping by remember { mutableStateOf(false) }
    
    val isAutomationActive by viewModel.isAutomationActive.collectAsState()
    var showOffDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isAutomationActive) {
        if (!isAutomationActive) {
            showOffDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage("Hello, I'm your WAcopilot! You can test the rules by sending messages to this chat.", false))
        }
    }

    LaunchedEffect(messages.size, isBotTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    if (showOffDialog) {
        AlertDialog(
            onDismissRequest = { showOffDialog = false; onBack() },
            title = { Text("Test your rules") },
            text = { Text("WAcopilot is off. Please activate it using the switch at the top.") },
            confirmButton = {
                Button(onClick = { showOffDialog = false; onBack() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            // Removed the card-like background from the input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B141A)) // Matches the main background
                    .padding(8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F2C34),
                        unfocusedContainerColor = Color(0xFF1F2C34),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputMessage.isNotBlank() && isAutomationActive) {
                            val userText = inputMessage
                            messages.add(ChatMessage(userText, true))
                            inputMessage = ""
                            
                            isBotTyping = true

                            scope.launch {
                                val botReplyText = viewModel.simulateMessage(userText)
                                isBotTyping = false
                                
                                if (botReplyText.isNotBlank()) {
                                    messages.add(ChatMessage(botReplyText, false))
                                }
                            }
                        }
                    },
                    containerColor = if (isAutomationActive) Color(0xFF00A884) else Color.Gray,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B141A))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp) // Adjusted top padding
            ) {
                // --- TODAY PILL ---
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F2C34)
                        ) {
                            Text(
                                text = "Today",
                                color = Color(0xFF8696A0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                // -----------------

                items(messages) { message ->
                    ChatBubble(message)
                }
                
                if (isBotTyping) {
                    item {
                        TypingBubble()
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) Color(0xFF005C4B) else Color(0xFF202C33)
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    
    // Time formatter
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormatter.format(Date(message.timestamp))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape)
                .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            // Row to align text and timestamp at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFE9EDEF),
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeString,
                    color = Color(0xFF8696A0),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TypingBubble() {
    val bubbleColor = Color(0xFF202C33)
    val shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Box(modifier = Modifier.background(bubbleColor, shape).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = "typing...", color = Color(0xFF8696A0), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
