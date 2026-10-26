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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)
    
    @Query("SELECT COUNT(*) FROM rules WHERE isActive = 1")
    fun getActiveRuleCount(): Flow<Int>
}
