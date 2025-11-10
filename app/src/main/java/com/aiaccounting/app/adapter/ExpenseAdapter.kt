package com.aiaccounting.app.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiaccounting.app.R
import com.aiaccounting.app.data.ExpenseEntity
import com.aiaccounting.app.ui.ExpenseDetailActivity
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter : ListAdapter<ExpenseEntity, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {
    
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
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        
        fun bind(expense: ExpenseEntity) {
            // Set category icon
            val iconRes = categoryIconMap[expense.category] ?: R.drawable.ic_category_other
            ivCategoryIcon.setImageResource(iconRes)
            
            tvDescription.text = expense.description
            tvTime.text = dateFormat.format(Date(expense.timestamp))
            tvAmount.text = String.format("¥%.2f", expense.amount)
            
            // Set click listener to open detail activity
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ExpenseDetailActivity::class.java).apply {
                    putExtra(ExpenseDetailActivity.EXPENSE_ID_KEY, expense.id)
                }
                context.startActivity(intent)
            }
        }
    }
    
    class ExpenseDiffCallback : DiffUtil.ItemCallback<ExpenseEntity>() {
        override fun areItemsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem == newItem
        }
    }
}
