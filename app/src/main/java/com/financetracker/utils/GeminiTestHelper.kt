package com.financetracker.utils

import android.content.Context
import android.util.Log
import com.financetracker.services.GeminiCloudProcessor
import com.financetracker.data.database.FinanceTrackerDatabase
import com.financetracker.data.repository.BankAccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to test Gemini integration
 */
object GeminiTestHelper {
    private const val TAG = "GeminiTestHelper"
    
    /**
     * Test Gemini integration with a sample transaction SMS
     */
    fun testGeminiIntegration(context: Context) {
        Log.d(TAG, "Starting Gemini integration test...")
        
        // Create dependencies manually
        val database = FinanceTrackerDatabase.getDatabase(context)
        val bankAccountRepository = BankAccountRepository(database.bankAccountDao())
        val geminiProcessor = GeminiCloudProcessor(context, bankAccountRepository)
        
        // Test SMS message (safe, no sensitive data)
        val testMessage = "Your account ending in 1234 has been debited with Rs. 500.00 on 2024-01-15. Available balance: Rs. 2500.00. Transaction ID: TXN123456789."
        val testSender = "TEST-BANK"
        val testTimestamp = System.currentTimeMillis()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Testing with message: $testMessage")
                Log.d(TAG, "Gemini available: ${geminiProcessor.isAvailable()}")
                
                val result = geminiProcessor.extractTransactionInfo(
                    testMessage, 
                    testSender, 
                    testTimestamp
                )
                
                if (result != null) {
                    Log.i(TAG, "✅ Gemini test SUCCESSFUL!")
                    Log.i(TAG, "Amount: ${result.amount}")
                    Log.i(TAG, "Type: ${result.transactionType}")
                    Log.i(TAG, "Description: ${result.description}")
                    Log.i(TAG, "Balance: ${result.balance}")
                } else {
                    Log.w(TAG, "⚠️ Gemini test returned null - check Firebase configuration")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gemini test FAILED", e)
            }
        }
    }
}
