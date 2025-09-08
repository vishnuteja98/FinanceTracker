package com.financetracker.data.database

import androidx.room.*
import com.financetracker.data.models.BankAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    
    @Query("SELECT * FROM bank_accounts WHERE isActive = 1 ORDER BY bankName, accountName")
    fun getAllActiveBankAccounts(): Flow<List<BankAccount>>
    
    @Query("SELECT * FROM bank_accounts ORDER BY bankName, accountName")
    fun getAllBankAccounts(): Flow<List<BankAccount>>
    
    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountById(id: Long): BankAccount?
    
    @Query("SELECT * FROM bank_accounts WHERE accountNumber = :accountNumber AND isActive = 1")
    suspend fun getBankAccountByAccountNumber(accountNumber: String): BankAccount?
    
    @Query("SELECT * FROM bank_accounts WHERE bankName = :bankName AND isActive = 1")
    suspend fun getBankAccountsByBankName(bankName: String): List<BankAccount>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bankAccount: BankAccount): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccounts(bankAccounts: List<BankAccount>)
    
    @Update
    suspend fun updateBankAccount(bankAccount: BankAccount)
    
    @Delete
    suspend fun deleteBankAccount(bankAccount: BankAccount)
    
    @Query("UPDATE bank_accounts SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateBankAccount(id: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE bank_accounts SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun activateBankAccount(id: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM bank_accounts WHERE isActive = 1")
    suspend fun getActiveBankAccountCount(): Int
    
    // Search for bank account that might match the extracted bank info from SMS
    @Query("""
        SELECT * FROM bank_accounts 
        WHERE isActive = 1 AND (
            bankName LIKE '%' || :searchTerm || '%' OR 
            accountName LIKE '%' || :searchTerm || '%' OR
            accountNumber LIKE '%' || :searchTerm || '%'
        )
        ORDER BY 
            CASE 
                WHEN bankName LIKE :searchTerm THEN 1
                WHEN accountName LIKE :searchTerm THEN 2
                WHEN accountNumber LIKE :searchTerm THEN 3
                ELSE 4
            END
    """)
    suspend fun searchBankAccountsByKeyword(searchTerm: String): List<BankAccount>
    
    @Query("""
        SELECT * FROM bank_accounts 
        WHERE isActive = 1 AND accountNumber IS NOT NULL 
        AND accountNumber LIKE '%' || :lastFourDigits
        ORDER BY bankName, accountName
    """)
    suspend fun findAccountsByLastFourDigits(lastFourDigits: String): List<BankAccount>
}
