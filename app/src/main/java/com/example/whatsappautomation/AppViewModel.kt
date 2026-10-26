package com.example.whatsappautomation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappautomation.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val contactDao = database.contactDao()
    private val ruleDao = database.ruleDao()

    // Local UI State
    private val _isAutomationActive = MutableStateFlow(true)
    val isAutomationActive: StateFlow<Boolean> = _isAutomationActive

    fun toggleAutomation(isActive: Boolean) {
        _isAutomationActive.value = isActive
    }

    // Read from database
    val activeRulesCount = ruleDao.getActiveRuleCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalContactsCount = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Function to add dummy data for testing
    fun addMockData() {
        viewModelScope.launch {
            contactDao.insertContact(
                com.example.whatsappautomation.data.Contact(
                    phoneNumber = "+1234567890",
                    name = "Test Contact",
                    sourceApp = "WhatsApp"
                )
            )
            ruleDao.insertRule(
                com.example.whatsappautomation.data.AutomationRule(
                    name = "Test Rule",
                    triggerKeyword = "Hello",
                    replyMessage = "Hi there!"
                )
            )
        }
    }
}
