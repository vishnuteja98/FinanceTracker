package com.financetracker.data.models

/**
 * Data class for category-wise spending analytics
 */
data class CategorySpending(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int
)
