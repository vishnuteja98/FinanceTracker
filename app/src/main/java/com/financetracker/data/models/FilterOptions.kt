package com.financetracker.data.models

import java.util.*

/**
 * Filter options for transaction lists with user-friendly names
 */
enum class DateFilter(val displayName: String, val days: Int?) {
    TODAY("Today", 0),
    LAST_7_DAYS("Last 7 Days", 7),
    CURRENT_MONTH("This Month", null), // Special handling for current month
    ALL("All Time", null)
}

/**
 * Sort options for transaction lists with user-friendly names
 */
enum class SortOption(val displayName: String, val dbValue: String) {
    MESSAGE_RECEIVED("Latest First", "MESSAGE_RECEIVED"),
    AMOUNT_HIGH_TO_LOW("Amount: High to Low", "AMOUNT_HIGH_TO_LOW"),
    AMOUNT_LOW_TO_HIGH("Amount: Low to High", "AMOUNT_LOW_TO_HIGH"),
    CATEGORY("Category", "CATEGORY"),
    LAST_MODIFIED("Recently Modified", "LAST_MODIFIED")
}

/**
 * Filter and sort configuration for transaction queries
 */
data class TransactionFilter(
    val dateFilter: DateFilter = DateFilter.ALL,
    val sortOption: SortOption = SortOption.MESSAGE_RECEIVED,
    val page: Int = 0,
    val pageSize: Int = 10
) {
    
    /**
     * Get start date timestamp based on filter
     */
    fun getStartDate(): Long? {
        return when (dateFilter) {
            DateFilter.TODAY -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.LAST_7_DAYS -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.CURRENT_MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.ALL -> null
        }
    }
    
    /**
     * Get end date timestamp based on filter
     */
    fun getEndDate(): Long? {
        return when (dateFilter) {
            DateFilter.TODAY -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }
            DateFilter.CURRENT_MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }
            else -> null
        }
    }
    
    /**
     * Get offset for pagination
     */
    fun getOffset(): Int = page * pageSize
}

/**
 * Pagination state for infinite scroll
 */
data class PaginationState(
    val isLoading: Boolean = false,
    val hasMoreData: Boolean = true,
    val currentPage: Int = 0,
    val totalCount: Int = 0,
    val error: String? = null
)
