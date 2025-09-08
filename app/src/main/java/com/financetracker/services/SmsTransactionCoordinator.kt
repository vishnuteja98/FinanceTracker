package com.financetracker.services

import android.util.Log
import com.financetracker.data.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsTransactionCoordinator - Main orchestrator for SMS transaction processing
 * 
 * This class coordinates the entire SMS processing flow:
 * 1. Uses SmsPreprocessor to filter sensitive/OTP messages
 * 2. Checks if Gemini Cloud API is available
 * 3. Routes to appropriate processor (Gemini Cloud or Basic)
 * 4. Handles the extracted transaction data
 */
@Singleton
class SmsTransactionCoordinator @Inject constructor(
    private val smsPreprocessor: SmsPreprocessor,
    private val geminiCloudProcessor: GeminiCloudProcessor,
    private val basicSmsProcessor: BasicSmsProcessor
) {
    
    companion object {
        private const val TAG = "SmsTransactionCoordinator"
    }
    
    /**
     * Main entry point for SMS transaction processing
     * 
     * Flow:
     * 1. Preprocess message to filter sensitive/OTP content
     * 2. Check if Gemini Cloud API is available
     * 3. Route to Gemini Cloud processor or Basic processor
     * 4. Return extracted transaction data
     */
    suspend fun processMessage(
        message: String,
        senderAddress: String,
        timestamp: Long
    ): Transaction? = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Starting SMS processing for message from: $senderAddress")
            
            // Step 1: Preprocess message - filter out sensitive information (OTP, etc.)
            if (!smsPreprocessor.shouldProcessMessage(message, senderAddress)) {
                Log.d(TAG, "Message filtered out by preprocessor")
                return@withContext null
            }
            
            // Step 2: Check if Gemini Cloud API is available
            if (geminiCloudProcessor.isAvailable()) {
                Log.d(TAG, "Gemini Cloud API is available, using cloud AI processing")
                val geminiResult = geminiCloudProcessor.extractTransactionInfo(message, senderAddress, timestamp)
                
                if (geminiResult != null) {
                    Log.i(TAG, "✅ Gemini Cloud API successfully processed transaction")
                    return@withContext geminiResult
                } else {
                    Log.d(TAG, "Gemini Cloud API couldn't process message, trying basic fallback")
                }
            } else {
                Log.d(TAG, "Gemini Cloud API not available, using basic processing")
            }
            
            // Step 3: Fallback to basic regex-based processing
            Log.d(TAG, "Using basic SMS processing")
            val basicResult = basicSmsProcessor.processMessage(message, senderAddress, timestamp)
            
            if (basicResult != null) {
                Log.i(TAG, "✅ Basic processing successfully extracted transaction")
                return@withContext basicResult
            } else {
                Log.d(TAG, "❌ Both Gemini Cloud and basic processing failed")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS transaction processing", e)
            return@withContext null
        }
    }
    
    /**
     * Get processing status for debugging/monitoring
     */
    fun getProcessingStatus(): ProcessingStatus {
        return ProcessingStatus(
            geminiCloudAvailable = geminiCloudProcessor.isAvailable(),
            basicAvailable = true // Always available
        )
    }
    
    data class ProcessingStatus(
        val geminiCloudAvailable: Boolean,
        val basicAvailable: Boolean
    )
}
