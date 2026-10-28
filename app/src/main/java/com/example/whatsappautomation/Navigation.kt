package com.example.whatsappautomation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Rules : Screen("rules", "Rules", Icons.Default.List)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Person)
    object Logs : Screen("logs", "Logs", Icons.Default.History) // NEW
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
