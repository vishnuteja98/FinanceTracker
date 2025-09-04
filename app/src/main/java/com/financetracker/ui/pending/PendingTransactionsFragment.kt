package com.financetracker.ui.pending

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.financetracker.R
import kotlin.math.abs
import com.financetracker.data.models.Transaction
import com.financetracker.databinding.FragmentPendingTransactionsBinding
import com.financetracker.ui.common.TransactionAdapter
import com.financetracker.ui.common.TransactionTagDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PendingTransactionsFragment : Fragment() {

    companion object {
        private const val TAG = "PendingTransactionsFragment"
    }

    private var _binding: FragmentPendingTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var gestureDetector: GestureDetector
    
    private val newTransactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transactionId = intent?.getLongExtra("transactionId", -1L) ?: -1L
            Log.d(TAG, "Received new transaction broadcast, ID: $transactionId")
            viewModel.autoRefresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupGestureDetector()
        observeViewModel()
        registerBroadcastReceiver()
        
        // Initialize data on first load
        viewModel.initializeData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                showTagDialog(transaction)
            },
            onIgnoreClick = { transaction ->
                viewModel.ignoreTransaction(transaction.id)
            }
        )

        binding.recyclerViewPendingTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    val paginationState = viewModel.paginationState.value
                    
                    // Calculate positions
                    val lastVisiblePosition = firstVisibleItemPosition + visibleItemCount - 1
                    
                    // Load more when we're near the end (within 2 items) or at the very bottom
                    val threshold = 2 // Load when 2 items away from end
                    val isNearEnd = lastVisiblePosition >= totalItemCount - threshold - 1
                    val isAtBottom = !recyclerView.canScrollVertically(1)
                    
                    val shouldLoadMore = !paginationState.isLoading && 
                        paginationState.hasMoreData &&
                        totalItemCount > 0 && // Make sure we have items
                        (isNearEnd || isAtBottom) // Either near end or at bottom
                    
                    Log.d(TAG, "SCROLL: visible=$visibleItemCount, total=$totalItemCount, first=$firstVisibleItemPosition, last=$lastVisiblePosition")
                    Log.d(TAG, "SCROLL STATE: isNearEnd=$isNearEnd, isAtBottom=$isAtBottom, dy=$dy")
                    Log.d(TAG, "PAGINATION: isLoading=${paginationState.isLoading}, hasMore=${paginationState.hasMoreData}, shouldLoad=$shouldLoadMore")
                    
                    if (shouldLoadMore) {
                        Log.d(TAG, "🚀 TRIGGERING loadNextPage() - User scrolled near/to end!")
                        viewModel.loadNextPage()
                    }
                }
            })
        }

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe refresh triggered")
            viewModel.refreshTransactions()
            // Scroll to top when refreshing
            binding.recyclerViewPendingTransactions.scrollToPosition(0)
        }
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                if (abs(diffX) > abs(diffY) && 
                    abs(diffX) > SWIPE_THRESHOLD && 
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    
                    if (diffX > 0) {
                        // Swipe right - go to Accounts tab (previous)
                        findNavController().navigate(R.id.navigation_accounts)
                    } else {
                        // Swipe left - go to Transactions tab (next)
                        findNavController().navigate(R.id.navigation_transactions)
                    }
                    return true
                }
                return false
            }
        })
        
        // Set touch listener on the main container
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Don't consume the event, let RecyclerView handle it too
        }
    }

    private fun observeViewModel() {
        viewModel.pendingTransactions.observe(viewLifecycleOwner) { transactions ->
            Log.d(TAG, "Received ${transactions.size} pending transactions")
            
            // Use submitList with callback for better performance
            transactionAdapter.submitList(transactions) {
                // Stop refresh animation after list is updated
                binding.swipeRefreshLayout.isRefreshing = false
                Log.d(TAG, "Transaction list updated, refresh animation stopped")
            }
            
            if (transactions.isEmpty()) {
                binding.recyclerViewPendingTransactions.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.textViewEmptyState.text = getString(R.string.message_no_pending_transactions)
            } else {
                binding.recyclerViewPendingTransactions.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
            }
        }

        viewModel.bankAccounts.observe(viewLifecycleOwner) { bankAccounts ->
            // Update adapter with bank accounts for display
            transactionAdapter.updateBankAccounts(bankAccounts)
        }

        // Observe pagination state
        lifecycleScope.launch {
            viewModel.paginationState.collect { paginationState ->
                // No loading icon - just handle refresh and errors
                binding.swipeRefreshLayout.isRefreshing = false
                
                paginationState.error?.let { _ ->
                    // Handle error (could show a snackbar)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun showTagDialog(transaction: Transaction) {
        val dialog = TransactionTagDialog.newInstance(transaction)
        dialog.setOnTagCompleteListener { transactionId, description, amount, category, subcategory, notes, bankAccountId ->
            viewModel.tagTransaction(transactionId, description, amount, category, subcategory, notes, bankAccountId)
        }
        dialog.show(childFragmentManager, "TagDialog")
    }



    private fun registerBroadcastReceiver() {
        val filter = IntentFilter("com.financetracker.NEW_TRANSACTION")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(newTransactionReceiver, filter)
        Log.d(TAG, "Registered broadcast receiver for new transactions")
    }
    
    private fun unregisterBroadcastReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(newTransactionReceiver)
        Log.d(TAG, "Unregistered broadcast receiver")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterBroadcastReceiver()
        _binding = null
    }
}
