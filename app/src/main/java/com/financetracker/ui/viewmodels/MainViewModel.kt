package com.financetracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository
) : ViewModel() {

    val pendingTransactionCount = transactionRepository.getPendingTransactionCount().asLiveData()

    fun initializeDefaultBankAccounts() {
        viewModelScope.launch {
            bankAccountRepository.createDefaultBankAccounts()
        }
    }


}
