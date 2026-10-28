package com.example.whatsappautomation.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY captureDate DESC")
    fun getAllContacts(): Flow<List<Contact>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)
    @Query("SELECT COUNT(*) FROM contacts")
    fun getContactCount(): Flow<Int>
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC")
    fun getAllRules(): Flow<List<AutomationRule>>
    @Query("SELECT * FROM rules ORDER BY priority ASC")
    suspend fun getAllRulesSync(): List<AutomationRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)
    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
    @Query("SELECT COUNT(*) FROM rules WHERE isActive = 1")
    fun getActiveRuleCount(): Flow<Int>
}

// NEW: Log DAO
@Dao
interface LogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
    
    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}
