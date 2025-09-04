package com.financetracker.ui.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.R
import com.financetracker.data.models.BankAccount
import com.financetracker.databinding.ItemBankAccountBinding

class BankAccountAdapter(
    private val onAccountClick: (BankAccount) -> Unit,
    private val onToggleActiveClick: (BankAccount) -> Unit
) : ListAdapter<BankAccount, BankAccountAdapter.BankAccountViewHolder>(BankAccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankAccountViewHolder {
        val binding = ItemBankAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BankAccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BankAccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BankAccountViewHolder(
        private val binding: ItemBankAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: BankAccount) {
            binding.apply {
                textViewAccountName.text = account.accountName
                textViewBankName.text = account.bankName
                
                // Convert account type to user-friendly display name
                textViewAccountType.text = getDisplayNameForAccountType(account.accountType)
                
                if (!account.accountNumber.isNullOrBlank()) {
                    val displayNumber = when (account.accountType) {
                        "CREDIT_CARD" -> "****${account.accountNumber.takeLast(4)}"
                        else -> "****${account.accountNumber.takeLast(4)}" // Bank accounts
                    }
                    textViewAccountNumber.text = displayNumber
                    textViewAccountNumber.visibility = android.view.View.VISIBLE
                } else {
                    textViewAccountNumber.visibility = android.view.View.GONE
                }

                // Active/Inactive styling
                if (account.isActive) {
                    root.alpha = 1.0f
                    buttonToggleActive.text = "Deactivate"
                    buttonToggleActive.setTextColor(
                        ContextCompat.getColor(root.context, R.color.debit_red)
                    )
                    textViewStatus.text = "Active"
                    textViewStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.credit_green)
                    )
                } else {
                    root.alpha = 0.6f
                    buttonToggleActive.text = "Activate"
                    buttonToggleActive.setTextColor(
                        ContextCompat.getColor(root.context, R.color.credit_green)
                    )
                    textViewStatus.text = "Inactive"
                    textViewStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.debit_red)
                    )
                }

                // Click listeners
                root.setOnClickListener { onAccountClick(account) }
                buttonToggleActive.setOnClickListener { onToggleActiveClick(account) }
            }
        }
        
        private fun getDisplayNameForAccountType(accountType: String): String {
            return when (accountType) {
                "SAVINGS" -> "Bank Account"
                "CREDIT_CARD" -> "Credit Card"
                else -> accountType
            }
        }
    }

    private class BankAccountDiffCallback : DiffUtil.ItemCallback<BankAccount>() {
        override fun areItemsTheSame(oldItem: BankAccount, newItem: BankAccount): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BankAccount, newItem: BankAccount): Boolean {
            return oldItem == newItem
        }
    }
}
