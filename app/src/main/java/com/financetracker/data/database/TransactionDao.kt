package com.financetracker.data.database

import androidx.room.*
import com.financetracker.data.models.CategorySpending
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.models.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE status = :status AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE bankAccountId = :bankAccountId AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByBankAccount(bankAccountId: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE bankAccountId = :bankAccountId AND status = :status AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByBankAccountAndStatus(bankAccountId: Long, status: TransactionStatus): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND isDeleted = 0 ORDER BY messageReceivedAt DESC")
    fun getPendingTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE status = 'TAGGED' AND isDeleted = 0 ORDER BY messageReceivedAt DESC")
    fun getTaggedTransactions(): Flow<List<Transaction>>
    
    // Enhanced queries for filtering, sorting, and pagination
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'PENDING' AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
        ORDER BY 
            CASE WHEN :sortBy = 'MESSAGE_RECEIVED' THEN messageReceivedAt END DESC,
            CASE WHEN :sortBy = 'AMOUNT_HIGH_TO_LOW' THEN amount END DESC,
            CASE WHEN :sortBy = 'AMOUNT_LOW_TO_HIGH' THEN amount END ASC,
            CASE WHEN :sortBy = 'CATEGORY' THEN category END ASC,
            CASE WHEN :sortBy = 'LAST_MODIFIED' THEN lastModifiedAt END DESC,
            messageReceivedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getPendingTransactionsFiltered(
        startDate: Long? = null,
        endDate: Long? = null,
        sortBy: String = "MESSAGE_RECEIVED",
        limit: Int = 50,
        offset: Int = 0
    ): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'TAGGED' AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
        AND (category != 'Low Value' OR category IS NULL)
        ORDER BY 
            CASE WHEN :sortBy = 'MESSAGE_RECEIVED' THEN messageReceivedAt END DESC,
            CASE WHEN :sortBy = 'AMOUNT_HIGH_TO_LOW' THEN amount END DESC,
            CASE WHEN :sortBy = 'AMOUNT_LOW_TO_HIGH' THEN amount END ASC,
            CASE WHEN :sortBy = 'CATEGORY' THEN category END ASC,
            CASE WHEN :sortBy = 'LAST_MODIFIED' THEN lastModifiedAt END DESC,
            messageReceivedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getTaggedTransactionsFiltered(
        startDate: Long? = null,
        endDate: Long? = null,
        sortBy: String = "MESSAGE_RECEIVED",
        limit: Int = 50,
        offset: Int = 0
    ): Flow<List<Transaction>>
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE status = 'PENDING' AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
    """)
    suspend fun getPendingTransactionCount(
        startDate: Long? = null,
        endDate: Long? = null
    ): Int
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE status = 'TAGGED' AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
        AND (category != 'Low Value' OR category IS NULL)
    """)
    suspend fun getTaggedTransactionCount(
        startDate: Long? = null,
        endDate: Long? = null
    ): Int
    
    @Query("SELECT * FROM transactions WHERE category = :category AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    

    
    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE originalMessage = :originalMessage")
    suspend fun getTransactionByOriginalMessage(originalMessage: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long)
    
    @Query("UPDATE transactions SET status = :status, lastModifiedAt = :lastModifiedAt WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus, lastModifiedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE transactions SET bankAccountId = :bankAccountId, lastModifiedAt = :lastModifiedAt WHERE id = :id")
    suspend fun updateTransactionBankAccount(id: Long, bankAccountId: Long?, lastModifiedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE transactions SET category = :category, subcategory = :subcategory, notes = :notes, status = 'TAGGED', lastModifiedAt = :lastModifiedAt WHERE id = :id")
    suspend fun tagTransaction(id: Long, category: String?, subcategory: String?, notes: String?, lastModifiedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING' AND isDeleted = 0")
    fun getPendingTransactionCount(): Flow<Int>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = :type AND isDeleted = 0 AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE bankAccountId = :bankAccountId AND transactionType = :type AND isDeleted = 0")
    suspend fun getTotalAmountByBankAccountAndType(bankAccountId: Long, type: TransactionType): Double?
    
    // Analytics queries for category-wise spending
    @Query("""
        SELECT category, SUM(amount) as totalAmount, COUNT(*) as transactionCount
        FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
        AND category IS NOT NULL
        GROUP BY category
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryWiseSpending(startDate: Long? = null, endDate: Long? = null): List<CategorySpending>
    
    @Query("""
        SELECT 
            CASE 
                WHEN category IS NULL OR category = '' THEN 'Uncategorized'
                ELSE category 
            END as category,
            SUM(amount) as totalAmount, 
            COUNT(*) as transactionCount
        FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND isDeleted = 0 
        AND (:startDate IS NULL OR messageReceivedAt >= :startDate)
        AND (:endDate IS NULL OR messageReceivedAt <= :endDate)
        GROUP BY 
            CASE 
                WHEN category IS NULL OR category = '' THEN 'Uncategorized'
                ELSE category 
            END
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryWiseSpendingIncludingUncategorized(startDate: Long? = null, endDate: Long? = null): List<CategorySpending>
}
