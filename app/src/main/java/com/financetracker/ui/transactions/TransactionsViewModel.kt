package com.financetracker.ui.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionFilter
import com.financetracker.data.models.DateFilter
import com.financetracker.data.models.SortOption
import com.financetracker.data.models.PaginationState
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import com.financetracker.ui.shared.TransactionStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val transactionStateManager: TransactionStateManager
) : ViewModel() {

        companion object {
        private const val TAG = "TransactionsVM"
    }

    // Filter and pagination state
    private val _currentFilter = MutableStateFlow(TransactionFilter())
    val currentFilter: StateFlow<TransactionFilter> = _currentFilter
    
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions

    private val refreshTrigger = MutableStateFlow(0L)

    init {
        // Initialize total count after all properties are initialized
        updateTotalCount()
        
        // Listen for transaction updates from other tabs
        viewModelScope.launch {
            transactionStateManager.transactionUpdated.collect { event ->
                Log.d(TAG, "Received transaction update event: $event")
                // Only refresh if we have data loaded (preserve state when no data)
                if (_allTransactions.value.isNotEmpty()) {
                    Log.d(TAG, "Refreshing transactions due to update from other tab")
                    refreshTransactions()
                } else {
                    Log.d(TAG, "Skipping refresh - no data loaded yet")
                }
            }
        }
    }

    val bankAccounts = bankAccountRepository.getAllActiveBankAccounts().asLiveData()
    
    // Enhanced transactions with filtering and pagination
    val taggedTransactions = _allTransactions.asLiveData()
    
    // Keep the old grouped transactions for compatibility
    val transactionsByAccount = combine(
        refreshTrigger.debounce(100),
        _currentFilter
    ) { trigger, filter ->
        filter
    }.flatMapLatest { filter ->
        transactionRepository.getTaggedTransactionsFiltered(filter)
    }.map { transactions: List<Transaction> ->
        // Group transactions by bank account
        transactions.groupBy { transaction: Transaction ->
            bankAccounts.value?.find { it.id == transaction.bankAccountId }?.let { account ->
                "${account.bankName} - ${account.accountName}"
            } ?: "Unknown Account"
        }
    }.asLiveData()

    fun getAllTransactions(callback: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            transactionRepository.getTaggedTransactionsFiltered(_currentFilter.value).collect { transactions ->
                callback(transactions)
            }
        }
    }
    
    fun updateTransaction(
        transactionId: Long,
        description: String,
        amount: Double,
        category: String?,
        subcategory: String?,
        notes: String?,
        bankAccountId: Long?
    ) {
        viewModelScope.launch {
            // Get the current transaction and update it
            val transaction = transactionRepository.getTransactionById(transactionId)
            if (transaction != null) {
                val updatedTransaction = transaction.copy(
                    description = description,
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    notes = notes,
                    bankAccountId = bankAccountId,
                    lastModifiedAt = System.currentTimeMillis()
                )
                transactionRepository.updateTransaction(updatedTransaction)
            }
        }
    }
    
        // Filter and sorting methods
    fun updateDateFilter(dateFilter: DateFilter) {
        val newFilter = _currentFilter.value.copy(dateFilter = dateFilter, page = 0)
        _currentFilter.value = newFilter
        resetPagination()
        updateTotalCount()
        loadInitialPage()
        Log.d(TAG, "Date filter updated to: ${dateFilter.displayName}")
    }

    fun updateSortOption(sortOption: SortOption) {
        val newFilter = _currentFilter.value.copy(sortOption = sortOption, page = 0)
        _currentFilter.value = newFilter
        resetPagination()
        updateTotalCount()
        loadInitialPage()
        Log.d(TAG, "Sort option updated to: ${sortOption.displayName}")
    }
    
    // Pagination methods
    private fun loadInitialPage() {
        viewModelScope.launch {
            try {
                val initialFilter = _currentFilter.value.copy(page = 0)
                val transactions = transactionRepository.getTaggedTransactionsFiltered(initialFilter)
                
                val newTransactions = transactions.first()
                _allTransactions.value = newTransactions
                
                // Update pagination state with hasMoreData flag
                val totalCount = transactionRepository.getTaggedTransactionCount(initialFilter)
                val hasMoreData = newTransactions.size < totalCount
                _paginationState.value = _paginationState.value.copy(
                    hasMoreData = hasMoreData,
                    totalCount = totalCount,
                    currentPage = 0
                )
                
                Log.d(TAG, "Loaded initial page with ${newTransactions.size} transactions, total: $totalCount, hasMore: $hasMoreData")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial page", e)
            }
        }
    }
    
    fun loadNextPage() {
        val currentState = _paginationState.value
        val currentFilter = _currentFilter.value
        Log.d(TAG, "=== loadNextPage() CALLED ===")
        Log.d(TAG, "Current state - isLoading: ${currentState.isLoading}, hasMoreData: ${currentState.hasMoreData}, currentPage: ${currentState.currentPage}")
        Log.d(TAG, "Current filter - page: ${currentFilter.page}, pageSize: ${currentFilter.pageSize}")
        Log.d(TAG, "Current transactions count: ${_allTransactions.value.size}")
        
        if (currentState.isLoading || !currentState.hasMoreData) {
            Log.d(TAG, "=== loadNextPage() SKIPPED === isLoading: ${currentState.isLoading}, hasMoreData: ${currentState.hasMoreData}")
            return
        }
        
        Log.d(TAG, "=== loadNextPage() PROCEEDING ===")
        _paginationState.value = _paginationState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val nextPage = currentFilter.page + 1
                val nextFilter = currentFilter.copy(page = nextPage)
                Log.d(TAG, "Fetching page $nextPage with pageSize ${nextFilter.pageSize}")
                
                // Get the next page of transactions
                val newTransactions = transactionRepository.getTaggedTransactionsFiltered(nextFilter).first()
                Log.d(TAG, "Repository returned ${newTransactions.size} new transactions")
                
                // Append new transactions to existing ones
                val currentTransactions = _allTransactions.value
                val updatedTransactions = currentTransactions + newTransactions
                Log.d(TAG, "Before update: ${currentTransactions.size}, After update: ${updatedTransactions.size}")
                _allTransactions.value = updatedTransactions
                
                // Get total count to check if we have more data
                val totalCount = transactionRepository.getTaggedTransactionCount(nextFilter)
                val hasMoreData = updatedTransactions.size < totalCount
                Log.d(TAG, "Total available: $totalCount, Loaded: ${updatedTransactions.size}, HasMore: $hasMoreData")
                
                _currentFilter.value = nextFilter
                _paginationState.value = _paginationState.value.copy(
                    isLoading = false,
                    hasMoreData = hasMoreData,
                    currentPage = nextPage,
                    totalCount = totalCount
                )
                
                Log.d(TAG, "=== loadNextPage() COMPLETED === Page: $nextPage, New: ${newTransactions.size}, Total: ${updatedTransactions.size}/$totalCount, HasMore: $hasMoreData")
            } catch (e: Exception) {
                _paginationState.value = _paginationState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                Log.e(TAG, "=== loadNextPage() ERROR ===", e)
            }
        }
    }
    
        fun refreshTransactions() {
        Log.d(TAG, "Manual refresh triggered")
        refreshTrigger.value = System.currentTimeMillis()
        resetPagination()
        updateTotalCount()
        loadInitialPage()
    }

    private fun resetPagination() {
        _paginationState.value = PaginationState()
        _allTransactions.value = emptyList()
        // Reset the current filter page to 0
        _currentFilter.value = _currentFilter.value.copy(page = 0)
        Log.d(TAG, "Pagination reset - page back to 0")
    }

    private fun updateTotalCount() {
        viewModelScope.launch {
            try {
                val totalCount = transactionRepository.getTaggedTransactionCount(_currentFilter.value)
                _paginationState.value = _paginationState.value.copy(totalCount = totalCount)
                Log.d(TAG, "Total count updated to: $totalCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating total count", e)
            }
        }
    }

    fun clearError() {
        _paginationState.value = _paginationState.value.copy(error = null)
    }
    
    // Public method to initialize data - call this from Fragment
    fun initializeData() {
        val currentTransactions = _allTransactions.value
        val currentState = _paginationState.value
        Log.d(TAG, "=== initializeData() CALLED ===")
        Log.d(TAG, "Current transactions: ${currentTransactions.size}")
        Log.d(TAG, "Current state: isLoading=${currentState.isLoading}, hasMoreData=${currentState.hasMoreData}, totalCount=${currentState.totalCount}")
        
        if (currentTransactions.isEmpty()) {
            Log.d(TAG, "=== initializeData() LOADING INITIAL PAGE ===")
            loadInitialPage()
        } else {
            Log.d(TAG, "=== initializeData() SKIPPED - DATA ALREADY EXISTS ===")
        }
    }
    
    fun getAllBankAccounts(callback: (List<com.financetracker.data.models.BankAccount>) -> Unit) {
        viewModelScope.launch {
            try {
                val accounts = bankAccountRepository.getAllBankAccounts().first()
                callback(accounts)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting bank accounts", e)
                callback(emptyList())
            }
        }
    }
    
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding new transaction: $transaction")
                transactionRepository.insertTransaction(transaction)
                
                // Refresh the transactions list to include the new transaction
                refreshTransactions()
                
                // Notify state manager about the new transaction
                transactionStateManager.notifyTransactionTagged(transaction.id)
                
                Log.d(TAG, "Successfully added new transaction")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding transaction", e)
            }
        }
    }
}
