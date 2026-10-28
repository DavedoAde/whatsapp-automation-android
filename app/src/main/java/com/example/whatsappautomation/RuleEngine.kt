package com.example.whatsappautomation

import android.content.Context
import com.example.whatsappautomation.data.AutomationRule

object RuleEngine {
    
    fun findMatchingRule(
        context: Context, 
        message: String, 
        rules: List<AutomationRule>, 
        checkConditions: Boolean = true
    ): AutomationRule? {
        if (message.isBlank()) return null

        val activeRules = rules.filter { it.isActive }.sortedBy { it.priority }

        for (rule in activeRules) {
            
            // 1. Check Hardware Conditions
            if (checkConditions) {
                if (rule.requireScreenOff && !ConditionChecker.isScreenOff(context)) continue
                if (rule.requireCharging && !ConditionChecker.isCharging(context)) continue
                if (rule.requireDnd && !ConditionChecker.isDndActive(context)) continue
                
                // 2. Check Time Conditions
                if (!ConditionChecker.isWithinTimeRange(rule.startTime, rule.endTime)) continue
            }

            // 3. Check Text Matching
            val isMatch = if (rule.triggerKeyword.trim() == "*") {
                true // Catch-all
            } else {
                when (rule.matchType) {
                    "EXACT" -> normalizeText(message) == normalizeText(rule.triggerKeyword)
                    "SIMILARITY" -> calculateSimilarity(message, rule.triggerKeyword) >= 0.75
                    "PATTERN" -> checkWildcardPattern(message, rule.triggerKeyword)
                    "REGEX" -> {
                        try {
                            Regex(rule.triggerKeyword, RegexOption.IGNORE_CASE).containsMatchIn(message)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    "WELCOME" -> true 
                    else -> message.contains(rule.triggerKeyword, ignoreCase = true)
                }
            }

            if (isMatch) return rule
        }
        return null
    }

    private fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        if (longer.isEmpty()) return 1.0
        val distance = levenshteinDistance(longer.lowercase(), shorter.lowercase())
        return (longer.length - distance).toDouble() / longer.length.toDouble()
    }

    private fun levenshteinDistance(lhs: String, rhs: String): Int {
        val lhsLength = lhs.length + 1
        val rhsLength = rhs.length + 1
        val cost = Array(lhsLength) { IntArray(rhsLength) }
        for (i in 0 until lhsLength) cost[i][0] = i
        for (j in 0 until rhsLength) cost[0][j] = j
        for (i in 1 until lhsLength) {
            for (j in 1 until rhsLength) {
                cost[i][j] = minOf(
                    cost[i - 1][j] + 1, cost[i][j - 1] + 1,
                    cost[i - 1][j - 1] + if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                )
            }
        }
        return cost[lhsLength - 1][rhsLength - 1]
    }

    private fun checkWildcardPattern(message: String, pattern: String): Boolean {
        val regexPattern = pattern.replace("//", "|").replace("*", ".*")
        return try {
            Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
        } catch (e: Exception) { false }
    }

    fun getReplyText(rule: AutomationRule, name: String = "", number: String = ""): String {
        val replies = rule.replyMessage.split("|").filter { it.isNotBlank() }
        if (replies.isEmpty()) return ""

        val selectedReply = when (rule.replyMode) {
            "ALL" -> replies.joinToString("\n\n")
            "RANDOM" -> replies.random()
            else -> replies.first()
        }

        return selectedReply.replace("{name}", if (name.isNotBlank()) name else "there").replace("{number}", number)
    }

    fun extractName(input: String): String {
        val lower = input.trim().lowercase()
        val prefixes = listOf("my name is ", "i am ", "im ", "call me ", "it's ", "its ")
        var name = input.trim()
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                name = name.substring(prefix.length).trim()
                break
            }
        }
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
