package com.financetracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.financetracker.data.models.BankAccount
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.Converters

@Database(
    entities = [Transaction::class, BankAccount::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanceTrackerDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    
    companion object {
        @Volatile
        private var INSTANCE: FinanceTrackerDatabase? = null
        
        fun getDatabase(context: Context): FinanceTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceTrackerDatabase::class.java,
                    "finance_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
