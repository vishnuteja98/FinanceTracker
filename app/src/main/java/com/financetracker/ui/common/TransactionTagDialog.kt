package com.financetracker.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.financetracker.R
import com.financetracker.data.models.BankAccount
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionCategories
import com.financetracker.databinding.DialogTransactionTagBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionTagDialog : DialogFragment() {

    private var _binding: DialogTransactionTagBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TransactionTagViewModel by viewModels()
    private lateinit var transaction: Transaction
    private var onTagCompleteListener: ((Long, String, Double, String?, String?, String?, Long?) -> Unit)? = null
    private var saveDialog: Dialog? = null
    
    companion object {
        private const val ARG_TRANSACTION = "transaction"
        
        fun newInstance(transaction: Transaction): TransactionTagDialog {
            val fragment = TransactionTagDialog()
            val args = Bundle().apply {
                putSerializable(ARG_TRANSACTION, transaction)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transaction = arguments?.getSerializable(ARG_TRANSACTION) as Transaction
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTransactionTagBinding.inflate(layoutInflater)
        
        setupViews()
        observeViewModel()
        
        saveDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Categorize Transaction")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                // Handle save in saveTransaction method
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        // Override the positive button click to handle async save
        saveDialog?.setOnShowListener { dialog ->
            val alertDialog = dialog as androidx.appcompat.app.AlertDialog
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                saveTransaction()
            }
        }
        
        return saveDialog!!
    }

    private fun setupViews() {
        binding.apply {
            // Set editable transaction header
            editTextTransactionHeader.setText(transaction.description)
            
            // Set editable amount (format without unnecessary decimals)
            val amountText = if (transaction.amount % 1.0 == 0.0) {
                transaction.amount.toInt().toString()
            } else {
                transaction.amount.toString()
            }
            editTextAmount.setText(amountText)
            
            // Setup category dropdown with existing categories + option to add new
            setupCategoryDropdown()
            
            // Pre-fill if transaction already has category
            if (!transaction.category.isNullOrBlank()) {
                spinnerCategory.setText(transaction.category, false)
            }
            
            // Pre-fill notes
            editTextNotes.setText(transaction.notes ?: "")
            
            // Setup original message toggle
            setupOriginalMessageView()
        }
    }
    
    private fun setupCategoryDropdown() {
        val existingCategories = TransactionCategories.getAllCategories().toMutableList()
        existingCategories.add("+ Add New Category")
        
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, existingCategories)
        binding.spinnerCategory.setAdapter(categoryAdapter)
        
        // Handle new category creation
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = existingCategories[position]
            if (selectedItem == "+ Add New Category") {
                showAddCategoryDialog()
            }
        }
        
        // Set default text if no category selected
        if (transaction.category.isNullOrBlank()) {
            binding.spinnerCategory.setText("Select Category", false)
        }
    }
    
    private fun setupOriginalMessageView() {
        binding.textViewOriginalMessage.text = transaction.originalMessage
        
        // Original message is hidden by default
        binding.textViewOriginalMessage.visibility = View.GONE
        binding.textViewShowMessage.text = "📱 View Original SMS Message"
        
        binding.textViewShowMessage.setOnClickListener {
            val isVisible = binding.textViewOriginalMessage.visibility == View.VISIBLE
            binding.textViewOriginalMessage.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.textViewShowMessage.text = if (isVisible) "📱 View Original SMS Message" else "📱 Hide Original SMS Message"
        }
    }
    
    private fun showAddCategoryDialog() {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "Enter new category name"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Category")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val newCategory = editText.text.toString().trim()
                if (newCategory.isNotEmpty()) {
                    binding.spinnerCategory.setText(newCategory, false)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.spinnerCategory.setText("", false)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.bankAccounts.observe(this) { bankAccounts ->
            setupBankAccountSpinner(bankAccounts)
        }
    }

    private fun setupBankAccountSpinner(bankAccounts: List<BankAccount>) {
        val accountDisplayNames = mutableListOf<String>()
        accountDisplayNames.add("Select Account")
        
        // Add active bank accounts
        val activeBankAccounts = bankAccounts.filter { it.isActive }
        activeBankAccounts.forEach { account ->
            val displayName = "${account.bankName} - ${account.accountName}"
            accountDisplayNames.add(displayName)
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountDisplayNames)
        binding.spinnerBankAccount.setAdapter(adapter)
        
        // Pre-select current bank account if available
        if (transaction.bankAccountId != null) {
            val currentAccount = activeBankAccounts.find { it.id == transaction.bankAccountId }
            if (currentAccount != null) {
                val accountText = "${currentAccount.bankName} - ${currentAccount.accountName}"
                binding.spinnerBankAccount.setText(accountText, false)
            }
        } else {
            binding.spinnerBankAccount.setText("Select Account", false)
        }
    }

    private fun saveTransaction() {
        // Show loading state
        showLoadingState(true)
        
        lifecycleScope.launch {
            try {
                val updatedDescription = binding.editTextTransactionHeader.text.toString().trim()
                val updatedAmount = binding.editTextAmount.text.toString().toDoubleOrNull() ?: transaction.amount
                val selectedCategory = binding.spinnerCategory.text.toString().takeIf { 
                    it.isNotEmpty() && it != "+ Add New Category" && it != "Select Category"
                }
                
                val notes = binding.editTextNotes.text.toString().trim().takeIf { it.isNotEmpty() }
                
                val selectedBankAccountText = binding.spinnerBankAccount.text.toString()
                val selectedBankAccountId = if (selectedBankAccountText != "Select Account" && selectedBankAccountText.isNotEmpty()) {
                    // Find the bank account by matching the display text
                    viewModel.bankAccounts.value?.filter { it.isActive }?.find { 
                        "${it.bankName} - ${it.accountName}" == selectedBankAccountText 
                    }?.id
                } else null
                
                // Call the save operation
                onTagCompleteListener?.invoke(
                    transaction.id,
                    updatedDescription,
                    updatedAmount,
                    selectedCategory,
                    null, // subcategory - removed for now
                    notes,
                    selectedBankAccountId
                )
                
                // Add a small delay to show the loading indicator
                kotlinx.coroutines.delay(500)
                
                // Hide loading and dismiss dialog
                showLoadingState(false)
                dismiss()
                
            } catch (e: Exception) {
                // Hide loading state on error
                showLoadingState(false)
                // Could show error message here
            }
        }
    }
    
    private fun showLoadingState(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        // Disable dialog buttons
        saveDialog?.let { dialog ->
            val alertDialog = dialog as androidx.appcompat.app.AlertDialog
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = !isLoading
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = !isLoading
        }
    }

    fun setOnTagCompleteListener(listener: (Long, String, Double, String?, String?, String?, Long?) -> Unit) {
        onTagCompleteListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
