package com.financetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.data.models.CategorySpending
import com.financetracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _categorySpending = MutableStateFlow<List<CategorySpending>>(emptyList())
    val categorySpending: StateFlow<List<CategorySpending>> = _categorySpending.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.ALL_TIME)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    init {
        loadCategorySpending()
    }

    fun setTimeFilter(timeFilter: TimeFilter, customStartDate: Long? = null, customEndDate: Long? = null) {
        _selectedTimeFilter.value = timeFilter
        loadCategorySpending(timeFilter, customStartDate, customEndDate)
    }

    private fun loadCategorySpending(
        timeFilter: TimeFilter = TimeFilter.ALL_TIME,
        customStartDate: Long? = null,
        customEndDate: Long? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (startDate, endDate) = getDateRange(timeFilter, customStartDate, customEndDate)
                val spending = transactionRepository.getCategoryWiseSpending(startDate, endDate)
                _categorySpending.value = spending
            } catch (e: Exception) {
                // Handle error - could emit error state
                _categorySpending.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getDateRange(
        timeFilter: TimeFilter,
        customStartDate: Long? = null,
        customEndDate: Long? = null
    ): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()

        return when (timeFilter) {
            TimeFilter.ALL_TIME -> Pair(null, null)
            
            TimeFilter.LAST_7_DAYS -> {
                calendar.timeInMillis = currentTime
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                Pair(calendar.timeInMillis, currentTime)
            }
            
            TimeFilter.CURRENT_MONTH -> {
                calendar.timeInMillis = currentTime
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis
                
                Pair(startOfMonth, endOfMonth)
            }
            
            TimeFilter.LAST_MONTH -> {
                calendar.timeInMillis = currentTime
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfLastMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfLastMonth = calendar.timeInMillis
                
                Pair(startOfLastMonth, endOfLastMonth)
            }
            
            TimeFilter.CUSTOM -> {
                Pair(customStartDate, customEndDate)
            }
        }
    }
}

enum class TimeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    LAST_7_DAYS("Last 7 Days"),
    CURRENT_MONTH("Current Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Range")
}
