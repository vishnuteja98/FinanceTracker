package com.financetracker.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.financetracker.data.repository.BankAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionTagViewModel @Inject constructor(
    bankAccountRepository: BankAccountRepository
) : ViewModel() {
    
    val bankAccounts = bankAccountRepository.getAllActiveBankAccounts().asLiveData()
}
