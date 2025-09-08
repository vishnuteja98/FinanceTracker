package com.financetracker.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SmartTransactionExtractionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsTransactionCoordinator: SmsTransactionCoordinator,
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SmartTransactionWorker"
    }

    init {
        Log.d(TAG, "SMART WORKER CREATED - SmartTransactionExtractionWorker instantiated")
        
        // Log processing capabilities
        val status = smsTransactionCoordinator.getProcessingStatus()
        Log.i(TAG, "Processing capabilities - Gemini Cloud: ${status.geminiCloudAvailable}, Basic: ${status.basicAvailable}")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SMART WORKER STARTED - doWork() called")
            val smsBody = inputData.getString(SmsReceiver.KEY_SMS_BODY) 
                ?: return@withContext Result.failure()
            val smsAddress = inputData.getString(SmsReceiver.KEY_SMS_ADDRESS) ?: "Unknown"
            val smsTimestamp = inputData.getLong(SmsReceiver.KEY_SMS_TIMESTAMP, System.currentTimeMillis())

            Log.d(TAG, "Processing SMS: $smsBody")

            // Use coordinator to process the message (GenAI first, then basic fallback)
            val transaction = smsTransactionCoordinator.processMessage(smsBody, smsAddress, smsTimestamp)
            
            if (transaction != null) {
                Log.d(TAG, "Transaction extracted successfully: ${transaction.description}")
                
                // Try to match with existing bank account
                val matchingBankAccount = bankAccountRepository.findMatchingBankAccount(transaction.extractedBankInfo)
                val finalTransaction = if (matchingBankAccount != null) {
                    transaction.copy(bankAccountId = matchingBankAccount.id)
                } else {
                    transaction
                }
                
                // Save the transaction
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
