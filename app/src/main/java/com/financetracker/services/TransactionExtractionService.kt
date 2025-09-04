package com.financetracker.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionExtractionService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val smsTransactionParser: SmsTransactionParser
) {

    companion object {
        private const val TAG = "TransactionExtractionService"
        private const val SMS_URI = "content://sms/inbox"
        private const val DAYS_TO_SCAN = 30 // Scan last 30 days of SMS
    }

    suspend fun extractTransactionsFromExistingSms(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Check SMS permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(SecurityException("SMS permission not granted"))
            }

            var processedCount = 0
            val cutoffTime = System.currentTimeMillis() - (DAYS_TO_SCAN * 24 * 60 * 60 * 1000L)

            val cursor: Cursor? = context.contentResolver.query(
                Uri.parse(SMS_URI),
                arrayOf("_id", "address", "body", "date", "type"),
                "date > ? AND type = 1", // type = 1 means inbox messages
                arrayOf(cutoffTime.toString()),
                "date DESC"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex("address")
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    try {
                        val address = it.getString(addressIndex) ?: "Unknown"
                        val body = it.getString(bodyIndex) ?: continue
                        val date = it.getLong(dateIndex)

                        // Parse transaction
                        val transaction = smsTransactionParser.parseTransactionFromSms(body, address, date)
                        
                        if (transaction != null) {
                            // Try to match with existing bank account
                            val matchingBankAccount = bankAccountRepository.findMatchingBankAccount(transaction.extractedBankInfo)
                            val finalTransaction = if (matchingBankAccount != null) {
                                transaction.copy(bankAccountId = matchingBankAccount.id)
                            } else {
                                transaction
                            }
                            
                            transactionRepository.insertTransaction(finalTransaction)
                            processedCount++
                            
                            Log.d(TAG, "Processed transaction: ${transaction.description}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing SMS", e)
                    }
                }
            }

            Log.i(TAG, "Processed $processedCount transactions from existing SMS")
            Result.success(processedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting transactions from SMS", e)
            Result.failure(e)
        }
    }

    suspend fun reprocessPendingTransactions() = withContext(Dispatchers.IO) {
        try {
            val pendingTransactions = transactionRepository.getPendingTransactions()
            
            pendingTransactions.collect { transactions ->
                transactions.forEach { transaction ->
                    if (transaction.bankAccountId == null && !transaction.extractedBankInfo.isNullOrBlank()) {
                        val matchingBankAccount = bankAccountRepository.findMatchingBankAccount(transaction.extractedBankInfo)
                        if (matchingBankAccount != null) {
                            transactionRepository.updateTransactionBankAccount(transaction.id, matchingBankAccount.id)
                            Log.d(TAG, "Updated bank account for transaction: ${transaction.id}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reprocessing pending transactions", e)
        }
    }
}
