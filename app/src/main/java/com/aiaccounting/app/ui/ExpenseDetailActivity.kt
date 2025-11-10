package com.aiaccounting.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aiaccounting.app.R
import com.aiaccounting.app.databinding.ActivityExpenseDetailBinding
import com.aiaccounting.app.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExpenseDetailBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private var expenseId: Long = -1
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    // Category icon mapping
    private val categoryIconMap = mapOf(
        "餐饮" to R.drawable.ic_category_food,
        "交通" to R.drawable.ic_category_transport,
        "衣服" to R.drawable.ic_category_other,
        "购物" to R.drawable.ic_category_other,
        "教育" to R.drawable.ic_category_education,
        "娱乐" to R.drawable.ic_category_entertainment,
        "生活" to R.drawable.ic_category_life,
        "运动" to R.drawable.ic_category_sports,
        "旅行" to R.drawable.ic_category_travel,
        "住房" to R.drawable.ic_category_housing,
        "保险" to R.drawable.ic_category_insurance,
        "服务" to R.drawable.ic_category_service,
        "公益" to R.drawable.ic_category_charity,
        "医疗" to R.drawable.ic_category_medical,
        "宠物" to R.drawable.ic_category_pet,
        "转账" to R.drawable.ic_category_transfer,
        "人情" to R.drawable.ic_category_social,
        "饮品" to R.drawable.ic_category_food,
        "水果" to R.drawable.ic_category_food,
        "日用" to R.drawable.ic_category_utilities,
        "零食" to R.drawable.ic_category_food,
        "其他" to R.drawable.ic_category_other
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Get expense ID from intent
        expenseId = intent.getLongExtra("EXPENSE_ID", -1)
        
        if (expenseId == -1L) {
            Toast.makeText(this, "无法加载消费详情", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load expense data
        loadExpenseData()
        
        // Setup delete button
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }
    
    private fun loadExpenseData() {
        viewModel.allExpenses.observe(this) { expenses ->
            val expense = expenses.find { it.id == expenseId }
            
            if (expense == null) {
                Toast.makeText(this, "消费记录不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }
            
            // Set category icon
            val iconRes = categoryIconMap[expense.category] ?: R.drawable.ic_category_other
            binding.ivCategoryIcon.setImageResource(iconRes)
            
            // Set data
            binding.tvCategory.text = expense.category
            binding.tvAmount.text = String.format("¥%.2f", expense.amount)
            binding.tvDescription.text = if (expense.description.isNotEmpty()) {
                expense.description
            } else {
                "无"
            }
            binding.tvTime.text = dateFormat.format(Date(expense.timestamp))
            binding.tvId.text = expense.id.toString()
        }
    }
    
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除消费")
            .setMessage("确定要删除这笔消费记录吗？此操作不可恢复！")
            .setPositiveButton("确定删除") { _, _ ->
                deleteExpense()
            }
            .setNegativeButton("取消", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun deleteExpense() {
        lifecycleScope.launch {
            try {
                viewModel.deleteExpenseById(expenseId)
                
                Toast.makeText(
                    this@ExpenseDetailActivity,
                    "消费记录已删除",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Close activity and return to previous screen
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExpenseDetailActivity,
                    "删除失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    companion object {
        const val EXPENSE_ID_KEY = "EXPENSE_ID"
    }
}
