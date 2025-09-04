package com.financetracker.ui.common

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.financetracker.R
import com.financetracker.data.models.BankAccount
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.models.TransactionType
import com.financetracker.databinding.DialogAddTransactionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialog(
    private val bankAccounts: List<BankAccount>,
    private val onTransactionAddedListener: (Transaction) -> Unit
) : DialogFragment() {
    
    companion object {
        private const val TAG = "AddTransactionDialog"
        
        fun newInstance(
            bankAccounts: List<BankAccount>,
            onTransactionAddedListener: (Transaction) -> Unit
        ): AddTransactionDialog {
            return AddTransactionDialog(bankAccounts, onTransactionAddedListener)
        }
    }

    private lateinit var binding: DialogAddTransactionBinding
    private var selectedDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Categories
    private val categories = arrayOf(
        "Food & Dining", "Shopping", "Transportation", "Bills & Utilities",
        "Entertainment", "Healthcare", "Travel", "Education", "Investment",
        "Transfer", "ATM Withdrawal", "Salary", "Other"
    )
    
    // Transaction types
    private val transactionTypes = arrayOf("DEBIT", "CREDIT")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddTransactionBinding.inflate(layoutInflater)
        
        setupUI()
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Transaction")
            .setView(binding.root)
            .setPositiveButton("Save", null) // We'll override this
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
            
        // Override positive button to handle async operation
        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                lifecycleScope.launch {
                    addTransaction()
                }
            }
        }
        
        return dialog
    }
    
    private fun setupUI() {
        setupTransactionTypeSpinner()
        setupCategorySpinner()
        setupBankAccountSpinner()
        setupDatePicker()
        
        // Set default date to today
        binding.editTextDate.setText(dateFormat.format(selectedDate.time))
    }
    
    private fun setupTransactionTypeSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transactionTypes)
        binding.spinnerTransactionType.setAdapter(adapter)
        binding.spinnerTransactionType.setText(transactionTypes[0], false) // Default to DEBIT
    }
    
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
    }
    
    private fun setupBankAccountSpinner() {
        val accountNames = bankAccounts.map { "${it.bankName} - ${it.accountNumber}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames)
        binding.spinnerBankAccount.setAdapter(adapter)
        
        if (bankAccounts.isNotEmpty()) {
            binding.spinnerBankAccount.setText(accountNames[0], false)
        }
    }
    
    private fun setupDatePicker() {
        binding.editTextDate.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editTextDate.setText(dateFormat.format(selectedDate.time))
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun showLoadingState(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) 
            android.view.View.VISIBLE else android.view.View.GONE
            
        // Disable/enable input fields
        binding.contentLayout.alpha = if (isLoading) 0.5f else 1.0f
        binding.editTextTransactionHeader.isEnabled = !isLoading
        binding.editTextAmount.isEnabled = !isLoading
        binding.spinnerTransactionType.isEnabled = !isLoading
        binding.spinnerCategory.isEnabled = !isLoading
        binding.spinnerBankAccount.isEnabled = !isLoading
        binding.editTextDate.isEnabled = !isLoading
        binding.editTextNotes.isEnabled = !isLoading
        
        // Disable dialog buttons
        (dialog as? androidx.appcompat.app.AlertDialog)?.let { alertDialog ->
            alertDialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = !isLoading
            alertDialog.getButton(Dialog.BUTTON_NEGATIVE).isEnabled = !isLoading
        }
    }
    
    private suspend fun addTransaction() {
        if (!validateInputs()) {
            return
        }
        
        showLoadingState(true)
        
        try {
            val transaction = createTransactionFromInputs()
            Log.d(TAG, "Adding new transaction: $transaction")
            
            // Simulate some processing time
            delay(500)
            
            onTransactionAddedListener(transaction)
            
            showLoadingState(false)
            dismiss()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding transaction", e)
            showLoadingState(false)
            // TODO: Show error message to user
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (binding.editTextTransactionHeader.text.isNullOrBlank()) {
            binding.editTextTransactionHeader.error = "Description is required"
            isValid = false
        }
        
        if (binding.editTextAmount.text.isNullOrBlank()) {
            binding.editTextAmount.error = "Amount is required"
            isValid = false
        } else {
            try {
                binding.editTextAmount.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                binding.editTextAmount.error = "Invalid amount"
                isValid = false
            }
        }
        
        if (binding.spinnerCategory.text.isNullOrBlank()) {
            binding.spinnerCategory.error = "Category is required"
            isValid = false
        }
        
        if (binding.spinnerBankAccount.text.isNullOrBlank()) {
            binding.spinnerBankAccount.error = "Bank account is required"
            isValid = false
        }
        
        return isValid
    }
    
    private fun createTransactionFromInputs(): Transaction {
        val description = binding.editTextTransactionHeader.text.toString().trim()
        val amount = binding.editTextAmount.text.toString().toDouble()
        val transactionTypeText = binding.spinnerTransactionType.text.toString()
        val transactionType = if (transactionTypeText == "CREDIT") TransactionType.CREDIT else TransactionType.DEBIT
        val category = binding.spinnerCategory.text.toString()
        val notes = binding.editTextNotes.text.toString().trim().ifEmpty { null }
        
        // Find selected bank account
        val selectedAccountText = binding.spinnerBankAccount.text.toString()
        val selectedAccount = bankAccounts.find { 
            "${it.bankName} - ${it.accountNumber}" == selectedAccountText 
        } ?: bankAccounts.first()
        
        val currentTimeMillis = System.currentTimeMillis()
        
        return Transaction(
            id = 0, // Will be auto-generated by Room
            amount = amount,
            transactionType = transactionType,
            description = description,
            originalMessage = "Manual Entry", // Mark as manual entry
            transactionDate = selectedDate.timeInMillis,
            messageReceivedAt = currentTimeMillis,
            lastModifiedAt = currentTimeMillis,
            extractedAt = currentTimeMillis,
            bankAccountId = selectedAccount.id,
            isTagged = true,
            category = category,
            notes = notes,
            status = TransactionStatus.TAGGED, // Directly tagged since it's manual
            createdAt = currentTimeMillis
        )
    }
}
