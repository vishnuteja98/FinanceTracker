package com.financetracker.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsPreprocessor - Filters out sensitive information from SMS messages
 * 
 * This class is responsible for:
 * 1. Filtering out OTP containing messages
 * 2. Filtering out other sensitive information
 * 3. Basic validation that message could be a transaction
 * 
 * This preprocessing happens BEFORE any transaction extraction logic.
 */
@Singleton
class SmsPreprocessor @Inject constructor() {
    
    companion object {
        private const val TAG = "SmsPreprocessor"
        
        // OTP and sensitive data indicators - filter these out immediately
        private val SENSITIVE_DATA_INDICATORS = listOf(
            "otp", "one time password", "verification code", "auth code", "authentication",
            "valid till", "expires in", "do not share", "don't share", "pin",
            "authorization", "verify", "confirm transaction", "transaction pending",
            "transaction initiated", "please confirm", "approval required",
            "enter otp", "use otp", "verify otp", "temporary password",
            "secure code", "access code", "login code", "passcode",
            "will be debited", "will be credited", "will be deducted", "will be charged",
            "has requested money", "on approving", "approve the request",
            "maintain balance", "ensure sufficient balance", "payment due",
            "mandate", "e-mandate", "auto-debit", "standing instruction",
            "scheduled for", "will be processed on"
        )
        
        // Basic transaction indicators - messages without these are likely not transactions
        private val BASIC_TRANSACTION_INDICATORS = listOf(
            "debited", "credited", "withdrawn", "deposited", "spent", "payment",
            "txn", "transaction", "upi", "atm", "balance", "account", "card",
            "₹", "rs", "inr", "amount", "deducted", "received", "transferred"
        )
        
        // Patterns that suggest OTP or sensitive information
        private val OTP_PATTERNS = listOf(
            Regex("\\b\\d{4,8}\\b.*(?:otp|code|pin|password)", RegexOption.IGNORE_CASE),
            Regex("(?:otp|code|pin).*\\b\\d{4,8}\\b", RegexOption.IGNORE_CASE),
            Regex("\\b\\d{4,8}\\s*(?:is|for).*(?:verification|authentication|login)", RegexOption.IGNORE_CASE),
            Regex("(?:expires?|valid).*\\d+.*(?:min|minutes|hrs|hours)", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Check if message should be processed for transaction extraction
     * Returns true if message is safe to process, false if it should be filtered out
     */
    fun shouldProcessMessage(message: String, senderAddress: String): Boolean {
        Log.d(TAG, "Preprocessing message from: $senderAddress")
        
        // Step 1: Check for sensitive data (OTP, etc.) - immediate rejection
        if (containsSensitiveData(message)) {
            Log.d(TAG, "Message contains sensitive data (OTP/PIN), filtering out")
            return false
        }
        
        // Step 2: Basic filtering - does it look like a transaction at all?
        if (!hasBasicTransactionIndicators(message)) {
            Log.d(TAG, "Message doesn't contain basic transaction indicators, filtering out")
            return false
        }
        
        Log.d(TAG, "Message passed preprocessing, safe to process")
        return true
    }
    
    /**
     * Check if message contains sensitive data like OTP, PIN, etc.
     * CRITICAL: No OTP containing messages should ever be sent for processing
     */
    private fun containsSensitiveData(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Check for sensitive keywords
        val hasSensitiveKeyword = SENSITIVE_DATA_INDICATORS.any { keyword ->
            lowerMessage.contains(keyword)
        }
        
        if (hasSensitiveKeyword) {
            Log.d(TAG, "Found sensitive keyword in message")
            return true
        }
        
        // Check for OTP patterns
        val hasOtpPattern = OTP_PATTERNS.any { pattern ->
            pattern.containsMatchIn(message)
        }
        
        if (hasOtpPattern) {
            Log.d(TAG, "Found OTP pattern in message")
            return true
        }
        
        return false
    }
    
    /**
     * Basic filtering to check if message could be a transaction
     */
    private fun hasBasicTransactionIndicators(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        return BASIC_TRANSACTION_INDICATORS.any { indicator ->
            lowerMessage.contains(indicator)
        }
    }
}
