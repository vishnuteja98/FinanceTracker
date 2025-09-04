package com.financetracker.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financetracker.data.database.FinanceTrackerDatabase
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SimpleTransactionExtractionWorkerEntryPoint {
    fun getDatabase(): FinanceTrackerDatabase
    fun getTransactionRepository(): TransactionRepository
    fun getBankAccountRepository(): BankAccountRepository
    fun getSmsTransactionParser(): SmsTransactionParser
    fun getSmsTransactionService(): SmsTransactionService
}

class SimpleTransactionExtractionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SimpleTransactionWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.e(TAG, "SIMPLE WORKER STARTED - doWork() called")
            
            val smsBody = inputData.getString(SmsReceiver.KEY_SMS_BODY) ?: return@withContext Result.failure()
            val smsAddress = inputData.getString(SmsReceiver.KEY_SMS_ADDRESS) ?: "Unknown"
            val smsTimestamp = inputData.getLong(SmsReceiver.KEY_SMS_TIMESTAMP, System.currentTimeMillis())

            Log.e(TAG, "Processing SMS: $smsBody")

            // Get Hilt-provided dependencies
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SimpleTransactionExtractionWorkerEntryPoint::class.java
            )
            val transactionRepository = entryPoint.getTransactionRepository()
            val bankAccountRepository = entryPoint.getBankAccountRepository()
            val smsTransactionService = entryPoint.getSmsTransactionService()

            // Parse the SMS to extract transaction details
            val transaction = smsTransactionService.parseTransactionFromSms(smsBody, smsAddress, smsTimestamp)
            
            if (transaction != null) {
                Log.e(TAG, "Transaction parsed successfully: ${transaction.description}")
                
                // Try to match with existing bank account
                val matchingBankAccount = bankAccountRepository.findMatchingBankAccount(transaction.extractedBankInfo)
                
                // Auto-categorize low value transactions (< ₹100)
                val isLowValue = transaction.amount < 100.0
                val finalTransaction = if (matchingBankAccount != null) {
                    val baseTransaction = transaction.copy(bankAccountId = matchingBankAccount.id)
                    if (isLowValue) {
                        baseTransaction.copy(
                            category = "Low Value",
                            isTagged = true,
                            status = com.financetracker.data.models.TransactionStatus.TAGGED
                        )
                    } else {
                        baseTransaction
                    }
                } else {
                    if (isLowValue) {
                        transaction.copy(
                            category = "Low Value",
                            isTagged = true,
                            status = com.financetracker.data.models.TransactionStatus.TAGGED
                        )
                    } else {
                        transaction
                    }
                }
                
                // Save the transaction
                val transactionId = transactionRepository.insertTransaction(finalTransaction)
                Log.e(TAG, "Transaction saved with ID: $transactionId")
                
                // Send broadcast to notify UI of new transaction
                val intent = Intent("com.financetracker.NEW_TRANSACTION")
                intent.putExtra("transactionId", transactionId)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                Log.d(TAG, "Broadcast sent for new transaction ID: $transactionId")
                
                Result.success()
            } else {
                Log.e(TAG, "SMS does not contain transaction information")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            Result.retry()
        }
    }
}