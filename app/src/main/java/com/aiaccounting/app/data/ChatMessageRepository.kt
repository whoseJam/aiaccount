package com.aiaccounting.app.data

import androidx.lifecycle.LiveData

class ChatMessageRepository(private val chatMessageDao: ChatMessageDao) {
    
    val allMessages: LiveData<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    
    suspend fun insert(message: ChatMessageEntity): Long {
        val id = chatMessageDao.insert(message)
        // After inserting, check if we need to delete old messages
        val count = chatMessageDao.getMessageCount()
        if (count > 300) {
            chatMessageDao.deleteOldMessages()
        }
        return id
    }
    
    suspend fun updateExpense(messageId: Long, expenseId: Long) {
        chatMessageDao.updateExpense(messageId, expenseId)
    }
    
    suspend fun getRecentMessages(): List<ChatMessageEntity> {
        return chatMessageDao.getRecentMessages()
    }
    
    suspend fun deleteById(messageId: Long) {
        chatMessageDao.deleteById(messageId)
    }
    
    suspend fun deleteAll() {
        chatMessageDao.deleteAll()
    }
}
