package com.aiaccounting.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao {
    
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long
    
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): LiveData<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesByTimeRange(startTime: Long, endTime: Long): LiveData<List<ExpenseEntity>>
    
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryStatistics(startTime: Long, endTime: Long): List<CategoryStatistic>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Double?
    
    @Delete
    suspend fun delete(expense: ExpenseEntity)
    
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Long)
    
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

data class CategoryStatistic(
    val category: String,
    val total: Double,
    val count: Int
)
