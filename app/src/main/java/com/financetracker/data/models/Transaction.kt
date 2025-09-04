package com.financetracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.io.Serializable

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = BankAccount::class,
            parentColumns = ["id"],
            childColumns = ["bankAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["bankAccountId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val transactionType: TransactionType, // DEBIT, CREDIT
    val description: String,
    val originalMessage: String, // The raw SMS message
    
    // Three distinct timestamps for different purposes
    val transactionDate: Long?, // Date from SMS text (optional, for reporting)
    val messageReceivedAt: Long, // Actual SMS arrival time (crucial for ordering)
    val lastModifiedAt: Long = System.currentTimeMillis(), // When transaction was last updated
    
    val extractedAt: Long = System.currentTimeMillis(), // When we processed the SMS
    
    // Bank account mapping
    val bankAccountId: Long? = null,
    val extractedBankInfo: String? = null, // Bank info extracted from SMS
    
    // Tagging and categorization
    val isTagged: Boolean = false,
    val category: String? = null,
    val subcategory: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    
    // SMS metadata
    val smsAddress: String? = null,
    val smsTimestamp: Long? = null,
    
    // Transaction details extracted from SMS
    val merchantName: String? = null,
    val transactionId: String? = null,
    val balance: Double? = null,
    
    // Status
    val status: TransactionStatus = TransactionStatus.PENDING,
    val isDeleted: Boolean = false,
    
    val createdAt: Long = System.currentTimeMillis()
    // Note: updatedAt is replaced by lastModifiedAt above
) : Serializable

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class TransactionStatus {
    PENDING,    // Untagged, needs user action
    TAGGED,     // Tagged and categorized
    IGNORED,    // User marked as not necessary
    DELETED     // Soft deleted
}

// Predefined categories for transactions
object TransactionCategories {
    const val FOOD_DINING = "Food & Dining"
    const val INVESTMENT = "Investment"
    const val TRANSFER = "Transfer"
    const val OTHER = "Other"
    
    fun getAllCategories(): List<String> {
        return listOf(
            FOOD_DINING, INVESTMENT, TRANSFER, OTHER
        )
    }
}
