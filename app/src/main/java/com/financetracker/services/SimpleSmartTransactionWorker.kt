package com.financetracker.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financetracker.data.database.FinanceTrackerDatabase
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Non-Hilt version of SmartTransactionExtractionWorker
 * This bypasses the Hilt WorkManager integration issues
 */
class SimpleSmartTransactionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SimpleSmartWorker"
    }

    init {
        Log.d(TAG, "SIMPLE SMART WORKER CREATED - Non-Hilt worker instantiated")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SIMPLE SMART WORKER STARTED - doWork() called")
            
            val smsBody = inputData.getString(SmsReceiver.KEY_SMS_BODY) 
                ?: return@withContext Result.failure()
            val smsAddress = inputData.getString(SmsReceiver.KEY_SMS_ADDRESS) ?: "Unknown"
            val smsTimestamp = inputData.getLong(SmsReceiver.KEY_SMS_TIMESTAMP, System.currentTimeMillis())

            Log.d(TAG, "Processing SMS: $smsBody")

            // Manually create dependencies (bypassing Hilt)
            val database = FinanceTrackerDatabase.getDatabase(applicationContext)
            val transactionRepository = TransactionRepository(database.transactionDao())
            val bankAccountRepository = BankAccountRepository(database.bankAccountDao())
            
            // Create processors manually
            val smsPreprocessor = SmsPreprocessor()
            val geminiCloudProcessor = GeminiCloudProcessor(applicationContext, bankAccountRepository)
            val basicSmsProcessor = BasicSmsProcessor(bankAccountRepository)
            val smsTransactionCoordinator = SmsTransactionCoordinator(smsPreprocessor, geminiCloudProcessor, basicSmsProcessor)
            
            // Log processing capabilities
            val status = smsTransactionCoordinator.getProcessingStatus()
            Log.i(TAG, "Processing capabilities - Gemini Cloud: ${status.geminiCloudAvailable}, Basic: ${status.basicAvailable}")

            // Use coordinator to process the message (GenAI first, then basic fallback)
            val transaction = smsTransactionCoordinator.processMessage(smsBody, smsAddress, smsTimestamp)
            
            if (transaction != null) {
                Log.d(TAG, "Transaction extracted successfully: ${transaction.description}")
                Log.d(TAG, "Transaction already has bankAccountId: ${transaction.bankAccountId}")
                
                // Only try to match if no account was already found by the processors
                val finalTransaction = if (transaction.bankAccountId == null) {
                    Log.d(TAG, "No account found by processors, trying legacy matching...")
                    val matchingBankAccount = bankAccountRepository.findMatchingBankAccount(transaction.extractedBankInfo)
                    if (matchingBankAccount != null) {
                        Log.d(TAG, "Legacy matching found: ID=${matchingBankAccount.id}, Name='${matchingBankAccount.accountName}'")
                        transaction.copy(bankAccountId = matchingBankAccount.id)
                    } else {
                        Log.d(TAG, "No legacy match found")
                        transaction
                    }
                } else {
                    Log.d(TAG, "Using account already found by processors: ${transaction.bankAccountId}")
                    transaction
                }
                
                // Save the transaction
                Log.d(TAG, "Final transaction before save: bankAccountId=${finalTransaction.bankAccountId}, description='${finalTransaction.description}'")
                val transactionId = transactionRepository.insertTransaction(finalTransaction)
                Log.d(TAG, "Transaction saved with ID: $transactionId")
                
                Result.success()
            } else {
                Log.d(TAG, "SMS does not contain transaction information")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            Result.retry()
        }
    }
}
