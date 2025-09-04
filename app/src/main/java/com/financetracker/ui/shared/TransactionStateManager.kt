package com.financetracker.ui.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared state manager to coordinate data changes between Pending and Transactions tabs
 */
@Singleton
class TransactionStateManager @Inject constructor() {
    
    // Event to notify when transactions have been updated (tagged, ignored, etc.)
    private val _transactionUpdated = MutableSharedFlow<TransactionUpdateEvent>()
    val transactionUpdated: SharedFlow<TransactionUpdateEvent> = _transactionUpdated
    
    // Event to notify when new transactions are added
    private val _newTransactionAdded = MutableSharedFlow<Unit>()
    val newTransactionAdded: SharedFlow<Unit> = _newTransactionAdded
    
    suspend fun notifyTransactionTagged(transactionId: Long) {
        _transactionUpdated.emit(TransactionUpdateEvent.Tagged(transactionId))
    }
    
    suspend fun notifyTransactionIgnored(transactionId: Long) {
        _transactionUpdated.emit(TransactionUpdateEvent.Ignored(transactionId))
    }
    
    suspend fun notifyTransactionUpdated(transactionId: Long) {
        _transactionUpdated.emit(TransactionUpdateEvent.Updated(transactionId))
    }
    
    suspend fun notifyNewTransactionAdded() {
        _newTransactionAdded.emit(Unit)
    }
}

sealed class TransactionUpdateEvent {
    data class Tagged(val transactionId: Long) : TransactionUpdateEvent()
    data class Ignored(val transactionId: Long) : TransactionUpdateEvent()
    data class Updated(val transactionId: Long) : TransactionUpdateEvent()
}
