package com.example.whatsappautomation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppAutomationTheme {
                DashboardScreen()
            }
        }
    }
}

@Composable
fun WhatsAppAutomationTheme(content: @Composable () -> Unit) {
    val darkNavy = Color(0xFF0A1929)
    val charcoal = Color(0xFF1E2A3A)
    val accentGreen = Color(0xFF4CAF50)
    val textLight = Color(0xFFE0E0E0)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentGreen,
            background = darkNavy,
            surface = charcoal,
            onBackground = textLight,
            onSurface = textLight
        ),
        content = content
    )
}

@Composable
fun DashboardScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WhatsApp Automation",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Milestone 1: Build skeleton ready.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Next steps:",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Add Room database", color = MaterialTheme.colorScheme.onSurface)
                    Text("• Implement rule engine", color = MaterialTheme.colorScheme.onSurface)
                    Text("• Notification listener", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
