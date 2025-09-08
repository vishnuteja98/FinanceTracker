package com.financetracker.data.repository

import com.financetracker.data.database.BankAccountDao
import com.financetracker.data.models.BankAccount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankAccountRepository @Inject constructor(
    private val bankAccountDao: BankAccountDao
) {
    
    fun getAllActiveBankAccounts(): Flow<List<BankAccount>> = bankAccountDao.getAllActiveBankAccounts()
    
    fun getAllBankAccounts(): Flow<List<BankAccount>> = bankAccountDao.getAllBankAccounts()
    
    suspend fun getBankAccountById(id: Long): BankAccount? = bankAccountDao.getBankAccountById(id)
    
    suspend fun getBankAccountByAccountNumber(accountNumber: String): BankAccount? = 
        bankAccountDao.getBankAccountByAccountNumber(accountNumber)
    
    suspend fun getBankAccountsByBankName(bankName: String): List<BankAccount> = 
        bankAccountDao.getBankAccountsByBankName(bankName)
    
    suspend fun insertBankAccount(bankAccount: BankAccount): Long = 
        bankAccountDao.insertBankAccount(bankAccount)
    
    suspend fun insertBankAccounts(bankAccounts: List<BankAccount>) = 
        bankAccountDao.insertBankAccounts(bankAccounts)
    
    suspend fun updateBankAccount(bankAccount: BankAccount) = bankAccountDao.updateBankAccount(bankAccount)
    
    suspend fun deleteBankAccount(bankAccount: BankAccount) = bankAccountDao.deleteBankAccount(bankAccount)
    
    suspend fun deactivateBankAccount(id: Long) = bankAccountDao.deactivateBankAccount(id)
    
    suspend fun activateBankAccount(id: Long) = bankAccountDao.activateBankAccount(id)
    
    suspend fun getActiveBankAccountCount(): Int = bankAccountDao.getActiveBankAccountCount()
    
    suspend fun searchBankAccountsByKeyword(searchTerm: String): List<BankAccount> = 
        bankAccountDao.searchBankAccountsByKeyword(searchTerm)
    
    suspend fun findAccountsByLastFourDigits(lastFourDigits: String): List<BankAccount> = 
        bankAccountDao.findAccountsByLastFourDigits(lastFourDigits)
    
    suspend fun findMatchingBankAccount(extractedBankInfo: String?): BankAccount? {
        if (extractedBankInfo.isNullOrBlank()) return null
        
        val searchResults = searchBankAccountsByKeyword(extractedBankInfo)
        return searchResults.firstOrNull()
    }
    
    suspend fun createDefaultBankAccounts() {
        val existingCount = getActiveBankAccountCount()
        if (existingCount == 0) {
            val defaultAccounts = listOf(
                BankAccount(
                    accountName = "Primary Savings",
                    bankName = "Unknown Bank",
                    accountType = "SAVINGS",
                    identifierKeywords = listOf("savings", "primary")
                ),
                BankAccount(
                    accountName = "Credit Card",
                    bankName = "Unknown Bank",
                    accountType = "CREDIT_CARD",
                    identifierKeywords = listOf("credit", "card")
                )
            )
            insertBankAccounts(defaultAccounts)
        }
    }
}
