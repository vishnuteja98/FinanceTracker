package com.financetracker.ui.analytics

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.financetracker.R
import com.financetracker.databinding.FragmentAnalyticsBinding
import com.financetracker.utils.AmountFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    private var customStartDate: Long? = null
    private var customEndDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPieChart()
        setupTimeFilters()
        observeData()
    }

    private fun setupPieChart() {
        binding.pieChartCategories.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            dragDecelerationFrictionCoef = 0.95f
            
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            
            holeRadius = 58f
            transparentCircleRadius = 61f
            
            setDrawCenterText(true)
            centerText = "Spending\nBreakdown"
            
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.xEntrySpace = 7f
            legend.yEntrySpace = 0f
            legend.yOffset = 0f
        }
    }

    private fun setupTimeFilters() {
        binding.chipGroupTimeFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipAllTime -> {
                        hideCustomDateRange()
                        viewModel.setTimeFilter(TimeFilter.ALL_TIME)
                    }
                    R.id.chipLast7Days -> {
                        hideCustomDateRange()
                        viewModel.setTimeFilter(TimeFilter.LAST_7_DAYS)
                    }
                    R.id.chipCurrentMonth -> {
                        hideCustomDateRange()
                        viewModel.setTimeFilter(TimeFilter.CURRENT_MONTH)
                    }
                    R.id.chipLastMonth -> {
                        hideCustomDateRange()
                        viewModel.setTimeFilter(TimeFilter.LAST_MONTH)
                    }
                    R.id.chipCustom -> {
                        showCustomDateRange()
                    }
                }
            }
        }
        
        // Setup custom date pickers
        binding.editTextStartDate.setOnClickListener {
            showDatePicker { date ->
                customStartDate = date
                binding.editTextStartDate.setText(dateFormat.format(Date(date)))
                updateCustomFilter()
            }
        }
        
        binding.editTextEndDate.setOnClickListener {
            showDatePicker { date ->
                customEndDate = date
                binding.editTextEndDate.setText(dateFormat.format(Date(date)))
                updateCustomFilter()
            }
        }
    }

    private fun showCustomDateRange() {
        binding.layoutCustomDateRange.visibility = View.VISIBLE
    }

    private fun hideCustomDateRange() {
        binding.layoutCustomDateRange.visibility = View.GONE
    }

    private fun updateCustomFilter() {
        if (customStartDate != null && customEndDate != null) {
            viewModel.setTimeFilter(TimeFilter.CUSTOM, customStartDate, customEndDate)
        }
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.pieChartCategories.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorySpending.collect { categorySpending ->
                updatePieChart(categorySpending)
            }
        }
    }

    private fun updatePieChart(categorySpending: List<com.financetracker.data.models.CategorySpending>) {
        if (categorySpending.isEmpty()) {
            binding.pieChartCategories.visibility = View.GONE
            binding.textViewNoData.visibility = View.VISIBLE
            binding.textViewSummary.visibility = View.GONE
            return
        }

        binding.pieChartCategories.visibility = View.VISIBLE
        binding.textViewNoData.visibility = View.GONE
        binding.textViewSummary.visibility = View.VISIBLE

        val entries = categorySpending.map { spending ->
            PieEntry(spending.totalAmount.toFloat(), spending.category)
        }

        val dataSet = PieDataSet(entries, "Categories").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = com.github.mikephil.charting.utils.MPPointF(0f, 40f)
            selectionShift = 5f
            
            // Use Material Design colors
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.chart_color_1),
                ContextCompat.getColor(requireContext(), R.color.chart_color_2),
                ContextCompat.getColor(requireContext(), R.color.chart_color_3),
                ContextCompat.getColor(requireContext(), R.color.chart_color_4),
                ContextCompat.getColor(requireContext(), R.color.chart_color_5)
            ) + ColorTemplate.MATERIAL_COLORS.toList()
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChartCategories))
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        binding.pieChartCategories.data = data
        binding.pieChartCategories.highlightValues(null)
        binding.pieChartCategories.invalidate()

        // Update summary
        val totalAmount = categorySpending.sumOf { it.totalAmount }
        val totalTransactions = categorySpending.sumOf { it.transactionCount }
        binding.textViewSummary.text = "Total: ${AmountFormatter.formatAmount(requireContext(), totalAmount)} across $totalTransactions transactions"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
