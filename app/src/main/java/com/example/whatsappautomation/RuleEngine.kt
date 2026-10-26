package com.example.whatsappautomation

import com.example.whatsappautomation.data.AutomationRule

object RuleEngine {
    
    fun findMatchingRule(message: String, rules: List<AutomationRule>): AutomationRule? {
        if (message.isBlank()) return null

        val activeRules = rules.filter { it.isActive }.sortedBy { it.priority }

        for (rule in activeRules) {
            val isMatch = when (rule.matchType) {
                "EXACT" -> message.trim().equals(rule.triggerKeyword.trim(), ignoreCase = true)
                "REGEX" -> {
                    try {
                        Regex(rule.triggerKeyword, RegexOption.IGNORE_CASE).containsMatchIn(message)
                    } catch (e: Exception) {
                        false // Invalid regex, ignore
                    }
                }
                else -> message.contains(rule.triggerKeyword, ignoreCase = true) // SIMILARITY / CONTAINS
            }

            if (isMatch) {
                return rule
            }
        }
        return null
    }
}
