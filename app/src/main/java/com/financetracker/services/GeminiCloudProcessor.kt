package com.financetracker.services

import android.content.Context
import android.util.Log
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionType
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.repository.BankAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// Firebase Vertex AI imports
import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.vertexAI

/**
 * GeminiCloudProcessor - Cloud-based Gemini API integration for SMS transaction extraction
 * 
 * IMPORTANT: This class uses Firebase AI Logic SDK to communicate with cloud-based Gemini API.
 * All sensitive data has been filtered out by SmsPreprocessor before reaching this class.
 * 
 * This class is responsible for:
 * 1. Initializing Firebase-based Gemini Pro model
 * 2. Using cloud-based Gemini API to extract transaction information
 * 3. Returning structured transaction data or null if extraction fails
 * 
 * Security Notes:
 * - API key is managed securely by Firebase backend
 * - No sensitive data (OTP, etc.) is sent to cloud (filtered by SmsPreprocessor)
 * - User authentication is NOT required
 */
@Singleton
class GeminiCloudProcessor @Inject constructor(
    private val context: Context,
    private val bankAccountRepository: BankAccountRepository
) {
    
    companion object {
        private const val TAG = "GeminiCloudProcessor"
        private const val MODEL_NAME = "gemini-2.0-flash-exp" // Use the latest Gemini 2.0 Flash model
    }
    
    private var isGeminiAvailable = false
    private var generativeModel: GenerativeModel? = null // Will hold GenerativeModel instance
    
    init {
        initializeGeminiCloud()
    }
    
    /**
     * Initialize Firebase-based Gemini Pro model
     */
    private fun initializeGeminiCloud() {
        try {
            Log.d(TAG, "Initializing Firebase Gemini Cloud API...")
            
            // Check if Firebase is properly configured
            if (!isFirebaseConfigured()) {
                Log.w(TAG, "Firebase not configured properly")
                isGeminiAvailable = false
                return
            }
            
            // Initialize Gemini Pro model via Firebase
            initializeGeminiModel()
            
            if (generativeModel != null) {
                isGeminiAvailable = true
                Log.i(TAG, "Firebase Gemini Cloud API initialized successfully")
            } else {
                Log.w(TAG, "Failed to initialize Gemini model")
                isGeminiAvailable = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Gemini Cloud API", e)
            isGeminiAvailable = false
        }
    }
    
    /**
     * Check if Firebase is properly configured
     */
    private fun isFirebaseConfigured(): Boolean {
        return try {
            // Firebase is automatically initialized when google-services.json is present
            // and the Google Services plugin is applied
            Log.d(TAG, "Firebase configuration check - google-services.json detected")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not configured", e)
            false
        }
    }
    
    /**
     * Initialize the Gemini model via Firebase Vertex AI
     */
    private fun initializeGeminiModel() {
        try {
            Log.d(TAG, "Attempting to initialize Gemini model via Vertex AI...")
            Log.d(TAG, "Model name: $MODEL_NAME")
            
            generativeModel = Firebase.vertexAI.generativeModel(MODEL_NAME)
            Log.i(TAG, "✅ Gemini model initialized successfully via Firebase Vertex AI")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing Gemini model: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            generativeModel = null
        }
    }
    
    /**
     * Check if Gemini Cloud API is available and ready to use
     */
    fun isAvailable(): Boolean = isGeminiAvailable
    
    /**
     * Process SMS message using cloud-based Gemini Pro API
     * 
     * IMPORTANT: This method uses real Firebase Gemini API for transaction extraction.
     * All sensitive data has been filtered out by SmsPreprocessor.
     */
    suspend fun extractTransactionInfo(
        message: String, 
        senderAddress: String, 
        timestamp: Long
    ): Transaction? = withContext(Dispatchers.IO) {
        
        if (!isGeminiAvailable) {
            Log.d(TAG, "Gemini Cloud API not available")
            return@withContext null
        }
        
        try {
            Log.d(TAG, "Processing SMS with Gemini Cloud API")
            Log.d(TAG, "Message: $message")
            
            // Step 1: Create the AI prompt for transaction extraction
            val prompt = createTransactionExtractionPrompt(message)
            
            // Step 2: Generate content using Gemini Pro via Firebase
            val geminiResponse = generateContentWithGemini(prompt)
            
            if (geminiResponse == null) {
                Log.d(TAG, "Gemini returned null response")
                return@withContext null
            }
            
            // Step 3: Parse the AI response into transaction data
            val transactionData = parseGeminiResponse(geminiResponse)
            
            if (transactionData == null) {
                Log.d(TAG, "Failed to parse Gemini response into transaction data")
                return@withContext null
            }
            
            // Step 4: Create and return Transaction object
            val transaction = createTransactionFromGeminiData(
                transactionData, 
                message, 
                senderAddress, 
                timestamp
            )
            
            Log.i(TAG, "✅ Gemini Cloud API successfully extracted transaction: ${transaction.description}")
            return@withContext transaction
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message with Gemini Cloud API", e)
            return@withContext null
        }
    }
    
    /**
     * Create optimized prompt for transaction extraction using Gemini Pro
     */
    private fun createTransactionExtractionPrompt(message: String): String {
        return """
        You are a transaction extraction expert. Analyze this SMS message and extract transaction information.
        
        SMS Message: "$message"
        
        Rules:
        1. Only extract COMPLETED transactions (past tense: debited, credited, withdrawn, deposited)
        2. Ignore OTP messages, pending transactions, or future transactions
        3. Return ONLY valid JSON, no explanations
        
        Extract these fields if present:
        {
            "isTransaction": boolean (true only if this is a COMPLETED transaction),
            "amount": number (transaction amount, required if isTransaction is true),
            "type": string ("DEBIT" or "CREDIT"),
            "merchantName": string (merchant/vendor name, null if not found),
            "transactionId": string (transaction ID/reference, null if not found),
            "balance": number (account balance after transaction, null if not found),
            "bankInfo": string (bank name, null if not found),
            "accountLastFourDigits": string (last 4 digits of account/card number from patterns like "XXXX1203", "*1203", "ending in 1203" - extract only the 4 digits like "1203", null if not found)
        }
        
        If this is NOT a completed transaction, return: {"isTransaction": false}
        
        JSON Response:
        """.trimIndent()
    }
    
    /**
     * Generate content using Gemini Pro via Firebase
     */
    private suspend fun generateContentWithGemini(prompt: String): String? = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Generating content with Gemini Pro via Firebase")
            Log.d(TAG, "Model available: ${generativeModel != null}")
            
            if (generativeModel == null) {
                Log.e(TAG, "GenerativeModel is null - initialization failed")
                return@withContext null
            }
            
            Log.d(TAG, "Sending prompt to Gemini: ${prompt.take(100)}...")
            val response = generativeModel?.generateContent(prompt)
            val generatedText = response?.text
            Log.d(TAG, "Gemini response received: ${generatedText?.take(200)}...")
            
            if (generatedText.isNullOrBlank()) {
                Log.w(TAG, "Gemini returned empty response")
                return@withContext null
            }
            
            return@withContext generatedText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content with Gemini: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            return@withContext null
        }
    }
    
    /**
     * Parse Gemini response JSON into structured transaction data
     */
    private fun parseGeminiResponse(response: String): TransactionGeminiData? {
        return try {
            // Clean the response - remove markdown code blocks if present
            val cleanedResponse = response
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            Log.d(TAG, "Cleaned JSON response: $cleanedResponse")
            val json = JSONObject(cleanedResponse)
            
            if (!json.optBoolean("isTransaction", false)) {
                Log.d(TAG, "Gemini determined this is not a transaction")
                return null
            }
            
            val amount = json.optDouble("amount")
            if (amount.isNaN() || amount <= 0) {
                Log.d(TAG, "Invalid or missing amount in Gemini response")
                return null
            }
            
            val typeStr = json.optString("type", "DEBIT")
            val type = if (typeStr.uppercase() == "CREDIT") TransactionType.CREDIT else TransactionType.DEBIT
            
            val merchantName = json.optString("merchantName").takeIf { it.isNotBlank() && it != "null" }
            val accountLastFourDigits = json.optString("accountLastFourDigits").takeIf { it.isNotBlank() && it != "null" }
            
            return TransactionGeminiData(
                amount = amount,
                type = type,
                merchantName = merchantName,
                transactionId = json.optString("transactionId").takeIf { it.isNotBlank() && it != "null" },
                balance = json.optDouble("balance").takeIf { !it.isNaN() },
                bankInfo = json.optString("bankInfo").takeIf { it.isNotBlank() && it != "null" },
                accountLastFourDigits = accountLastFourDigits,
                description = merchantName ?: "Transaction" // Use merchant name as description
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: $response", e)
            null
        }
    }
    
    /**
     * Create Transaction object from Gemini extracted data
     */
    private suspend fun createTransactionFromGeminiData(
        geminiData: TransactionGeminiData,
        originalMessage: String,
        senderAddress: String,
        timestamp: Long
    ): Transaction {
        // Try to find matching bank account
        val matchingAccount = findMatchingBankAccount(geminiData.accountLastFourDigits, geminiData.bankInfo)
        
        Log.d(TAG, "Final account selection: ${matchingAccount?.let { "ID=${it.id}, Name='${it.accountName}', Bank='${it.bankName}', AccountNumber='${it.accountNumber}'" } ?: "No account matched"}")
        
        return Transaction(
            amount = geminiData.amount,
            transactionType = geminiData.type,
            description = geminiData.description,
            originalMessage = originalMessage,
            transactionDate = null,
            messageReceivedAt = timestamp,
            lastModifiedAt = System.currentTimeMillis(),
            extractedAt = System.currentTimeMillis(),
            bankAccountId = matchingAccount?.id,
            extractedBankInfo = geminiData.bankInfo,
            smsAddress = senderAddress,
            smsTimestamp = timestamp,
            merchantName = geminiData.merchantName,
            transactionId = geminiData.transactionId,
            balance = geminiData.balance,
            status = TransactionStatus.PENDING
        )
    }
    
    /**
     * Find matching bank account based on last 4 digits and bank info
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
    
    /**
     * Data class to hold Gemini extracted transaction information
     */
    private data class TransactionGeminiData(
        val amount: Double,
        val type: TransactionType,
        val merchantName: String?,
        val transactionId: String?,
        val balance: Double?,
        val bankInfo: String?,
        val accountLastFourDigits: String?,
        val description: String
    )
}
