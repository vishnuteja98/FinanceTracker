package com.financetracker.utils

import android.content.Context
import com.financetracker.R

object AmountFormatter {
    
    /**
     * Formats amount to display without unnecessary decimals
     * - Shows whole numbers without decimals (e.g., ₹500 instead of ₹500.00)
     * - Shows decimals only when they exist (e.g., ₹500.50)
     */
    fun formatAmount(context: Context, amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            // Whole number - show without decimals
            "₹${amount.toInt()}"
        } else {
            // Has decimals - show with decimals
            context.getString(R.string.amount_format, amount)
        }
    }
}
