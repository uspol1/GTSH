package com.example.gtsh

data class ChatMessage(val role: String, val content: String)

data class ChatUiState(
    val isTyping: Boolean = false,
    val currentReply: String = "",
    val affection: Int = 0,
    val trust: Int = 50,
    val error: String? = null
)

data class TimerRule(
    val id: String,
    val label: String,
    val intervalSeconds: Long,
    val isEnabled: Boolean
)

data class ProactiveSettings(
    val isEnabled: Boolean = false,
    val minIntervalSeconds: Long = 60,
    val maxIntervalSeconds: Long = 3600
)

data class ChatSettings(
    val aiName: String = "AI-NAME",
    val systemPrompt: String = "你是一名专业的心理辅导医生，兼结构架构师",
    val memoryCompressionStrength: Int = 50,
    val minCompressionLength: Int = 1000,
    val appLanguage: String = "ZH_CN" // 👈 新增
)

data class ApiConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val selectedModel: String = "deepseek-chat"
)