package com.financetracker.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.financetracker.data.models.Transaction
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    suspend fun exportTransactions(context: Context, transactions: List<Transaction>) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "finance_tracker_export_${fileNameFormat.format(Date())}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                FileWriter(file).use { fileWriter ->
                    CSVWriter(fileWriter).use { csvWriter ->
                        // Write header
                        val header = arrayOf(
                            "ID",
                            "Date",
                            "Type",
                            "Amount",
                            "Description",
                            "Category",
                            "Subcategory",
                            "Notes",
                            "Bank Account",
                            "Merchant",
                            "Transaction ID",
                            "Balance",
                            "Status",
                            "Original Message"
                        )
                        csvWriter.writeNext(header)
                        
                        // Write transaction data
                        transactions.forEach { transaction ->
                            val row = arrayOf(
                                transaction.id.toString(),
                                dateFormat.format(Date(transaction.transactionDate ?: transaction.messageReceivedAt)),
                                transaction.transactionType.name,
                                transaction.amount.toString(),
                                transaction.description,
                                transaction.category ?: "",
                                transaction.subcategory ?: "",
                                transaction.notes ?: "",
                                transaction.extractedBankInfo ?: "",
                                transaction.merchantName ?: "",
                                transaction.transactionId ?: "",
                                transaction.balance?.toString() ?: "",
                                transaction.status.name,
                                transaction.originalMessage
                            )
                            csvWriter.writeNext(row)
                        }
                    }
                }
                
                // Share the file
                withContext(Dispatchers.Main) {
                    shareFile(context, file)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error exporting transactions: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Finance Tracker Export")
                putExtra(Intent.EXTRA_TEXT, "Exported transactions from Finance Tracker")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Share CSV File")
            context.startActivity(chooser)
            
            Toast.makeText(context, "Transactions exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
