package com.aiaccounting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiaccounting.app.adapter.ChatMessage
import com.aiaccounting.app.adapter.ExpenseData
import com.aiaccounting.app.data.AppDatabase
import com.aiaccounting.app.data.ChatMessageEntity
import com.aiaccounting.app.data.ChatMessageRepository
import com.aiaccounting.app.data.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatMessageRepository: ChatMessageRepository
    private val expenseRepository: ExpenseRepository
    
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages
    
    private val _newMessage = MutableLiveData<ChatMessage>()
    val newMessage: LiveData<ChatMessage> = _newMessage
    
    private val _deleteMessageEvent = MutableLiveData<ChatMessage>()
    val deleteMessageEvent: LiveData<ChatMessage> = _deleteMessageEvent
    
    init {
        val database = AppDatabase.getDatabase(application)
        chatMessageRepository = ChatMessageRepository(database.chatMessageDao())
        expenseRepository = ExpenseRepository(database.expenseDao())
    }
    
    fun loadHistoryMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                chatMessageRepository.getRecentMessages().map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        isUser = entity.messageType == "user",
                        expenseId = entity.expenseId,
                        expenseData = entity.expenseId?.takeIf { entity.messageType == "system" }
                            ?.let { expenseRepository.getExpenseById(it) }
                            ?.let { ExpenseData(it.category, it.amount, it.description, it.timestamp) }
                    )
                }
            }.onSuccess { _messages.postValue(it) }
             .onFailure { it.printStackTrace() }
        }
    }
    
    fun saveMessage(text: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val timestamp = System.currentTimeMillis()
                val messageId = chatMessageRepository.insert(
                    ChatMessageEntity(text = text, timestamp = timestamp, messageType = type)
                )
                ChatMessage(id = messageId, text = text, isUser = type == "user", timestamp = timestamp)
            }.onSuccess { _newMessage.postValue(it) }
             .onFailure { it.printStackTrace() }
        }
    }
    
    fun saveExpenseMessage(text: String, expenseId: Long, expenseData: ExpenseData) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val timestamp = System.currentTimeMillis()
                val messageId = chatMessageRepository.insert(
                    ChatMessageEntity(text = text, timestamp = timestamp, messageType = "system", expenseId = expenseId)
                )
                ChatMessage(id = messageId, text = text, isUser = false, expenseData = expenseData, 
                    expenseId = expenseId, timestamp = timestamp)
            }.onSuccess { _newMessage.postValue(it) }
             .onFailure { it.printStackTrace() }
        }
    }
    
    suspend fun deleteMessageById(messageId: Long) = withContext(Dispatchers.IO) {
        chatMessageRepository.deleteById(messageId)
    }
    
    suspend fun deleteAllMessages() = withContext(Dispatchers.IO) {
        chatMessageRepository.deleteAll()
    }
    
    fun notifyMessageDeleted(message: ChatMessage) {
        _deleteMessageEvent.postValue(message)
    }
}
