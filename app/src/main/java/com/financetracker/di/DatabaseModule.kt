package com.financetracker.di

import android.content.Context
import androidx.room.Room
import com.financetracker.data.database.BankAccountDao
import com.financetracker.data.database.FinanceTrackerDatabase
import com.financetracker.data.database.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFinanceTrackerDatabase(@ApplicationContext context: Context): FinanceTrackerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FinanceTrackerDatabase::class.java,
            "finance_tracker_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTransactionDao(database: FinanceTrackerDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideBankAccountDao(database: FinanceTrackerDatabase): BankAccountDao {
        return database.bankAccountDao()
    }
}
