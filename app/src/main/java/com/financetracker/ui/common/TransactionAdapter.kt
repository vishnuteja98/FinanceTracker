package com.financetracker.ui.common

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
import com.financetracker.databinding.ItemPendingTransactionBinding
import com.financetracker.utils.AmountFormatter
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onIgnoreClick: ((Transaction) -> Unit)? = null,
    private val showActions: Boolean = true
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private var bankAccounts: List<BankAccount> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun updateBankAccounts(accounts: List<BankAccount>) {
        bankAccounts = accounts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemPendingTransactionBinding.inflate(
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
        private val binding: ItemPendingTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Transaction details
                textViewDescription.text = transaction.description
                textViewAmount.text = AmountFormatter.formatAmount(root.context, transaction.amount)
                textViewDate.text = dateFormat.format(Date(transaction.transactionDate ?: transaction.messageReceivedAt))
                
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

                // Merchant info
                if (!transaction.merchantName.isNullOrBlank()) {
                    textViewMerchant.text = transaction.merchantName
                    textViewMerchant.visibility = android.view.View.VISIBLE
                } else {
                    textViewMerchant.visibility = android.view.View.GONE
                }

                // Balance info
                if (transaction.balance != null) {
                    textViewBalance.text = "Balance: ${AmountFormatter.formatAmount(root.context, transaction.balance!!)}"
                    textViewBalance.visibility = android.view.View.VISIBLE
                } else {
                    textViewBalance.visibility = android.view.View.GONE
                }

                // Action buttons
                if (showActions) {
                    buttonTag.visibility = android.view.View.VISIBLE
                    buttonIgnore.visibility = android.view.View.VISIBLE
                    
                    buttonTag.setOnClickListener { onTransactionClick(transaction) }
                    buttonIgnore.setOnClickListener { onIgnoreClick?.invoke(transaction) }
                } else {
                    buttonTag.visibility = android.view.View.GONE
                    buttonIgnore.visibility = android.view.View.GONE
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
