package com.aiaccounting.app.ui

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistics Fragment - displays expense statistics with pie chart and category breakdown
 * Refactored to use ExpensePieChartView component for better code organization
 */
class StatisticsFragment : Fragment() {
    
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryStatAdapter
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var pieChartView: ExpensePieChartView
    
    // Period selection state
    private var isMonthSelected = true
    
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
        
        // Initialize pie chart view
        pieChartView = binding.expensePieChart
        
        setupPeriodToggle()
        setupRecyclerViews()
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
                pieChartView.updateChart(statistics, "本月")
                categoryAdapter.setStatistics(statistics)
            }
        } else {
            binding.tvPeriodLabel.text = "本年支出"
            // Trigger year data update
            viewModel.yearTotal.value?.let { total ->
                binding.tvTotal.text = String.format("¥%.2f", total)
            }
            viewModel.yearStatistics.value?.let { statistics ->
                pieChartView.updateChart(statistics, "本年")
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
                pieChartView.updateChart(statistics, "本月")
                categoryAdapter.setStatistics(statistics)
            }
        }
        
        viewModel.yearStatistics.observe(viewLifecycleOwner) { statistics ->
            if (!isMonthSelected) {
                pieChartView.updateChart(statistics, "本年")
                categoryAdapter.setStatistics(statistics)
            }
        }
        
        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.submitList(expenses)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
