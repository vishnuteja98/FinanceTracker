package com.financetracker.data.repository

import com.financetracker.data.database.TransactionDao
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.models.TransactionType
import com.financetracker.data.models.TransactionFilter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByStatus(status)
    
    fun getTransactionsByBankAccount(bankAccountId: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByBankAccount(bankAccountId)
    
    fun getTransactionsByBankAccountAndStatus(bankAccountId: Long, status: TransactionStatus): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByBankAccountAndStatus(bankAccountId, status)
    
    fun getPendingTransactions(): Flow<List<Transaction>> = transactionDao.getPendingTransactions()
    
    fun getTaggedTransactions(): Flow<List<Transaction>> = transactionDao.getTaggedTransactions()
    
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(category)
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startDate, endDate)
    
    suspend fun getTransactionByOriginalMessage(originalMessage: String): Transaction? = 
        transactionDao.getTransactionByOriginalMessage(originalMessage)
    
    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    
    suspend fun insertTransaction(transaction: Transaction): Long = 
        transactionDao.insertTransaction(transaction)
    
    suspend fun insertTransactions(transactions: List<Transaction>) = 
        transactionDao.insertTransactions(transactions)
    
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    
    suspend fun softDeleteTransaction(id: Long) = transactionDao.softDeleteTransaction(id)
    
    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus) = 
        transactionDao.updateTransactionStatus(id, status)
    
    suspend fun updateTransactionBankAccount(id: Long, bankAccountId: Long?) = 
        transactionDao.updateTransactionBankAccount(id, bankAccountId)
    
    suspend fun tagTransaction(id: Long, category: String?, subcategory: String?, notes: String?) = 
        transactionDao.tagTransaction(id, category, subcategory, notes)
    
    fun getPendingTransactionCount(): Flow<Int> = transactionDao.getPendingTransactionCount()
    
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double = 
        transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    
    suspend fun getTotalAmountByBankAccountAndType(bankAccountId: Long, type: TransactionType): Double = 
        transactionDao.getTotalAmountByBankAccountAndType(bankAccountId, type) ?: 0.0
    
    suspend fun isDuplicateTransaction(originalMessage: String): Boolean {
        return getTransactionByOriginalMessage(originalMessage) != null
    }
    
    // Enhanced methods with filtering, sorting, and pagination
    fun getPendingTransactionsFiltered(filter: TransactionFilter): Flow<List<Transaction>> = 
        transactionDao.getPendingTransactionsFiltered(
            startDate = filter.getStartDate(),
            endDate = filter.getEndDate(),
            sortBy = filter.sortOption.dbValue,
            limit = filter.pageSize,
            offset = filter.getOffset()
        )
    
    fun getTaggedTransactionsFiltered(filter: TransactionFilter): Flow<List<Transaction>> = 
        transactionDao.getTaggedTransactionsFiltered(
            startDate = filter.getStartDate(),
            endDate = filter.getEndDate(),
            sortBy = filter.sortOption.dbValue,
            limit = filter.pageSize,
            offset = filter.getOffset()
        )
    
    suspend fun getPendingTransactionCount(filter: TransactionFilter): Int = 
        transactionDao.getPendingTransactionCount(
            startDate = filter.getStartDate(),
            endDate = filter.getEndDate()
        )
    
    suspend fun getTaggedTransactionCount(filter: TransactionFilter): Int = 
        transactionDao.getTaggedTransactionCount(
            startDate = filter.getStartDate(),
            endDate = filter.getEndDate()
        )
}
