package com.financetracker.utils

import com.financetracker.data.models.BankAccount
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.models.TransactionType
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestDataHelper @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val transactionRepository: TransactionRepository
) {
    
    suspend fun createTestData() {
        // Create test bank accounts
        val testAccounts = listOf(
            BankAccount(
                accountName = "Chase Checking",
                bankName = "Chase Bank",
                accountNumber = "****1234",
                accountType = "CHECKING",
                identifierKeywords = listOf("chase", "checking", "1234")
            ),
            BankAccount(
                accountName = "Wells Fargo Savings",
                bankName = "Wells Fargo",
                accountNumber = "****5678",
                accountType = "SAVINGS",
                identifierKeywords = listOf("wells", "fargo", "savings", "5678")
            ),
            BankAccount(
                accountName = "Citi Credit Card",
                bankName = "Citibank",
                accountNumber = "****9012",
                accountType = "CREDIT_CARD",
                identifierKeywords = listOf("citi", "credit", "card", "9012")
            )
        )
        
        // Insert test accounts
        val accountIds = mutableListOf<Long>()
        testAccounts.forEach { account ->
            val id = bankAccountRepository.insertBankAccount(account)
            accountIds.add(id)
        }
        
        // Create test transactions
        val currentTime = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        
        val testTransactions = listOf(
            // 2 Pending transactions
            Transaction(
                originalMessage = "Sent rs. 1000 From HDFC Bank A/C *0067 To CRED Club On 30/08/25 Ref 5601234",
                amount = 1000.0,
                transactionType = TransactionType.DEBIT,
                description = "Payment to CRED Club",
                merchantName = "CRED Club",
                extractedBankInfo = "HDFC Bank A/C *0067",
                transactionDate = currentTime - dayInMillis,
                messageReceivedAt = currentTime - dayInMillis,
                status = TransactionStatus.PENDING,
                bankAccountId = accountIds.getOrNull(0)
            ),
            Transaction(
                originalMessage = "Dear customer, Your a/c no. XXXX9309 is credited by 5000 on 29/08/25 by salary transfer (IMPS Ref No 524211345).",
                amount = 5000.0,
                transactionType = TransactionType.CREDIT,
                description = "Salary Credit",
                merchantName = "Salary Transfer",
                extractedBankInfo = "a/c no. XXXX9309",
                transactionDate = currentTime - (2 * dayInMillis),
                messageReceivedAt = currentTime - (2 * dayInMillis),
                status = TransactionStatus.PENDING,
                bankAccountId = accountIds.getOrNull(1)
            ),
            
            // 3 Categorized transactions
            Transaction(
                originalMessage = "Your card ending 5678 used for Rs.300 at STARBUCKS on 28/08/25. Avl limit Rs.45000",
                amount = 300.0,
                transactionType = TransactionType.DEBIT,
                description = "Coffee at Starbucks",
                merchantName = "STARBUCKS",
                extractedBankInfo = "card ending 5678",
                transactionDate = currentTime - (3 * dayInMillis),
                messageReceivedAt = currentTime - (3 * dayInMillis),
                status = TransactionStatus.TAGGED,
                isTagged = true,
                category = "Food & Dining",
                notes = "Morning coffee",
                bankAccountId = accountIds.getOrNull(2)
            ),
            Transaction(
                originalMessage = "Rs.2000 debited from A/C *0067 for SIP-HDFC Fund on 27/08/25. Balance: Rs.1,25,000",
                amount = 2000.0,
                transactionType = TransactionType.DEBIT,
                description = "SIP Investment - HDFC Fund",
                merchantName = "HDFC Fund",
                extractedBankInfo = "A/C *0067",
                transactionDate = currentTime - (4 * dayInMillis),
                messageReceivedAt = currentTime - (4 * dayInMillis),
                status = TransactionStatus.TAGGED,
                isTagged = true,
                category = "Investment",
                notes = "Monthly SIP",
                bankAccountId = accountIds.getOrNull(0)
            ),
            Transaction(
                originalMessage = "Rs.800 transferred to FRIEND via UPI on 26/08/25. Ref: 524299999",
                amount = 800.0,
                transactionType = TransactionType.DEBIT,
                description = "Money Transfer to Friend",
                merchantName = "FRIEND",
                extractedBankInfo = "UPI",
                transactionDate = currentTime - (5 * dayInMillis),
                messageReceivedAt = currentTime - (5 * dayInMillis),
                status = TransactionStatus.TAGGED,
                isTagged = true,
                category = "Transfer",
                notes = "Dinner split",
                bankAccountId = accountIds.getOrNull(1)
            )
        )
        
        // Insert test transactions
        transactionRepository.insertTransactions(testTransactions)
    }
    
    suspend fun clearTestData() {
        // Note: This would require additional DAO methods to clear all data
        // For now, we'll just create the test data
    }
}
