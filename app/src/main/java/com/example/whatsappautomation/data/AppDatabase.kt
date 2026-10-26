package com.example.whatsappautomation.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Contact::class, AutomationRule::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatsapp_automation_database"
                )
                .fallbackToDestructiveMigration() // Wipes old data to prevent crashes during development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
