package com.aiaccounting.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aiaccounting.app.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val onDeleteClick: (ChatMessage) -> Unit) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    
    private val messages = mutableListOf<ChatMessage>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_SYSTEM = 2
        private const val VIEW_TYPE_EXPENSE_CARD = 3
    }
    
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun removeMessage(message: ChatMessage) {
        val position = messages.indexOf(message)
        if (position != -1) {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun addMessages(newMessages: List<ChatMessage>) {
        val startPosition = messages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(startPosition, newMessages.size)
    }
    
    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }
    
    fun getMessages(): List<ChatMessage> {
        return messages.toList()
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.expenseData != null -> VIEW_TYPE_EXPENSE_CARD
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_SYSTEM
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_chat_user
            VIEW_TYPE_EXPENSE_CARD -> R.layout.item_chat_expense_card
            else -> R.layout.item_chat_system
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ChatViewHolder(view, viewType)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class ChatViewHolder(itemView: View, private val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView? = itemView.findViewById(R.id.tv_message)
        private val tvTime: TextView? = itemView.findViewById(R.id.tv_time)
        
        // Expense card views
        private val tvExpenseTime: TextView? = itemView.findViewById(R.id.tv_expense_time)
        private val tvCategory: TextView? = itemView.findViewById(R.id.tv_category)
        private val tvDescription: TextView? = itemView.findViewById(R.id.tv_description)
        private val tvAmount: TextView? = itemView.findViewById(R.id.tv_amount)
        private val ivCategoryIcon: ImageView? = itemView.findViewById(R.id.iv_category_icon)
        private val btnDelete: Button? = itemView.findViewById(R.id.btn_delete)
        
        fun bind(message: ChatMessage) {
            when (viewType) {
                VIEW_TYPE_EXPENSE_CARD -> {
                    message.expenseData?.let { expense ->
                        tvExpenseTime?.text = dateFormat.format(Date(message.timestamp))
                        tvCategory?.text = expense.category
                        tvDescription?.text = expense.description
                        tvAmount?.text = "¥${String.format("%.2f", expense.amount)}"
                        
                        // Set category icon based on category
                        ivCategoryIcon?.setImageResource(getCategoryIcon(expense.category))
                        
                        // Set delete button click listener
                        btnDelete?.setOnClickListener {
                            onDeleteClick(message)
                        }
                    }
                }
                else -> {
                    tvMessage?.text = message.text
                    tvTime?.text = dateFormat.format(Date(message.timestamp))
                }
            }
        }
        
        private fun getCategoryIcon(category: String): Int {
            return when (category) {
                "餐饮" -> R.drawable.ic_category_food
                "交通" -> R.drawable.ic_category_transport
                "衣服" -> R.drawable.ic_category_other
                "购物" -> R.drawable.ic_category_other
                "教育" -> R.drawable.ic_category_education
                "娱乐" -> R.drawable.ic_category_entertainment
                "生活" -> R.drawable.ic_category_life
                "运动" -> R.drawable.ic_category_sports
                "旅行" -> R.drawable.ic_category_travel
                "住房" -> R.drawable.ic_category_housing
                "保险" -> R.drawable.ic_category_insurance
                "服务" -> R.drawable.ic_category_service
                "公益" -> R.drawable.ic_category_charity
                "医疗" -> R.drawable.ic_category_medical
                "宠物" -> R.drawable.ic_category_pet
                "转账" -> R.drawable.ic_category_transfer
                "人情" -> R.drawable.ic_category_social
                "饮品" -> R.drawable.ic_category_food
                "水果" -> R.drawable.ic_category_food
                "日用" -> R.drawable.ic_category_utilities
                "零食" -> R.drawable.ic_category_food
                else -> R.drawable.ic_category_other
            }
        }
    }
}

data class ChatMessage(
    val id: Long = 0,  // ID for tracking messages in database
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean = false,  // true for user messages, false for system messages
    val expenseData: ExpenseData? = null,  // for accounting success card
    val expenseId: Long? = null  // ID of the expense record in database
)

data class ExpenseData(
    val category: String,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
