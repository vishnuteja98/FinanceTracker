package com.financetracker.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.financetracker.data.models.BankAccount
import com.financetracker.data.repository.BankAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankAccountsViewModel @Inject constructor(
    private val bankAccountRepository: BankAccountRepository
) : ViewModel() {

    val bankAccounts = bankAccountRepository.getAllBankAccounts().asLiveData()

    fun addBankAccount(accountName: String, bankName: String, accountNumber: String?, accountType: String) {
        viewModelScope.launch {
            val account = BankAccount(
                accountName = accountName,
                bankName = bankName,
                accountNumber = accountNumber,
                accountType = accountType,
                isActive = true
            )
            bankAccountRepository.insertBankAccount(account)
        }
    }

    fun updateBankAccount(id: Long, accountName: String, bankName: String, accountNumber: String?, accountType: String) {
        viewModelScope.launch {
            val existingAccount = bankAccountRepository.getBankAccountById(id)
            if (existingAccount != null) {
                val updatedAccount = existingAccount.copy(
                    accountName = accountName,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    accountType = accountType,
                    updatedAt = System.currentTimeMillis()
                )
                bankAccountRepository.updateBankAccount(updatedAccount)
            }
        }
    }

    fun deactivateAccount(id: Long) {
        viewModelScope.launch {
            bankAccountRepository.deactivateBankAccount(id)
        }
    }

    fun activateAccount(id: Long) {
        viewModelScope.launch {
            bankAccountRepository.activateBankAccount(id)
        }
    }
}
