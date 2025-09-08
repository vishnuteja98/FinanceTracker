package com.financetracker.services

import android.util.Log
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionType
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.repository.BankAccountRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BasicSmsProcessor @Inject constructor(
    private val bankAccountRepository: BankAccountRepository
) {
    
    companion object {
        private const val TAG = "BasicSmsProcessor"
        
        // Simple patterns for basic transaction detection
        private val AMOUNT_PATTERNS = listOf(
            Regex("(?:rs\\.?|₹|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
            Regex("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|₹|inr)", RegexOption.IGNORE_CASE)
        )
        
        // Simple debit/credit indicators
        private val DEBIT_KEYWORDS = listOf("debited", "withdrawn", "spent", "paid", "deducted")
        private val CREDIT_KEYWORDS = listOf("credited", "deposited", "received")
        
        // Simple bank patterns
        private val BANK_PATTERNS = listOf(
            Regex("(hdfc|icici|sbi|axis|kotak|pnb|bob)\\s*bank", RegexOption.IGNORE_CASE),
            Regex("-(hdfc|icici|sbi|axis|kotak|pnb|bob)", RegexOption.IGNORE_CASE)
        )
        
        // Account number patterns (last 4 digits)
        private val ACCOUNT_PATTERNS = listOf(
            Regex("xxxx(\\d{4})", RegexOption.IGNORE_CASE),
            Regex("\\*(\\d{4})", RegexOption.IGNORE_CASE),
            Regex("ending\\s+in\\s+(\\d{4})", RegexOption.IGNORE_CASE),
            Regex("a/c\\s+\\*+(\\d{4})", RegexOption.IGNORE_CASE)
        )
        
        // Simple merchant patterns
        private val MERCHANT_PATTERNS = listOf(
            Regex("(?:at|to|for)\\s+([a-zA-Z0-9\\s&.-]+?)(?:\\s+on|\\s+\\d|$)", RegexOption.IGNORE_CASE),
            Regex("(?:purchase|order|payment)\\s+(?:at|to|for)\\s+([a-zA-Z0-9\\s&.-]+?)(?:\\s+on|\\s+\\d|$)", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Simple fallback processing - no GenAI, just basic pattern matching
     */
    suspend fun processMessage(
        message: String,
        senderAddress: String,
        timestamp: Long
    ): Transaction? {
        
        try {
            Log.d(TAG, "Processing SMS with basic rules")
            Log.d(TAG, "Message: $message")
            
            // Extract amount using simple patterns
            val amount = extractAmount(message)
            if (amount == null) {
                Log.d(TAG, "No amount found in message")
                return null
            }
            
            // Determine transaction type
            val transactionType = determineTransactionType(message)
            
            // Extract bank info
            val bankInfo = extractBankInfo(message)
            
            // Extract account last 4 digits
            val accountLastFourDigits = extractAccountLastFourDigits(message)
            
            // Extract merchant name
            val merchantName = extractMerchantName(message)
            
            // Use merchant name as description, fallback to transaction type
            val description = merchantName ?: "Transaction - ${transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
            
            // Try to find matching bank account
            val matchingAccount = findMatchingBankAccount(accountLastFourDigits, bankInfo)
            
            Log.d(TAG, "Final account selection: ${matchingAccount?.let { "ID=${it.id}, Name='${it.accountName}', Bank='${it.bankName}', AccountNumber='${it.accountNumber}'" } ?: "No account matched"}")
            
            return Transaction(
                amount = amount,
                transactionType = transactionType,
                description = description,
                originalMessage = message,
                transactionDate = null,
                messageReceivedAt = timestamp,
                lastModifiedAt = System.currentTimeMillis(),
                extractedAt = System.currentTimeMillis(),
                bankAccountId = matchingAccount?.id,
                extractedBankInfo = bankInfo,
                smsAddress = senderAddress,
                smsTimestamp = timestamp,
                merchantName = merchantName,
                transactionId = null,
                balance = null,
                status = TransactionStatus.PENDING
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in basic processing", e)
            return null
        }
    }
    
    /**
     * Extract amount using simple regex patterns
     */
    private fun extractAmount(message: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(message)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    amountStr.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }
    
    /**
     * Determine transaction type using simple keyword matching
     */
    private fun determineTransactionType(message: String): TransactionType {
        val lowerMessage = message.lowercase()
        
        val hasDebitKeyword = DEBIT_KEYWORDS.any { lowerMessage.contains(it) }
        val hasCreditKeyword = CREDIT_KEYWORDS.any { lowerMessage.contains(it) }
        
        return when {
            hasCreditKeyword -> TransactionType.CREDIT
            hasDebitKeyword -> TransactionType.DEBIT
            else -> TransactionType.DEBIT // Default assumption
        }
    }
    
    /**
     * Extract bank information using simple patterns
     */
    private fun extractBankInfo(message: String): String? {
        for (pattern in BANK_PATTERNS) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].uppercase() + " Bank"
            }
        }
        return null
    }
    
    /**
     * Extract account last 4 digits using simple patterns
     */
    private fun extractAccountLastFourDigits(message: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Extract merchant name using simple patterns
     */
    private fun extractMerchantName(message: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    /**
     * Find matching bank account based on last 4 digits and bank info
     * (Same logic as GeminiCloudProcessor)
     */
    private suspend fun findMatchingBankAccount(lastFourDigits: String?, bankInfo: String?): com.financetracker.data.models.BankAccount? {
        return try {
            // First try to match by last 4 digits if available
            if (!lastFourDigits.isNullOrBlank()) {
                val accountsByDigits = bankAccountRepository.findAccountsByLastFourDigits(lastFourDigits)
                Log.d(TAG, "Found ${accountsByDigits.size} accounts matching last 4 digits: $lastFourDigits")
                
                // Log all matching accounts for debugging
                accountsByDigits.forEachIndexed { index, account ->
                    Log.d(TAG, "  Account $index: ID=${account.id}, Name='${account.accountName}', Bank='${account.bankName}', AccountNumber='${account.accountNumber}'")
                }
                
                // If we have bank info, try to narrow down further
                if (!bankInfo.isNullOrBlank() && accountsByDigits.size > 1) {
                    Log.d(TAG, "Attempting to narrow down by bank info: '$bankInfo'")
                    val filteredByBank = accountsByDigits.filter { account ->
                        account.bankName.contains(bankInfo, ignoreCase = true)
                    }
                    if (filteredByBank.isNotEmpty()) {
                        Log.d(TAG, "Narrowed down to ${filteredByBank.size} accounts by bank info:")
                        filteredByBank.forEachIndexed { index, account ->
                            Log.d(TAG, "  Filtered Account $index: ID=${account.id}, Name='${account.accountName}', Bank='${account.bankName}'")
                        }
                        Log.d(TAG, "Selected account: ID=${filteredByBank.first().id}, Name='${filteredByBank.first().accountName}'")
                        return filteredByBank.first()
                    } else {
                        Log.d(TAG, "No accounts matched bank info '$bankInfo', using first digit match")
                    }
                }
                
                // Return first match if available
                if (accountsByDigits.isNotEmpty()) {
                    val selectedAccount = accountsByDigits.first()
                    Log.d(TAG, "Using first account match: ID=${selectedAccount.id}, Name='${selectedAccount.accountName}', Bank='${selectedAccount.bankName}'")
                    return selectedAccount
                }
            }
            
            // Fallback: try to match by bank info only
            if (!bankInfo.isNullOrBlank()) {
                val accountsByBank = bankAccountRepository.getBankAccountsByBankName(bankInfo)
                if (accountsByBank.isNotEmpty()) {
                    Log.d(TAG, "Found ${accountsByBank.size} accounts by bank name: $bankInfo")
                    return accountsByBank.first()
                }
            }
            
            Log.d(TAG, "No matching bank account found")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding matching bank account", e)
            null
        }
    }
}
