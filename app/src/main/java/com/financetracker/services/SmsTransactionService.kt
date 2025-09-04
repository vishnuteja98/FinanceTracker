package com.financetracker.services

import android.content.Context
import android.util.Log
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionType
import com.financetracker.data.models.TransactionStatus
// Privacy-first approach: No external AI dependencies
// Using enhanced custom parser for all SMS processing
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTransactionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fallbackParser: SmsTransactionParser
) {
    
    companion object {
        private const val TAG = "SmsTransactionService"
        

    }
    
    init {
        Log.i(TAG, "SMS Transaction Service initialized")
                Log.i(TAG, "Device: ${android.os.Build.BRAND} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        Log.i(TAG, "Using custom parser for SMS processing")
    }
    
    suspend fun parseTransactionFromSms(message: String, senderAddress: String, timestamp: Long): Transaction? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Parsing SMS from: $senderAddress")
            return@withContext fallbackParser.parseTransactionFromSms(message, senderAddress, timestamp)
        }
    }
    

    

    

    
    // Test method to validate SMS parsing
    suspend fun testParser(): List<Transaction> {
        val testMessages = listOf(
            "Payment Alert! INR 325.00 deducted from HDFC Bank A/C No 0067 towards STATE BANK OF INDIA",
            "Rs. 220.00 spent on your SBI Credit Card ending 1624 at KDLOMACS GOLLAPUDU on 20/08/25",
            "Dear Customer, Thx for INB txn of Rs.500000 from A/c X9309 to Thammana Si.... Ref IR00DSJCX7",
            "Get 50% off on your next purchase! Use code SAVE50", // Should be filtered out
            "Your OTP is 123456 for login" // Should be filtered out
        )
        
        val results = mutableListOf<Transaction>()
        for (message in testMessages) {
            val transaction = parseTransactionFromSms(message, "TEST-BANK", System.currentTimeMillis())
            if (transaction != null) {
                results.add(transaction)
            }
        }
        return results
    }
}
