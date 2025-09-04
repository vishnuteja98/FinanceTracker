package com.financetracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountName: String,
    val bankName: String,
    val accountNumber: String? = null,
    val accountType: String, // SAVINGS, CURRENT, CREDIT_CARD, etc.
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Keywords that help identify this account in SMS messages
    val identifierKeywords: List<String> = emptyList()
) : Serializable

enum class AccountType {
    SAVINGS,
    CURRENT,
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    WALLET,
    OTHER
}
