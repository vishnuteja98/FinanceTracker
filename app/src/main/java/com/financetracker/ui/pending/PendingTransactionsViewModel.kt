package com.financetracker.ui.pending

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.financetracker.data.models.TransactionStatus
import com.financetracker.data.models.TransactionFilter
import com.financetracker.data.models.DateFilter
import com.financetracker.data.models.SortOption
import com.financetracker.data.models.PaginationState
import com.financetracker.data.models.Transaction
import com.financetracker.data.repository.BankAccountRepository
import com.financetracker.data.repository.TransactionRepository
import com.financetracker.ui.shared.TransactionStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val transactionStateManager: TransactionStateManager
) : ViewModel() {

    companion object {
        private const val TAG = "PendingTransactionsVM"
    }

    // Filter and pagination state
    private val _currentFilter = MutableStateFlow(TransactionFilter())
    val currentFilter: StateFlow<TransactionFilter> = _currentFilter
    
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions

    private val refreshTrigger = MutableStateFlow(0L)
    
    // Combined flow for filtered and paginated transactions
    val pendingTransactions = _allTransactions.asLiveData()

    init {
        // Initialize total count after all properties are initialized
        updateTotalCount()
    }
    
    val bankAccounts = bankAccountRepository.getAllActiveBankAccounts().asLiveData()

    fun tagTransaction(
        transactionId: Long,
        description: String,
        amount: Double,
        category: String?,
        subcategory: String?,
        notes: String?,
        bankAccountId: Long?
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== tagTransaction() CALLED === ID: $transactionId")
                
                // Get the current transaction and update it
                val transaction = transactionRepository.getTransactionById(transactionId)
                if (transaction != null) {
                    Log.d(TAG, "Found transaction: ${transaction.description}, current status: ${transaction.status}")
                    
                    val updatedTransaction = transaction.copy(
                        description = description,
                        amount = amount,
                        category = category,
                        subcategory = subcategory,
                        notes = notes,
                        bankAccountId = bankAccountId,
                        isTagged = true,
                        status = com.financetracker.data.models.TransactionStatus.TAGGED,
                        lastModifiedAt = System.currentTimeMillis()
                    )
                    
                    Log.d(TAG, "Updating transaction to TAGGED status")
                    transactionRepository.updateTransaction(updatedTransaction)
                    
                    Log.d(TAG, "Transaction updated, notifying state manager and refreshing UI data")
                    // Notify other tabs about the change
                    transactionStateManager.notifyTransactionTagged(transactionId)
                    
                    // Refresh the UI data to reflect the change
                    refreshTransactions()
                    
                } else {
                    Log.e(TAG, "Transaction with ID $transactionId not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error tagging transaction", e)
            }
        }
    }

    fun ignoreTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.updateTransactionStatus(transactionId, TransactionStatus.IGNORED)
            // Notify other tabs about the change
            transactionStateManager.notifyTransactionIgnored(transactionId)
            // Refresh the UI data to reflect the change
            refreshTransactions()
        }
    }

    fun refreshTransactions() {
        Log.d(TAG, "Manual refresh triggered")
        refreshTrigger.value = System.currentTimeMillis()
        resetPagination()
        updateTotalCount()
        loadInitialPage()
    }
    
    // Auto-refresh method that can be called when new transactions are added
    fun autoRefresh() {
        Log.d(TAG, "Auto refresh triggered")
        refreshTrigger.value = System.currentTimeMillis()
        resetPagination()
        updateTotalCount()
        loadInitialPage()
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
                val transactions = transactionRepository.getPendingTransactionsFiltered(initialFilter)
                
                val newTransactions = transactions.first()
                _allTransactions.value = newTransactions
                
                // Update pagination state with hasMoreData flag
                val totalCount = transactionRepository.getPendingTransactionCount(initialFilter)
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
                val newTransactions = transactionRepository.getPendingTransactionsFiltered(nextFilter).first()
                Log.d(TAG, "Repository returned ${newTransactions.size} new transactions")
                
                // Append new transactions to existing ones
                val currentTransactions = _allTransactions.value
                val updatedTransactions = currentTransactions + newTransactions
                Log.d(TAG, "Before update: ${currentTransactions.size}, After update: ${updatedTransactions.size}")
                _allTransactions.value = updatedTransactions
                
                // Get total count to check if we have more data
                val totalCount = transactionRepository.getPendingTransactionCount(nextFilter)
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
                val totalCount = transactionRepository.getPendingTransactionCount(_currentFilter.value)
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
}
