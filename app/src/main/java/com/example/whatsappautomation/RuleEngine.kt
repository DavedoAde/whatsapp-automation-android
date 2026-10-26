package com.example.whatsappautomation

import com.example.whatsappautomation.data.AutomationRule

object RuleEngine {
    /**
     * Finds the best matching rule for an incoming message.
     * Returns null if no rules match.
     */
    fun findMatchingRule(message: String, rules: List<AutomationRule>): AutomationRule? {
        if (message.isBlank()) return null

        // Filter only active rules and sort by priority (lowest number = highest priority)
        val activeRules = rules.filter { it.isActive }.sortedBy { it.priority }

        // For now, we do a simple "contains" check, ignoring case.
        for (rule in activeRules) {
            if (message.contains(rule.triggerKeyword, ignoreCase = true)) {
                return rule
            }
        }
        return null
    }
}
