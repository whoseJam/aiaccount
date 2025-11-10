package com.aiaccounting.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aiaccounting.app.R
import com.aiaccounting.app.data.CategoryStatistic

class CategoryStatAdapter(
    private val onDetailClick: (CategoryStatistic) -> Unit
) : RecyclerView.Adapter<CategoryStatAdapter.CategoryStatViewHolder>() {
    
    private val statistics = mutableListOf<CategoryStatistic>()
    private var totalAmount = 0.0
    
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
    
    fun setStatistics(stats: List<CategoryStatistic>) {
        statistics.clear()
        statistics.addAll(stats)
        
        // Calculate total amount
        totalAmount = stats.sumOf { it.total }
        
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_stat, parent, false)
        return CategoryStatViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CategoryStatViewHolder, position: Int) {
        holder.bind(statistics[position])
    }
    
    override fun getItemCount(): Int = statistics.size
    
    inner class CategoryStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tv_percentage)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)
        private val progressBar: View = itemView.findViewById(R.id.progress_bar)
        private val btnDetail: ImageView = itemView.findViewById(R.id.btn_detail)
        
        fun bind(stat: CategoryStatistic) {
            // Set category icon
            val iconRes = categoryIconMap[stat.category] ?: R.drawable.ic_category_other
            ivCategoryIcon.setImageResource(iconRes)
            
            // Set category name
            tvCategory.text = stat.category
            
            // Calculate and set percentage
            val percentage = if (totalAmount > 0) {
                (stat.total / totalAmount * 100).toInt()
            } else {
                0
            }
            tvPercentage.text = String.format("%d%%", percentage)
            
            // Set amount
            tvAmount.text = String.format("¥%.2f", stat.total)
            
            // Set count
            tvCount.text = String.format("%d笔", stat.count)
            
            // Update progress bar width
            val layoutParams = progressBar.layoutParams
            progressBar.post {
                val parentWidth = (progressBar.parent as View).width
                layoutParams.width = (parentWidth * percentage / 100f).toInt()
                progressBar.layoutParams = layoutParams
            }
            
            // Set detail button click listener
            btnDetail.setOnClickListener {
                onDetailClick(stat)
            }
        }
    }
}
