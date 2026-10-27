package com.example.whatsappautomation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Person)
    object Rules : Screen("rules", "Rules", Icons.Default.List) // Changed icon to List so Settings can use the gear
    object Settings : Screen("settings", "Settings", Icons.Default.Settings) // Added Settings
}
