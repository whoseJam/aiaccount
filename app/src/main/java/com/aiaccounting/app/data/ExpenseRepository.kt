package com.aiaccounting.app.data

import androidx.lifecycle.LiveData

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    
    val allExpenses: LiveData<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    
    suspend fun insert(expense: ExpenseEntity): Long {
        return expenseDao.insert(expense)
    }
    
    fun getExpensesByTimeRange(startTime: Long, endTime: Long): LiveData<List<ExpenseEntity>> {
        return expenseDao.getExpensesByTimeRange(startTime, endTime)
    }
    
    suspend fun getCategoryStatistics(startTime: Long, endTime: Long): List<CategoryStatistic> {
        return expenseDao.getCategoryStatistics(startTime, endTime)
    }
    
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double {
        return expenseDao.getTotalExpense(startTime, endTime) ?: 0.0
    }
    
    suspend fun getExpenseById(expenseId: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(expenseId)
    }
    
    suspend fun delete(expense: ExpenseEntity) {
        expenseDao.delete(expense)
    }
    
    suspend fun deleteById(expenseId: Long) {
        expenseDao.deleteById(expenseId)
    }
}
