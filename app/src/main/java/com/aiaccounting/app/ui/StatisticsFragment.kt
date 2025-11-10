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
    
    // Modern minimalist grayscale color palette
    private val chartColors = listOf(
        Color.rgb(50, 50, 50),      // Dark gray
        Color.rgb(80, 80, 80),      // Medium dark gray
        Color.rgb(110, 110, 110),   // Medium gray
        Color.rgb(140, 140, 140),   // Light medium gray
        Color.rgb(170, 170, 170),   // Light gray
        Color.rgb(200, 200, 200),   // Very light gray
        Color.rgb(100, 100, 100),   // Alternative medium gray
        Color.rgb(60, 60, 60)       // Alternative dark gray
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
            setExtraOffsets(20f, 20f, 20f, 20f)  // Increased offsets for label space
            
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
        
        val entries = validStatistics.map { stat ->
            PieEntry(stat.total.toFloat(), stat.category)
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            // Visual settings
            setDrawIcons(false)
            sliceSpace = 2f  // Smaller gap for cleaner look
            selectionShift = 8f  // Larger shift on selection
            colors = chartColors
            
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
            // Custom formatter to show category name and percentage
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
                    return pieEntry.label + "\n" + String.format("%.1f%%", value)
                }
            })
            setValueTextSize(11f)  // Match dataSet text size
            setValueTextColor(Color.BLACK)
        }
        
        binding.chartPie.data = data
        binding.chartPie.centerText = if (isMonthSelected) "本月" else "本年"
        binding.chartPie.invalidate()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
