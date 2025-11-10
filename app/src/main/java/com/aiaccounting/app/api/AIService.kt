package com.aiaccounting.app.api

import retrofit2.Response
import retrofit2.http.*

interface AIService {
    @POST("chat/completions")
    suspend fun analyzeExpense(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
