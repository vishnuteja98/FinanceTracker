package com.financetracker.ui.accounts

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.financetracker.data.models.AccountType
import com.financetracker.data.models.BankAccount
import com.financetracker.databinding.DialogAddEditBankAccountBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AddEditBankAccountDialog : DialogFragment() {

    private var _binding: DialogAddEditBankAccountBinding? = null
    private val binding get() = _binding!!
    
    private var bankAccount: BankAccount? = null
    private var onSaveListener: ((String, String, String?, String) -> Unit)? = null
    private var saveDialog: Dialog? = null
    
    companion object {
        private const val ARG_BANK_ACCOUNT = "bank_account"
        
        fun newInstance(bankAccount: BankAccount? = null): AddEditBankAccountDialog {
            val fragment = AddEditBankAccountDialog()
            val args = Bundle().apply {
                bankAccount?.let { putSerializable(ARG_BANK_ACCOUNT, it) }
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bankAccount = arguments?.getSerializable(ARG_BANK_ACCOUNT) as? BankAccount
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditBankAccountBinding.inflate(layoutInflater)
        
        setupViews()
        
        val title = if (bankAccount != null) "Edit Bank Account" else "Add Bank Account"
        
        saveDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                // Handle save in saveAccount method
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        // Override the positive button click to handle async save
        saveDialog?.setOnShowListener { dialog ->
            val alertDialog = dialog as androidx.appcompat.app.AlertDialog
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                saveAccount()
            }
        }
        
        return saveDialog!!
    }

    private fun setupViews() {
        // Setup account type dropdown with simplified options
        val accountTypeOptions = mapOf(
            "Bank Account" to AccountType.SAVINGS.name,
            "Credit Card" to AccountType.CREDIT_CARD.name
        )
        
        val displayNames = accountTypeOptions.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames)
        binding.spinnerAccountType.setAdapter(adapter)
        
        // Set up dynamic field updates based on account type selection
        binding.spinnerAccountType.addTextChangedListener { text ->
            val selectedType = accountTypeOptions[text.toString()]
            updateFieldsForAccountType(selectedType)
        }
        
        // Pre-fill if editing
        bankAccount?.let { account ->
            binding.editTextAccountName.setText(account.accountName)
            binding.editTextInstitutionName.setText(account.bankName)
            binding.editTextAccountNumber.setText(account.accountNumber ?: "")
            
            // Find display name for account type
            val displayName = accountTypeOptions.entries.find { it.value == account.accountType }?.key
            displayName?.let { binding.spinnerAccountType.setText(it, false) }
        }
        
        // Set default if new account
        if (bankAccount == null) {
            binding.spinnerAccountType.setText("Bank Account", false)
            updateFieldsForAccountType(AccountType.SAVINGS.name)
        }
    }
    
    private fun updateFieldsForAccountType(accountType: String?) {
        when (accountType) {
            AccountType.CREDIT_CARD.name -> {
                binding.layoutInstitutionName.hint = "Credit Card Company"
                binding.layoutAccountNumber.hint = "Credit Card Number"
                binding.editTextAccountNumber.filters = arrayOf(InputFilter.LengthFilter(19)) // Max for credit cards with spaces
            }
            else -> { // SAVINGS/Bank Account or default
                binding.layoutInstitutionName.hint = "Bank Name"
                binding.layoutAccountNumber.hint = "Account Number"
                binding.editTextAccountNumber.filters = arrayOf(InputFilter.LengthFilter(20)) // Max for bank accounts
            }
        }
    }

    private fun saveAccount() {
        // Show loading state
        showLoadingState(true)
        
        lifecycleScope.launch {
            try {
                val accountName = binding.editTextAccountName.text.toString().trim()
                val institutionName = binding.editTextInstitutionName.text.toString().trim()
                val accountNumber = binding.editTextAccountNumber.text.toString().trim().takeIf { it.isNotEmpty() }
                val selectedDisplayName = binding.spinnerAccountType.text.toString()
                
                // Convert display name back to enum value
                val accountTypeOptions = mapOf(
                    "Bank Account" to AccountType.SAVINGS.name,
                    "Credit Card" to AccountType.CREDIT_CARD.name
                )
                
                val accountType = accountTypeOptions[selectedDisplayName] ?: AccountType.SAVINGS.name
                
                if (accountName.isNotEmpty() && institutionName.isNotEmpty() && selectedDisplayName.isNotEmpty()) {
                    // Call the save operation
                    onSaveListener?.invoke(accountName, institutionName, accountNumber, accountType)
                    
                    // Add a small delay to show the loading indicator
                    kotlinx.coroutines.delay(500)
                    
                    // Hide loading and dismiss dialog
                    showLoadingState(false)
                    dismiss()
                } else {
                    // Hide loading state if validation fails
                    showLoadingState(false)
                }
                
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

    fun setOnSaveListener(listener: (String, String, String?, String) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
