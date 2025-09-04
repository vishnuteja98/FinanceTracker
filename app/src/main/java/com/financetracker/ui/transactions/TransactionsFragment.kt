package com.financetracker.ui.transactions

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.financetracker.R
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.DateFilter
import com.financetracker.data.models.SortOption
import com.financetracker.databinding.FragmentTransactionsBinding
import com.financetracker.ui.common.TransactionTagDialog
import com.financetracker.ui.common.AddTransactionDialog
import com.financetracker.utils.CsvExporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment(), MenuProvider {

    companion object {
        private const val TAG = "TransactionsFragment"
    }

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var gestureDetector: GestureDetector
    
    @Inject
    lateinit var csvExporter: CsvExporter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterUI()
        setupFAB()
        setupGestureDetector()
        observeViewModel()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        // Initialize data on first load
        viewModel.initializeData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                showEditDialog(transaction)
            }
        )

        binding.recyclerViewTransactions.apply {
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
        
        // Setup swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshTransactions()
            // Scroll to top when refreshing
            binding.recyclerViewTransactions.scrollToPosition(0)
        }
    }
    
    private fun setupFilterUI() {
        // Date filter button
        binding.root.findViewById<View>(R.id.btn_date_filter).setOnClickListener { view ->
            showDateFilterMenu(view)
        }
        
        // Sort option button
        binding.root.findViewById<View>(R.id.btn_sort_option).setOnClickListener { view ->
            showSortOptionsMenu(view)
        }
    }
    
    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
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
                        // Swipe right - go to Pending tab (previous)
                        findNavController().navigate(R.id.navigation_pending)
                    } else {
                        // Swipe left - go to Accounts tab (next)
                        findNavController().navigate(R.id.navigation_accounts)
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
        // Observe the paginated transactions (same as PendingTransactionsFragment)
        viewModel.taggedTransactions.observe(viewLifecycleOwner) { transactions: List<Transaction> ->
            Log.d(TAG, "Received ${transactions.size} transactions from ViewModel")
            
            if (transactions.isEmpty()) {
                binding.recyclerViewTransactions.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.textViewEmptyState.text = getString(R.string.message_no_transactions)
            } else {
                binding.recyclerViewTransactions.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
                
                transactionAdapter.submitList(transactions)
            }
        }

        viewModel.bankAccounts.observe(viewLifecycleOwner) { bankAccounts ->
            transactionAdapter.updateBankAccounts(bankAccounts)
        }
        
        // Observe current filter to update button text
        lifecycleScope.launch {
            viewModel.currentFilter.collect { filter ->
                // Update button text to show current selection
                binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_date_filter).text = 
                    filter.dateFilter.displayName
                binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_sort_option).text = 
                    filter.sortOption.displayName
                    
                // Update accessibility descriptions
                binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_date_filter).contentDescription = 
                    "Filter by date: ${filter.dateFilter.displayName}"
                binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_sort_option).contentDescription = 
                    "Sort by: ${filter.sortOption.displayName}"
            }
        }
        
        // Observe pagination state
        lifecycleScope.launch {
            viewModel.paginationState.collect { paginationState ->
                // No loading icon - just handle refresh and other UI updates
                binding.swipeRefreshLayout.isRefreshing = false
                
                // Update result count
                val resultCount = if (paginationState.totalCount > 0) {
                    "${paginationState.totalCount} transactions"
                } else {
                    "0 transactions"
                }
                binding.root.findViewById<android.widget.TextView>(R.id.tv_result_count).text = resultCount
                
                // Hide "all caught up" message to match pending tab behavior
                binding.textViewAllCaughtUp.visibility = View.GONE
                
                paginationState.error?.let { _ ->
                    // Handle error (could show a snackbar)
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun showDateFilterMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        
        DateFilter.values().forEach { filter ->
            val menuItem = popup.menu.add(filter.displayName)
            menuItem.setOnMenuItemClickListener {
                viewModel.updateDateFilter(filter)
                // Scroll to top when filter changes
                binding.recyclerViewTransactions.scrollToPosition(0)
                true
            }
        }
        
        popup.show()
    }
    
    private fun showSortOptionsMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        
        SortOption.values().forEach { option ->
            val menuItem = popup.menu.add(option.displayName)
            menuItem.setOnMenuItemClickListener {
                viewModel.updateSortOption(option)
                // Scroll to top when sort changes
                binding.recyclerViewTransactions.scrollToPosition(0)
                true
            }
        }
        
        popup.show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_transactions, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_export -> {
                exportTransactions()
                true
            }
            else -> false
        }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialog = TransactionTagDialog.newInstance(transaction)
        dialog.setOnTagCompleteListener { transactionId, description, amount, category, subcategory, notes, bankAccountId ->
            viewModel.updateTransaction(transactionId, description, amount, category, subcategory, notes, bankAccountId)
        }
        dialog.show(childFragmentManager, "EditDialog")
    }
    
    private fun showAddTransactionDialog() {
        // Get bank accounts from ViewModel
        viewModel.getAllBankAccounts { bankAccounts ->
            if (bankAccounts.isEmpty()) {
                // Show message to add bank account first
                // TODO: Navigate to accounts tab or show alert
                return@getAllBankAccounts
            }
            
            val dialog = AddTransactionDialog.newInstance(bankAccounts) { transaction ->
                viewModel.addTransaction(transaction)
            }
            dialog.show(childFragmentManager, "AddTransactionDialog")
        }
    }

    private fun exportTransactions() {
        viewModel.getAllTransactions { transactions ->
            if (transactions.isNotEmpty()) {
                lifecycleScope.launch {
                    csvExporter.exportTransactions(requireContext(), transactions)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
