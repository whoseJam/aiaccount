package com.aiaccounting.app.api

import android.content.Context
import com.aiaccounting.app.BuildConfig
import com.google.gson.Gson
class AIHelper(private val context: Context) {
    private val chatApiKey = BuildConfig.CHAT_API_KEY
    private val chatAuthorization = "Bearer $chatApiKey"


    suspend fun analyzeExpense(text: String): Result<ExpenseAnalysis> {
        return try {
            val systemMessage = Message(
                role = "system",
                content = """你是一个记账助手。用户会告诉你一笔支出，你需要先判断这是否是一条记账类型的文本。

**判断规则：**
- 记账类型文本：包含消费/支出行为和金额信息（如"买了XX花了XX元"、"午饭35元"、"打车20"等）
- 非记账类型文本：问候语、闲聊、询问、无关内容等（如"你好"、"今天天气怎么样"、"帮我查一下"等）

**如果是记账类型文本**，分析出：
1. 支出类目（从以下选择：餐饮、交通、衣服、购物、教育、娱乐、生活、运动、旅行、住房、保险、服务、公益、医疗、宠物、转账、人情、饮品、水果、日用、零食、其他）
2. 支出金额（数字）
3. 描述信息

返回JSON格式：
{"category": "类目", "amount": 金额, "description": "描述"}

**如果不是记账类型文本**，返回：
{"category": "INVALID", "amount": 0, "description": "无法识别记账内容"}

例如：
输入："今天午饭花了35元"
输出：{"category": "餐饮", "amount": 35.0, "description": "午饭"}

输入："你好"
输出：{"category": "INVALID", "amount": 0, "description": "无法识别记账内容"}

输入："今天天气真好"
输出：{"category": "INVALID", "amount": 0, "description": "无法识别记账内容"}"""
            )
            
            val userMessage = Message(
                role = "user",
                content = text
            )
            
            val request = ChatRequest(
                model = BuildConfig.CHAT_MODEL,
                messages = listOf(systemMessage, userMessage),
                temperature = 0.3
            )
            
            val response = RetrofitClient.service.analyzeExpense(chatAuthorization, request)
            
            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices.firstOrNull()?.message?.content
                if (content != null) {
                    val analysis = parseExpenseAnalysis(content)
                    Result.success(analysis)
                } else {
                    Result.failure(Exception("无法解析AI响应"))
                }
            } else {
                Result.failure(Exception("分析失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseExpenseAnalysis(jsonString: String): ExpenseAnalysis {
        return try {
            val cleanJson = jsonString.trim().removePrefix("```json").removeSuffix("```").trim()
            val analysis = Gson().fromJson(cleanJson, ExpenseAnalysis::class.java)
            
            // Check if this is an invalid (non-accounting) input
            if (analysis.category == "INVALID") {
                throw InvalidExpenseException("无法识别记账内容，请输入包含消费金额的记账信息")
            }
            
            analysis
        } catch (e: InvalidExpenseException) {
            throw e
        } catch (e: Exception) {
            // 如果解析失败，尝试简单的文本分析
            val amount = extractAmount(jsonString)
            if (amount <= 0) {
                throw InvalidExpenseException("无法识别记账内容，请输入包含消费金额的记账信息")
            }
            ExpenseAnalysis(
                category = "其他",
                amount = amount,
                description = jsonString
            )
        }
    }
    
    private fun extractAmount(text: String): Double {
        val regex = """(\d+\.?\d*)""".toRegex()
        val match = regex.find(text)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }
}

data class ExpenseAnalysis(
    val category: String,
    val amount: Double,
    val description: String
)

class InvalidExpenseException(message: String) : Exception(message)
