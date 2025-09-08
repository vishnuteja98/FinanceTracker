package com.financetracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.R
import com.financetracker.data.models.BankAccount
import com.financetracker.data.models.Transaction
import com.financetracker.data.models.TransactionType
import com.financetracker.databinding.ItemTransactionBinding
import com.financetracker.utils.AmountFormatter
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private var bankAccounts: List<BankAccount> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun updateBankAccounts(accounts: List<BankAccount>) {
        bankAccounts = accounts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Transaction details
                textViewDescription.text = transaction.description
                textViewAmount.text = AmountFormatter.formatAmount(root.context, transaction.amount)
                textViewDate.text = dateFormat.format(Date(transaction.transactionDate ?: transaction.messageReceivedAt))
                
                // Category with dynamic styling
                val categoryText = transaction.category ?: "Uncategorized"
                textViewCategory.text = categoryText
                
                // Set category background based on category type with vibrant colors
                val categoryBackground = when {
                    categoryText.contains("Food", ignoreCase = true) || 
                    categoryText.contains("Dining", ignoreCase = true) -> R.drawable.bg_category_food
                    categoryText.contains("Transfer", ignoreCase = true) -> R.drawable.bg_category_transfer
                    categoryText.contains("Investment", ignoreCase = true) -> R.drawable.bg_category_investment
                    categoryText.contains("Other", ignoreCase = true) -> R.drawable.bg_category_default
                    else -> R.drawable.bg_category_default
                }
                textViewCategory.setBackgroundResource(categoryBackground)
                
                // Set white text for better contrast on colored backgrounds
                textViewCategory.setTextColor(
                    ContextCompat.getColor(root.context, android.R.color.white)
                )
                
                // Transaction type styling
                when (transaction.transactionType) {
                    TransactionType.DEBIT -> {
                        textViewAmount.setTextColor(
                            ContextCompat.getColor(root.context, R.color.debit_red)
                        )
                        textViewTransactionType.text = root.context.getString(R.string.transaction_debit)
                        textViewTransactionType.setTextColor(
                            ContextCompat.getColor(root.context, R.color.debit_red)
                        )
                    }
                    TransactionType.CREDIT -> {
                        textViewAmount.setTextColor(
                            ContextCompat.getColor(root.context, R.color.credit_green)
                        )
                        textViewTransactionType.text = root.context.getString(R.string.transaction_credit)
                        textViewTransactionType.setTextColor(
                            ContextCompat.getColor(root.context, R.color.credit_green)
                        )
                    }
                }

                // Bank account info
                val bankAccount = bankAccounts.find { it.id == transaction.bankAccountId }
                textViewBankAccount.text = when {
                    bankAccount != null -> bankAccount.accountName
                    !transaction.extractedBankInfo.isNullOrBlank() -> transaction.extractedBankInfo
                    else -> "Unknown Account"
                }

                // Click listener for the entire item
                root.setOnClickListener { onTransactionClick(transaction) }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
