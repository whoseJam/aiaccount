package com.aiaccounting.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChatMessageDao {

    @Query("UPDATE chat_messages SET expenseId = :expenseId where id = :messageId")
    suspend fun updateExpense(messageId: Long, expenseId: Long)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): LiveData<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM (SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 300) ORDER BY timestamp ASC")
    suspend fun getRecentMessages(): List<ChatMessageEntity>
    
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long
    
    @Query("DELETE FROM chat_messages WHERE id NOT IN (SELECT id FROM chat_messages ORDER BY timestamp DESC LIMIT 300)")
    suspend fun deleteOldMessages()
    
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)
    
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}
