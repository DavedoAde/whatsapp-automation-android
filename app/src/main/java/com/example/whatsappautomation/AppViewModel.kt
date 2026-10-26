package com.example.whatsappautomation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappautomation.data.AppDatabase
import com.example.whatsappautomation.data.AutomationRule
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

    val totalContactsCount = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allRules = ruleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(name: String, keyword: String, reply: String) {
        viewModelScope.launch {
            val newRule = AutomationRule(
                name = name,
                triggerKeyword = keyword,
                replyMessage = reply,
                isActive = true,
                priority = 1
            )
            ruleDao.insertRule(newRule)
        }
    }

    fun updateRule(updatedRule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.insertRule(updatedRule) // Room's REPLACE strategy handles the update
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.deleteRuleById(rule.id)
        }
    }

    fun simulateMessage(message: String): AutomationRule? {
        val currentRules = allRules.value
        return RuleEngine.findMatchingRule(message, currentRules)
    }
}
