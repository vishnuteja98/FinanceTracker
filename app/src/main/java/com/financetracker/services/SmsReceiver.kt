package com.financetracker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        const val KEY_SMS_BODY = "sms_body"
        const val KEY_SMS_ADDRESS = "sms_address"
        const val KEY_SMS_TIMESTAMP = "sms_timestamp"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "SMS RECEIVER TRIGGERED - Action: ${intent.action}")
        
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in smsMessages) {
                val messageBody = smsMessage.messageBody
                val originatingAddress = smsMessage.originatingAddress ?: "Unknown"
                val timestamp = smsMessage.timestampMillis
                
                Log.e(TAG, "Processing SMS from: $originatingAddress")
                Log.e(TAG, "Message: '$messageBody'")
                Log.e(TAG, "Enqueueing WorkManager task...")
                
                // Queue the SMS for processing using WorkManager
                val workRequest = OneTimeWorkRequestBuilder<SimpleTransactionExtractionWorker>()
                    .setInputData(workDataOf(
                        KEY_SMS_BODY to messageBody,
                        KEY_SMS_ADDRESS to originatingAddress,
                        KEY_SMS_TIMESTAMP to timestamp
                    ))
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
