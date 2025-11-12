package com.aiaccounting.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiaccounting.app.R
import com.aiaccounting.app.adapter.CategoryStatAdapter
import com.aiaccounting.app.adapter.ExpenseAdapter
import com.aiaccounting.app.data.CategoryStatistic
import com.aiaccounting.app.data.ExpenseEntity
import com.aiaccounting.app.databinding.FragmentStatisticsBinding
import com.aiaccounting.app.viewmodel.ExpenseViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {
    
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryStatAdapter
    private lateinit var expenseAdapter: ExpenseAdapter
    
    // Period selection state
    private var isMonthSelected = true
    
    // Configuration constants
    companion object {
        // Minimum display percentage for small categories (e.g., 0.05 means 5%)
        private const val MIN_DISPLAY_PERCENTAGE = 0.06f
        // Threshold to merge small categories (e.g., 0.03 means categories < 3% will be merged)
        private const val MERGE_THRESHOLD_PERCENTAGE = 0.03f
    }
    
    // Colorful modern color palette for different categories
    private val chartColors = listOf(
        Color.rgb(255, 99, 132),    // Red/Pink
        Color.rgb(54, 162, 235),    // Blue
        Color.rgb(255, 206, 86),    // Yellow
        Color.rgb(75, 192, 192),    // Teal
        Color.rgb(153, 102, 255),   // Purple
        Color.rgb(255, 159, 64),    // Orange
        Color.rgb(46, 204, 113),    // Green
        Color.rgb(231, 76, 60),     // Red
        Color.rgb(52, 152, 219),    // Light Blue
        Color.rgb(241, 196, 15),    // Gold
        Color.rgb(155, 89, 182),    // Violet
        Color.rgb(26, 188, 156)     // Turquoise
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPeriodToggle()
        setupRecyclerViews()
        setupChart()
        observeViewModel()
        
        // Load statistics data
        viewModel.loadStatistics()
        
        // Set default selection to month
        binding.togglePeriod.check(R.id.btn_month)
    }
    
    private fun setupPeriodToggle() {
        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_month -> {
                        isMonthSelected = true
                        updatePeriodUI()
                    }
                    R.id.btn_year -> {
                        isMonthSelected = false
                        updatePeriodUI()
                    }
                }
            }
        }
    }
    
    private fun updatePeriodUI() {
        if (isMonthSelected) {
            binding.tvPeriodLabel.text = "本月支出"
            // Trigger month data update
            viewModel.monthTotal.value?.let { total ->
                binding.tvTotal.text = String.format("¥%.2f", total)
            }
            viewModel.monthStatistics.value?.let { statistics ->
                updateChart(statistics)
                categoryAdapter.setStatistics(statistics)
            }
        } else {
            binding.tvPeriodLabel.text = "本年支出"
            // Trigger year data update
            viewModel.yearTotal.value?.let { total ->
                binding.tvTotal.text = String.format("¥%.2f", total)
            }
            viewModel.yearStatistics.value?.let { statistics ->
                updateChart(statistics)
                categoryAdapter.setStatistics(statistics)
            }
        }
    }
    
    private fun setupRecyclerViews() {
        categoryAdapter = CategoryStatAdapter { categoryStatistic ->
            // Handle detail button click - show category details
            showCategoryDetails(categoryStatistic)
        }
        binding.recyclerCategory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
        
        expenseAdapter = ExpenseAdapter()
        binding.recyclerExpenses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseAdapter
        }
    }
    
    private fun showCategoryDetails(categoryStatistic: CategoryStatistic) {
        // Filter expenses by category
        val categoryExpenses = viewModel.allExpenses.value?.filter { 
            it.category == categoryStatistic.category 
        } ?: emptyList()
        
        // Create and show dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${categoryStatistic.category}明细")
            .setMessage(buildCategoryDetailsMessage(categoryExpenses, categoryStatistic))
            .setPositiveButton("关闭", null)
            .create()
        
        dialog.show()
    }
    
    private fun buildCategoryDetailsMessage(
        expenses: List<ExpenseEntity>,
        categoryStatistic: CategoryStatistic
    ): String {
        if (expenses.isEmpty()) {
            return "暂无${categoryStatistic.category}支出记录"
        }
        
        val sb = StringBuilder()
        sb.append("总计：¥${String.format("%.2f", categoryStatistic.total)}\n")
        sb.append("共${expenses.size}笔支出\n\n")
        
        expenses.sortedByDescending { it.timestamp }.take(10).forEach { expense ->
            val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                .format(Date(expense.timestamp))
            sb.append("${date}  ¥${String.format("%.2f", expense.amount)}\n")
            if (expense.description.isNotEmpty()) {
                sb.append("  ${expense.description}\n")
            }
        }
        
        if (expenses.size > 10) {
            sb.append("\n... 还有${expenses.size - 10}笔记录")
        }
        
        return sb.toString()
    }
    
    private fun setupChart() {
        binding.chartPie.apply {
            // Basic settings
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(25f, 0f, 25f, 0f)  // Left/right offsets for text, minimal top/bottom
            
            // Hole settings - larger for modern look
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 65f
            setTransparentCircleColor(Color.rgb(240, 240, 240))
            setTransparentCircleAlpha(80)
            transparentCircleRadius = 68f
            
            // Center text
            setDrawCenterText(true)
            setCenterTextSize(14f)
            setCenterTextColor(Color.BLACK)
            
            // Interaction - disable rotation for cleaner UX
            isRotationEnabled = false
            isHighlightPerTapEnabled = true
            setTouchEnabled(true)
            
            // Legend - enable and customize
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 12f
                yEntrySpace = 8f
                yOffset = 8f
                textSize = 11f
                textColor = Color.BLACK
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                formSize = 10f
                formToTextSpace = 6f
            }
            
            // Animation
            animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }
    }
    
    private fun observeViewModel() {
        viewModel.monthTotal.observe(viewLifecycleOwner) { total ->
            if (isMonthSelected) {
                binding.tvTotal.text = String.format("¥%.2f", total)
            }
        }
        
        viewModel.yearTotal.observe(viewLifecycleOwner) { total ->
            if (!isMonthSelected) {
                binding.tvTotal.text = String.format("¥%.2f", total)
            }
        }
        
        viewModel.monthStatistics.observe(viewLifecycleOwner) { statistics ->
            if (isMonthSelected) {
                updateChart(statistics)
                categoryAdapter.setStatistics(statistics)
            }
        }
        
        viewModel.yearStatistics.observe(viewLifecycleOwner) { statistics ->
            if (!isMonthSelected) {
                updateChart(statistics)
                categoryAdapter.setStatistics(statistics)
            }
        }
        
        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.submitList(expenses)
        }
    }
    
    private fun updateChart(statistics: List<CategoryStatistic>) {
        if (statistics.isEmpty()) {
            binding.chartPie.clear()
            binding.chartPie.centerText = "暂无数据"
            return
        }
        
        // Filter out categories with zero amount (only show categories with percentage > 0)
        val validStatistics = statistics.filter { it.total > 0 }
        
        if (validStatistics.isEmpty()) {
            binding.chartPie.clear()
            binding.chartPie.centerText = "暂无数据"
            return
        }
        
        // Calculate total amount
        val totalAmount = validStatistics.sumOf { it.total.toDouble() }.toFloat()
        
        // Process statistics: merge small categories and apply minimum display percentage
        val processedStatistics = processStatisticsForDisplay(validStatistics, totalAmount)
        
        // Create entries with labels for legend display
        // Category names will be shown in legend and via ValueFormatter on indicator lines
        val entries = processedStatistics.mapIndexed { index, stat ->
            PieEntry(stat.displayValue, stat.category).apply {
                // Store category name and actual percentage in data field for ValueFormatter
                data = mapOf(
                    "category" to stat.category,
                    "actualPercentage" to (stat.actualValue / totalAmount * 100)
                )
            }
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            // Visual settings
            setDrawIcons(false)
            sliceSpace = 2f  // Smaller gap for cleaner look
            selectionShift = 8f  // Larger shift on selection
            colors = chartColors
            
            // Set custom labels for legend (not for pie slices)
            val labels = processedStatistics.map { it.category }
            // Note: We'll handle legend labels through the data structure
            
            // Value text settings - show labels outside with lines
            valueTextSize = 11f  // Reduced size for better fit
            valueTextColor = Color.BLACK
            setValueTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL))
            
            // Enable value lines to show category labels outside
            setDrawValues(true)
            setUsingSliceColorAsValueLineColor(true)  // Use slice color for lines
            valueLineColor = Color.rgb(100, 100, 100)  // Gray line color
            valueLinePart1OffsetPercentage = 80f  // Start from edge
            valueLinePart1Length = 0.3f  // First line segment length (reduced)
            valueLinePart2Length = 0.4f  // Second line segment length (reduced)
            valueLineWidth = 1.5f  // Line thickness
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE  // Position outside
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }
        
        val data = PieData(dataSet).apply {
            // Custom formatter to show category name and actual percentage
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
                    // Get category name and actual percentage from data field
                    val dataMap = pieEntry.data as? Map<*, *>
                    val categoryName = dataMap?.get("category") as? String ?: ""
                    val actualPercentage = dataMap?.get("actualPercentage") as? Float ?: value
                    // Add newline before text to push it down and center-align with indicator line
                    return "\n" + categoryName + " " + String.format("%.1f%%", actualPercentage)
                }
                
                override fun getFormattedValue(value: Float): String {
                    // Return empty string to prevent default label display
                    return ""
                }
            })
            setValueTextSize(11f)  // Match dataSet text size
            setValueTextColor(Color.BLACK)
        }
        
        binding.chartPie.data = data
        binding.chartPie.centerText = if (isMonthSelected) "本月" else "本年"
        
        binding.chartPie.invalidate()
    }
    
    /**
     * Process statistics for display:
     * 1. Merge categories below threshold into "其他" category
     * 2. Apply minimum display percentage to ensure visibility
     */
    private fun processStatisticsForDisplay(
        statistics: List<CategoryStatistic>,
        totalAmount: Float
    ): List<DisplayStatistic> {
        // Separate large and small categories
        val largeCategories = mutableListOf<DisplayStatistic>()
        val smallCategories = mutableListOf<CategoryStatistic>()
        
        statistics.forEach { stat ->
            val percentage = stat.total / totalAmount
            if (percentage >= MERGE_THRESHOLD_PERCENTAGE) {
                largeCategories.add(DisplayStatistic(
                    category = stat.category,
                    actualValue = stat.total.toFloat(),
                    displayValue = stat.total.toFloat()
                ))
            } else {
                smallCategories.add(stat)
            }
        }
        
        // Merge small categories into "其他"
        if (smallCategories.isNotEmpty()) {
            val otherTotal = smallCategories.sumOf { it.total.toDouble() }.toFloat()
            largeCategories.add(DisplayStatistic(
                category = "其他",
                actualValue = otherTotal,
                displayValue = otherTotal
            ))
        }
        
        // Apply minimum display percentage
        val result = applyMinimumDisplayPercentage(largeCategories, totalAmount)
        
        return result
    }
    
    /**
     * Apply minimum display percentage to ensure small categories are visible
     */
    private fun applyMinimumDisplayPercentage(
        statistics: List<DisplayStatistic>,
        totalAmount: Float
    ): List<DisplayStatistic> {
        val minDisplayValue = totalAmount * MIN_DISPLAY_PERCENTAGE
        
        // Calculate how much we need to add to small categories
        var totalAdjustment = 0f
        val adjustedStats = statistics.map { stat ->
            if (stat.displayValue < minDisplayValue) {
                val adjustment = minDisplayValue - stat.displayValue
                totalAdjustment += adjustment
                stat.copy(displayValue = minDisplayValue)
            } else {
                stat
            }
        }
        
        // If we added extra display value, proportionally reduce from larger categories
        if (totalAdjustment > 0) {
            val largeCategories = adjustedStats.filter { it.displayValue > minDisplayValue }
            val largeCategoriesTotal = largeCategories.sumOf { it.displayValue.toDouble() }.toFloat()
            
            if (largeCategoriesTotal > 0) {
                return adjustedStats.map { stat ->
                    if (stat.displayValue > minDisplayValue) {
                        val reductionRatio = totalAdjustment / largeCategoriesTotal
                        val newValue = stat.displayValue * (1 - reductionRatio)
                        // Ensure we don't reduce below minimum
                        stat.copy(displayValue = maxOf(newValue, minDisplayValue))
                    } else {
                        stat
                    }
                }
            }
        }
        
        return adjustedStats
    }
    
    /**
     * Data class to hold both actual and display values for statistics
     */
    private data class DisplayStatistic(
        val category: String,
        val actualValue: Float,
        val displayValue: Float
    )
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
