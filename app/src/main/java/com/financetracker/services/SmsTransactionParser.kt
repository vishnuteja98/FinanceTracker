package com.financetracker.services

import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionType
import com.financetracker.data.models.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTransactionParser @Inject constructor() {

    companion object {
        // Strong transaction indicators - these are highly specific to banking transactions
        private val STRONG_TRANSACTION_KEYWORDS = listOf(
            "debited", "credited", "withdrawn", "deposited", "spent", "payment received",
            "txn", "transaction", "upi", "atm", "available balance", "a/c no", "account",
            "card ending", "deducted from", "received towards", "inb txn"
        )
        
        // OTP/Transaction initiation indicators - these should be IGNORED
        private val OTP_KEYWORDS = listOf(
            "otp", "one time password", "verification code", "auth code", "authentication",
            "valid till", "expires in", "do not share", "don't share", "initiated",
            "authorization", "verify", "confirm transaction", "transaction pending",
            "transaction initiated", "please confirm", "approval required"
        )
        
        // OTP-specific patterns
        private val OTP_PATTERNS = listOf(
            Pattern.compile("otp\\s*(?:is|:)?\\s*[0-9]{4,8}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:code|pin)\\s*(?:is|:)?\\s*[0-9]{4,8}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("valid\\s*(?:till|until|for)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expires?\\s*(?:in|at)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("do\\s*not\\s*share", Pattern.CASE_INSENSITIVE),
            Pattern.compile("don'?t\\s*share", Pattern.CASE_INSENSITIVE),
            Pattern.compile("transaction\\s*(?:initiated|pending)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:confirm|verify|approve)\\s*(?:transaction|payment)", Pattern.CASE_INSENSITIVE)
        )
        
        // Bank-specific patterns that are strong indicators
        private val BANK_SPECIFIC_PATTERNS = listOf(
            Pattern.compile("(?:hdfc|sbi|icici|axis|kotak|pnb|bob|canara|union|indian)\\s*bank", Pattern.CASE_INSENSITIVE),
            Pattern.compile("a/c\\s*no\\.?\\s*[0-9x]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("card\\s*(?:ending|no\\.?)\\s*[0-9x]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("available\\s*(?:balance|limit)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ref\\s*:?\\s*[a-z0-9]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("txn\\s*(?:id|ref)", Pattern.CASE_INSENSITIVE)
        )
        
        // Enhanced amount patterns for various formats
        private val AMOUNT_PATTERNS = listOf(
            // Standard formats: Rs.325.00, ₹1,000, INR 500
            Pattern.compile("(?:rs\\.?|₹|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|₹|inr)", Pattern.CASE_INSENSITIVE),
            
            // Contextual patterns: "deducted Rs.325", "spent Rs.369", "payment of Rs.13471"
            Pattern.compile("(?:deducted|spent|paid|received|credited|debited)\\s*(?:rs\\.?|₹|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:payment|amount|txn)\\s*(?:of|:)?\\s*(?:rs\\.?|₹|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            
            // Balance patterns: "Available balance: Rs.5000"
            Pattern.compile("(?:balance|limit)\\s*(?:is)?\\s*(?:rs\\.?|₹|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        // Transaction type indicators
        private val DEBIT_KEYWORDS = listOf(
            "debited", "withdrawn", "spent", "purchase", "payment", "paid", "debit",
            "emi", "bill", "transferred", "sent", "atm withdrawal"
        )
        
        private val CREDIT_KEYWORDS = listOf(
            "credited", "deposited", "received", "refund", "cashback", "salary",
            "credit", "deposit", "interest", "bonus", "reimbursement"
        )
        
        // Date patterns for extracting transaction dates from SMS
        private val DATE_PATTERNS = listOf(
            // DD-MM-YY, DD-MM-YYYY formats: 15-Jan-24, 20Aug25, 8-8-2025
            Pattern.compile("(\\d{1,2}[-/]\\w{3}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})"),
            Pattern.compile("(\\d{1,2}\\w{3}\\d{2,4})", Pattern.CASE_INSENSITIVE),
            
            // DD/MM/YY format: 20/08/25
            Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{2,4})"),
            
            // Contextual date patterns: "on 15-Jan-24", "On 8-8-2025"
            Pattern.compile("(?:on|at)\\s+(\\d{1,2}[-/]\\w{3}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:on|at)\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:on|at)\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})", Pattern.CASE_INSENSITIVE)
        )
        
        // Bank name patterns
        private val BANK_NAME_PATTERNS = listOf(
            Pattern.compile("(?:from|by|at)\\s+([a-zA-Z\\s]+(?:bank|card))", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z\\s]+bank)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z]+)\\s*(?:credit|debit)\\s*card", Pattern.CASE_INSENSITIVE)
        )
        
        // Account number patterns
        private val ACCOUNT_PATTERNS = listOf(
            Pattern.compile("a/c\\s*:?\\s*([x*0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("account\\s*:?\\s*([x*0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("card\\s*:?\\s*([x*0-9]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Balance patterns
        private val BALANCE_PATTERNS = listOf(
            Pattern.compile("balance\\s*:?\\s*(?:rs\\.?|₹|inr)?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("available\\s*(?:balance)?\\s*:?\\s*(?:rs\\.?|₹|inr)?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
        )
        
        // Transaction ID patterns
        private val TRANSACTION_ID_PATTERNS = listOf(
            Pattern.compile("(?:txn|transaction|ref)\\s*(?:id|no|number)?\\s*:?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("utr\\s*:?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Merchant/Vendor patterns
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("for\\s+([a-zA-Z0-9\\s]+?)(?:\\s*\\.|\\s+avl|\\s+bal|\\s*-|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("at\\s+([a-zA-Z0-9\\s]+)(?:\\s+on|\\s+via|\\.|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:to|from)\\s+([a-zA-Z0-9\\s]+)(?:\\s+on|\\s+via|\\.|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("merchant\\s*:?\\s*([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("towards\\s+([a-zA-Z0-9\\s]+?)(?:\\s*,|\\s*\\.|\\s*-|$)", Pattern.CASE_INSENSITIVE)
        )
    }

    fun parseTransactionFromSms(message: String, senderAddress: String, timestamp: Long): Transaction? {
        // First check if this is likely a banking/transaction SMS
        if (!isTransactionMessage(message)) {
            return null
        }

        val cleanMessage = message.trim().replace("\\s+".toRegex(), " ")
        
        // Extract amount
        val amount = extractAmount(cleanMessage) ?: return null
        
        // Determine transaction type
        val transactionType = determineTransactionType(cleanMessage)
        
        // Extract other details
        val bankInfo = extractBankInfo(cleanMessage)
        val balance = extractBalance(cleanMessage)
        val transactionId = extractTransactionId(cleanMessage)
        val merchantName = extractMerchantName(cleanMessage)
        val accountInfo = extractAccountInfo(cleanMessage)
        val extractedDate = extractTransactionDate(cleanMessage)
        
        return Transaction(
            amount = amount,
            transactionType = transactionType,
            description = generateDescription(cleanMessage, transactionType, merchantName),
            originalMessage = message,
            
            // Three distinct timestamps
            transactionDate = extractedDate, // Date from SMS text (optional, for reporting)
            messageReceivedAt = timestamp, // Actual SMS arrival time (crucial for ordering)
            lastModifiedAt = System.currentTimeMillis(), // When transaction was last updated
            
            extractedAt = System.currentTimeMillis(),
            extractedBankInfo = bankInfo,
            smsAddress = senderAddress,
            smsTimestamp = timestamp,
            merchantName = merchantName,
            transactionId = transactionId,
            balance = balance,
            status = TransactionStatus.PENDING
        )
    }

    private fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // FIRST: Check if this is an OTP/transaction initiation message - REJECT immediately
        val hasOtpKeyword = OTP_KEYWORDS.any { keyword ->
            lowerMessage.contains(keyword.lowercase())
        }
        
        val hasOtpPattern = OTP_PATTERNS.any { pattern ->
            pattern.matcher(message).find()
        }
        
        // If it contains OTP indicators, it's NOT a completed transaction - ignore it
        if (hasOtpKeyword || hasOtpPattern) {
            return false
        }
        
        // Check for strong transaction indicators
        val hasStrongKeyword = STRONG_TRANSACTION_KEYWORDS.any { keyword ->
            lowerMessage.contains(keyword.lowercase())
        }
        
        // Check for bank-specific patterns
        val hasBankPattern = BANK_SPECIFIC_PATTERNS.any { pattern ->
            pattern.matcher(message).find()
        }
        
        // Check for amount pattern
        val hasAmount = AMOUNT_PATTERNS.any { pattern ->
            pattern.matcher(message).find()
        }
        
        // Must have at least one strong indicator AND an amount
        // OR have a bank pattern AND an amount
        return (hasStrongKeyword && hasAmount) || (hasBankPattern && hasAmount)
    }

    private fun extractAmount(message: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                return try {
                    amountStr?.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }

    private fun determineTransactionType(message: String): TransactionType {
        val lowerMessage = message.lowercase()
        
        val debitScore = DEBIT_KEYWORDS.count { lowerMessage.contains(it) }
        val creditScore = CREDIT_KEYWORDS.count { lowerMessage.contains(it) }
        
        return if (creditScore > debitScore) TransactionType.CREDIT else TransactionType.DEBIT
    }

    private fun extractBankInfo(message: String): String? {
        for (pattern in BANK_NAME_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }

    private fun extractTransactionDate(message: String): Long? {
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val dateStr = matcher.group(1)?.trim()
                return parseDate(dateStr)
            }
        }
        return null
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        
        val dateFormats = listOf(
            SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("ddMMMyy", Locale.ENGLISH),
            SimpleDateFormat("ddMMMyyy", Locale.ENGLISH)
        )
        
        for (format in dateFormats) {
            try {
                val date = format.parse(dateStr)
                return date?.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    private fun extractAccountInfo(message: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }

    private fun extractBalance(message: String): Double? {
        for (pattern in BALANCE_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val balanceStr = matcher.group(1)?.replace(",", "")
                return try {
                    balanceStr?.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }

    private fun extractTransactionId(message: String): String? {
        for (pattern in TRANSACTION_ID_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }

    private fun extractMerchantName(message: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                // Filter out common non-merchant words
                if (merchant != null && !merchant.matches(".*\\b(?:bank|card|account|via|upi|paytm)\\b.*".toRegex(RegexOption.IGNORE_CASE))) {
                    return merchant
                }
            }
        }
        return null
    }

    private fun generateDescription(message: String, type: TransactionType, merchantName: String?): String {
        val typeStr = if (type == TransactionType.DEBIT) "Payment" else "Credit"
        return when {
            merchantName != null -> "$typeStr at $merchantName"
            message.contains("atm", ignoreCase = true) -> "ATM ${if (type == TransactionType.DEBIT) "Withdrawal" else "Deposit"}"
            message.contains("transfer", ignoreCase = true) -> "Bank Transfer"
            message.contains("upi", ignoreCase = true) -> "UPI ${if (type == TransactionType.DEBIT) "Payment" else "Receipt"}"
            else -> "$typeStr transaction"
        }
    }

    // Test method to validate parsing with sample SMS messages
    fun testParser(): List<Transaction> {
        val testMessages = listOf(
            // Original test messages
            "Your account XXXX1234 has been debited with Rs.500.00 on 15-Jan-24 at AMAZON via UPI. Available balance: Rs.5000.00. Txn ID: 123456789",
            "Rs.2000 credited to your SBI account XXXX5678 on 15-Jan-24. Ref: SAL123456. Available balance: Rs.7000.00",
            "You have spent Rs.150.00 at CAFE COFFEE DAY using your HDFC card ending 9876 on 15-Jan-24. Available balance: Rs.4850.00",
            
            // New sample messages from user
            "Payment Alert! INR 325.00 deducted from HDFC Bank A/C No 0067 towards STATE BANK OF INDIA, RACPC II, V UMRN: HDFC0000000013365504",
            "Spent Rs.369 On HDFC Bank Card 4946 At No 55 Sy No 8 14 Ground F On 2025-08-18:22:04:15. Not You? To Block+ReissueCall 180012341234/SMS BLOCK CC 4946 to 73080901234",
            "Dear HDFCBANK CardMEMBER, Payment of Rs.13471.00 Received towards your credit card ending with 4946 On 8-8-2025. Your Available limit is Rs. 103612.51",
            "Dear Customer, Thx for INB txn of Rs.500000 from A/c X9309 to Thammana Si.... Ref IR00DSJCX7 on 20Aug25. If not done, fwd this SMS to 740012341234 to block INB or call 180011234-SBI",
            "Rs. 220.00 spent on your SBI Credit Card ending 1624 at KDLOMACS GOLLAPUDU on 20/08/25. Trxn. not done by you? Report here"
        )
        
        return testMessages.mapNotNull { message ->
            parseTransactionFromSms(message, "TEST-BANK", System.currentTimeMillis())
        }
    }
}
