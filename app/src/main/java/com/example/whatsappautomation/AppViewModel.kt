package com.example.whatsappautomation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappautomation.data.AppDatabase
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val contactDao = database.contactDao()
    private val ruleDao = database.ruleDao()

    private val _isAutomationActive = MutableStateFlow(true)
    val isAutomationActive: StateFlow<Boolean> = _isAutomationActive

    fun toggleAutomation(isActive: Boolean) {
        _isAutomationActive.value = isActive
    }

    val activeRulesCount = ruleDao.getActiveRuleCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allContacts = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalContactsCount = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allRules = ruleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.insertRule(rule)
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.deleteRuleById(rule.id)
        }
    }

    // --- SIMULATION & TASKER LOGIC STATE ---
    private var currentSimulatedMenuId: Long? = null
    private var currentSenderId = "+1234567890" // Fake number for testing
    
    // This boolean acts like the Tasker trigger. If true, the next message is a name!
    private var isAwaitingContactName = false 

    fun simulateMessage(message: String): String {
        
        // 1. THE TASKER LOGIC: If we are waiting for a name, capture it immediately!
        if (isAwaitingContactName) {
            val extractedName = RuleEngine.extractName(message)
            
            viewModelScope.launch {
                // Save to App Database
                val newContact = Contact(phoneNumber = currentSenderId, name = extractedName, sourceApp = "WhatsApp")
                contactDao.insertContact(newContact)
                
                // Save to Phone's Actual Address Book
                val context = getApplication<Application>()
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    PhoneContactManager.saveContactToPhone(context, extractedName, currentSenderId)
                }
            }
            
            isAwaitingContactName = false // Reset the Tasker state
            return "Thank you, $extractedName! Your details have been saved." // This bypasses the YES/NO confirmation entirely
        }

        val allRulesList = allRules.value

        // 2. SUBMENU ROUTING: If we are currently inside a submenu, ONLY check child rules of that menu
        if (currentSimulatedMenuId != null) {
            val childRules = allRulesList.filter { it.parentRuleId == currentSimulatedMenuId && it.isActive }
            val matchedChild = RuleEngine.findMatchingRule(message, childRules)

            if (matchedChild != null) {
                // Check if this child has its own children (nested menus)
                val hasSubMenu = allRulesList.any { it.parentRuleId == matchedChild.id }
                currentSimulatedMenuId = if (hasSubMenu) matchedChild.id else null
                
                if (matchedChild.actionType == "CAPTURE_CONTACT") {
                    isAwaitingContactName = true
                }
                return RuleEngine.getReplyText(matchedChild)
            } else {
                // User typed something invalid. Stay in the menu and remind them.
                val parentMenu = allRulesList.find { it.id == currentSimulatedMenuId }
                val parentText = if (parentMenu != null) RuleEngine.getReplyText(parentMenu) else ""
                return "Invalid option. Please choose from the menu:\n\n$parentText"
            }
        }

        // 3. STANDARD TOP-LEVEL MATCHING
        val topLevelRules = allRulesList.filter { it.parentRuleId == null }
        val matchedRule = RuleEngine.findMatchingRule(message, topLevelRules)
        
        return if (matchedRule != null) {
            val hasSubMenu = allRulesList.any { it.parentRuleId == matchedRule.id }
            currentSimulatedMenuId = if (hasSubMenu) matchedRule.id else null

            if (matchedRule.actionType == "CAPTURE_CONTACT") {
                isAwaitingContactName = true
            }
            RuleEngine.getReplyText(matchedRule)
        } else {
            "No rule matched. (No reply sent)"
        }
    }
}
