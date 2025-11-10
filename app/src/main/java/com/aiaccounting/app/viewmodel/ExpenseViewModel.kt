package com.aiaccounting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiaccounting.app.api.AIHelper
import com.aiaccounting.app.api.InvalidExpenseException
import com.aiaccounting.app.data.AppDatabase
import com.aiaccounting.app.data.CategoryStatistic
import com.aiaccounting.app.data.ChatMessageRepository
import com.aiaccounting.app.data.ExpenseEntity
import com.aiaccounting.app.data.ExpenseRepository
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    
    private val expenseRepository: ExpenseRepository
    private val chatMessageRepository: ChatMessageRepository
    private val aiHelper: AIHelper = AIHelper(application)
    
    val allExpenses: LiveData<List<ExpenseEntity>>
    
    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    private val _latestExpense = MutableLiveData<ExpenseEntity>()
    val latestExpense: LiveData<ExpenseEntity> = _latestExpense
    
    private val _monthStatistics = MutableLiveData<List<CategoryStatistic>>()
    val monthStatistics: LiveData<List<CategoryStatistic>> = _monthStatistics
    
    private val _yearStatistics = MutableLiveData<List<CategoryStatistic>>()
    val yearStatistics: LiveData<List<CategoryStatistic>> = _yearStatistics
    
    private val _monthTotal = MutableLiveData<Double>()
    val monthTotal: LiveData<Double> = _monthTotal
    
    private val _yearTotal = MutableLiveData<Double>()
    val yearTotal: LiveData<Double> = _yearTotal
    
    init {
        val database = AppDatabase.getDatabase(application)
        expenseRepository = ExpenseRepository(database.expenseDao())
        chatMessageRepository = ChatMessageRepository(database.chatMessageDao())
        allExpenses = expenseRepository.allExpenses
    }
    
    fun processChatMessage(messageId: Long, text: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            runCatching {
                aiHelper.analyzeExpense(text).getOrThrow()
            }.onSuccess { analysis ->
                runCatching {
                    val expense = ExpenseEntity(
                        category = analysis.category,
                        amount = analysis.amount,
                        description = analysis.description
                    )
                    val expenseId = expenseRepository.insert(expense)
                    chatMessageRepository.updateExpense(messageId, expenseId)
                    _latestExpense.value = expense.copy(id = expenseId)
                    _successMessage.value = "记账成功：${analysis.category} ¥${analysis.amount}"
                    loadStatistics()
                }.onFailure { error ->
                    _errorMessage.value = "保存失败：${error.message}"
                }
            }.onFailure { error ->
                _errorMessage.value = when (error) {
                    is InvalidExpenseException -> error.message
                    else -> "分析失败：${error.message}"
                }
            }
            _isProcessing.value = false
        }
    }
    
    fun processVoiceInput(audioFile: File) {}
    
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            runCatching {
                expenseRepository.deleteById(expenseId)
                loadStatistics()
            }.onFailure { error ->
                _errorMessage.value = "删除失败：${error.message}"
            }
        }
    }
    
    fun deleteExpenseById(expenseId: Long) = deleteExpense(expenseId)
    
    fun loadStatistics() {
        viewModelScope.launch {
            runCatching {
                val calendar = Calendar.getInstance()
                val now = System.currentTimeMillis()
                
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val monthStart = calendar.timeInMillis
                
                _monthStatistics.value = expenseRepository.getCategoryStatistics(monthStart, now)
                _monthTotal.value = expenseRepository.getTotalExpense(monthStart, now)
                
                calendar.apply {
                    set(Calendar.MONTH, 0)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val yearStart = calendar.timeInMillis
                
                _yearStatistics.value = expenseRepository.getCategoryStatistics(yearStart, now)
                _yearTotal.value = expenseRepository.getTotalExpense(yearStart, now)
            }.onFailure { error ->
                _errorMessage.value = "加载统计数据失败：${error.message}"
            }
        }
    }
}
