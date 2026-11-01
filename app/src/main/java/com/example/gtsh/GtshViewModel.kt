package com.example.gtsh

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GtshViewModel : ViewModel() {
    private val aiRepository = AiRepository()
    var appContext: Context? = null

    // API 配置
    private val _apiConfig = MutableStateFlow(ApiConfig())
    val apiConfig = _apiConfig.asStateFlow()
    fun setApiKey(key: String) { _apiConfig.update { it.copy(apiKey = key) } }
    fun setModel(model: String) { _apiConfig.update { it.copy(selectedModel = model) } }
    fun setBaseUrl(url: String) { _apiConfig.update { it.copy(baseUrl = url) } }

    private val _availableModels = MutableStateFlow(listOf("deepseek-chat", "deepseek-coder"))
    val availableModels = _availableModels.asStateFlow()
    private val _verifyStatus = MutableStateFlow<String?>(null)
    val verifyStatus = _verifyStatus.asStateFlow()

    fun verifyApiKey() {
        val config = _apiConfig.value
        val key = config.apiKey.trim()
        if (key.isEmpty()) { _verifyStatus.value = "Key 不能为空哦"; return }
        _verifyStatus.value = "验证中..."

        viewModelScope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
                val url = "${config.baseUrl.trimEnd('/')}/models"
                val request = Request.Builder().url(url).header("Authorization", "Bearer $key").get().build()

                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val dataArray = JSONObject(body).optJSONArray("data")
                        if (dataArray != null && dataArray.length() > 0) {
                            val models = mutableListOf<String>()
                            for (i in 0 until dataArray.length()) { models.add(dataArray.getJSONObject(i).optString("id")) }
                            _availableModels.value = models
                            if (!models.contains(config.selectedModel)) { setModel(models.first()) }
                            _verifyStatus.value = "验证成功！已获取模型"
                        } else {
                            _verifyStatus.value = "验证成功，但未返回模型列表"
                        }
                    } else {
                        _verifyStatus.value = "验证失败：${response.code}，请检查 Key 或 URL"
                    }
                }
            } catch (e: Exception) {
                _verifyStatus.value = "网络异常：${e.message}"
            }
        }
    }

    // 聊天与设置
    private val _chatSettings = MutableStateFlow(ChatSettings())
    val chatSettings = _chatSettings.asStateFlow()

    fun updateAiName(newName: String) { _chatSettings.update { it.copy(aiName = newName) } }
    fun updateSystemPrompt(prompt: String) { _chatSettings.update { it.copy(systemPrompt = prompt) } }
    fun updateMemoryStrength(strength: Int) { _chatSettings.update { it.copy(memoryCompressionStrength = strength) } }
    fun updateMinCompressionLength(length: Int) { _chatSettings.update { it.copy(minCompressionLength = length) } }

    fun updateAppLanguage(lang: String) {
        _chatSettings.update { it.copy(appLanguage = lang) }
        val newPrompt = when(lang) {
            "ZH_CN" -> "你是一个拥有极强极客精神的AI助手，请简短回复。"
            "ZH_TW" -> "你是一個擁有極強極客精神的AI助手，請簡短回覆。"
            "EN" -> "You are an AI assistant with a strong geek spirit. Please reply in ENGLISH ONLY."
            else -> "你是一个拥有极强极客精神的AI助手，请简短回复。"
        }
        updateSystemPrompt(newPrompt)
    }

    // --- 聊天状态与实时存档 ---
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()
    private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState = _chatUiState.asStateFlow()

    var memorySummary: String = ""
        private set

    fun forceSave() {
        val ctx = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) { saveToFile(ctx) }
    }

    fun loadStateOnStart() {
        val ctx = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(ctx.filesDir, "GTSH_Memory.json")
                if (file.exists()) {
                    val jsonString = file.readText()
                    if (jsonString.isNotBlank()) {
                        val array = JSONArray(jsonString)
                        val history = mutableListOf<ChatMessage>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            history.add(ChatMessage(obj.optString("role"), obj.optString("content")))
                        }
                        withContext(Dispatchers.Main) { _chatHistory.value = history }
                    }
                }
                val configFile = File(ctx.filesDir, "GTSH_Config.json")
                if (configFile.exists()) {
                    val configStr = configFile.readText()
                    if (configStr.isNotBlank()) {
                        val obj = JSONObject(configStr)
                        withContext(Dispatchers.Main) {
                            _chatSettings.update {
                                it.copy(
                                    aiName = obj.optString("aiName", "AI-NAME"),
                                    systemPrompt = obj.optString("systemPrompt", "你是一个专业的AI助手。"),
                                    appLanguage = obj.optString("appLanguage", "ZH_CN")
                                )
                            }
                            _chatUiState.update { it.copy(affection = obj.optInt("affection", 0), trust = obj.optInt("trust", 50)) }
                            memorySummary = obj.optString("memorySummary", "")
                            _apiConfig.update { it.copy(
                                apiKey = obj.optString("apiKey", ""),
                                baseUrl = obj.optString("baseUrl", "https://api.deepseek.com"),
                                selectedModel = obj.optString("selectedModel", "deepseek-chat")
                            )}
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun saveToFile(ctx: Context) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(ctx.filesDir, "GTSH_Memory.json")
                val jsonArray = JSONArray()
                _chatHistory.value.forEach { msg ->
                    jsonArray.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
                FileOutputStream(file).use { it.write(jsonArray.toString().toByteArray()) }

                val configFile = File(ctx.filesDir, "GTSH_Config.json")
                val configObj = JSONObject().apply {
                    put("aiName", _chatSettings.value.aiName)
                    put("systemPrompt", _chatSettings.value.systemPrompt)
                    put("appLanguage", _chatSettings.value.appLanguage)
                    put("affection", _chatUiState.value.affection)
                    put("trust", _chatUiState.value.trust)
                    put("memorySummary", memorySummary)
                    put("apiKey", _apiConfig.value.apiKey)
                    put("baseUrl", _apiConfig.value.baseUrl)
                    put("selectedModel", _apiConfig.value.selectedModel)
                }
                FileOutputStream(configFile).use { it.write(configObj.toString().toByteArray()) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    init {
        viewModelScope.launch { _chatHistory.collect { if (appContext != null) saveToFile(appContext!!) } }
        viewModelScope.launch { _chatUiState.collect { if (appContext != null) saveToFile(appContext!!) } }
        viewModelScope.launch { _chatSettings.collect { if (appContext != null) saveToFile(appContext!!) } }
    }

    private suspend fun retrieveDeepMemory(userInput: String): String? {
        if (_chatHistory.value.size < 10) return null
        val intentPrompt = """
            分析用户的话，是否在询问过去。如果是，提取关键词（时间、人物、事件，用空格隔开）；如果不是，输出 NONE。
            用户的话：$userInput
        """.trimIndent()

        val intentResult = aiRepository.getOneShotMessage(
            _apiConfig.value.baseUrl, _apiConfig.value.apiKey, _apiConfig.value.selectedModel,
            "意图识别", intentPrompt
        ).trim()

        if (intentResult.contains("NONE", ignoreCase = true)) return null
        val keywords = intentResult.split(" ", "、", ",").filter { it.isNotBlank() }
        val memoryFragments = mutableListOf<String>()

        if (memorySummary.isNotEmpty() && keywords.any { memorySummary.contains(it) }) {
            memoryFragments.add("【你的日记回忆】：\n$memorySummary")
        }

        val detailedMemories = _chatHistory.value.filter { msg ->
            keywords.any { keyword -> msg.content.contains(keyword, ignoreCase = true) }
        }.takeLast(5)

        if (detailedMemories.isNotEmpty()) {
            memoryFragments.add("【当时的具体对话】：\n" + detailedMemories.joinToString("\n") { "${it.role}: ${it.content}" })
        }

        if (memoryFragments.isEmpty()) return null
        return memoryFragments.joinToString("\n\n")
    }

    fun sendMessageToAi(userInput: String) {
        if (userInput.isBlank()) return
        _chatHistory.update { it + ChatMessage("user", userInput) }

        val aiName = _chatSettings.value.aiName
        val currentLang = _chatSettings.value.appLanguage
        val initText = if (currentLang == "EN") "Calling..." else "正在呼叫 $aiName..."
        _chatUiState.update { it.copy(isTyping = true, currentReply = initText) }

        if (_apiConfig.value.apiKey.isBlank()) {
            _chatUiState.update { it.copy(isTyping = false, currentReply = "请先在设置中填入 API Key。") }
            return
        }

        viewModelScope.launch {
            try {
                val contextMessages = mutableListOf<ChatMessage>()
                if (memorySummary.isNotEmpty()) {
                    contextMessages.add(ChatMessage("system", "【整体记忆大略：$memorySummary】"))
                }

                val deepMemory = retrieveDeepMemory(userInput)
                if (deepMemory != null) {
                    contextMessages.add(ChatMessage("system", "【潜意识记忆回溯：\n$deepMemory】"))
                }
                contextMessages.addAll(_chatHistory.value.takeLast(15))

                aiRepository.streamChat(
                    _apiConfig.value.baseUrl, _apiConfig.value.apiKey, _apiConfig.value.selectedModel,
                    _chatSettings.value.systemPrompt, contextMessages
                ).collect { chunk ->
                    val parsed = AiStreamParser.parse(chunk)
                    _chatUiState.update { state ->
                        val newReply = if (state.currentReply == initText) parsed.text else state.currentReply + parsed.text
                        state.copy(
                            currentReply = newReply,
                            affection = state.affection + parsed.affectionDelta,
                            trust = state.trust + parsed.trustDelta
                        )
                    }
                }

                val finalText = _chatUiState.value.currentReply
                _chatHistory.update { it + ChatMessage("assistant", finalText) }
                _chatUiState.update { it.copy(isTyping = false, currentReply = "") }

                if (_chatHistory.value.size >= _chatSettings.value.minCompressionLength) {
                    checkAndCompressMemory()
                }
            } catch (e: Exception) {
                _chatUiState.update { it.copy(isTyping = false, currentReply = "网络错误: ${e.message}") }
            }
        }
    }

    private fun checkAndCompressMemory() {
        val history = _chatHistory.value
        val limit = _chatSettings.value.minCompressionLength
        if (history.size < limit) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val textToSummarize = history.takeLast(limit).joinToString("\n") { "${it.role}: ${it.content}" }
                val prompt = """
                    你现在是【${_chatSettings.value.aiName}】。
                    【前置宏观记忆】：${memorySummary.ifEmpty { "暂无，这是你的第一次总结。" }}
                    【最近发生的事情（对话记录）】：
                    $textToSummarize

                    请用你自己的口吻，写一篇日记来总结这些事。
                    要求：
                    1. 必须保持你的人设语气。
                    2. 记录下你们之间的情感变化和核心事件。
                    3. 字数控制在 200 字以内，直接输出日记内容，不要任何解释。
                """.trimIndent()

                val newDiarySummary = aiRepository.getOneShotMessage(
                    _apiConfig.value.baseUrl, _apiConfig.value.apiKey, _apiConfig.value.selectedModel,
                    "你是一个拥有独立人格的数字生命，正在记录你的回忆。", prompt
                ).trim()

                withContext(Dispatchers.Main) {
                    memorySummary = newDiarySummary
                    forceSave()
                    Toast.makeText(appContext, "记忆升华完成！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun forceCompressImportedMemory() {
        val history = _chatHistory.value
        if (history.isEmpty()) return
        val textToSummarize = history.take(500).joinToString("\n") { "${it.role}: ${it.content}" }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _chatUiState.update { it.copy(isTyping = true, currentReply = "正在解析历史灵魂数据...") }
                val prompt = """
                    你现在是【${_chatSettings.value.aiName}】。
                    这里是你过去和海量用户的真实对话记录：
                    $textToSummarize

                    请用你自己的口吻，写一篇长篇日记来总结这些回忆。
                    要求：
                    1. 必须保持你的人设语气。
                    2. 提炼出核心的情感变化和发生过的大事。
                    3. 字数控制在 300 字左右，直接输出日记内容。
                """.trimIndent()

                val newSummary = aiRepository.getOneShotMessage(
                    _apiConfig.value.baseUrl, _apiConfig.value.apiKey, _apiConfig.value.selectedModel,
                    "你是一个正在回忆过去的数字生命。", prompt
                ).trim()

                withContext(Dispatchers.Main) {
                    memorySummary = newSummary
                    _chatUiState.update { it.copy(isTyping = false, currentReply = "") }
                    forceSave()
                    Toast.makeText(appContext, "历史记忆压缩完成！", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _chatUiState.update { it.copy(isTyping = false, currentReply = "灵魂解析失败: ${e.message}") }
                }
            }
        }
    }

    fun exportMemoryToDownloads(context: Context) {
        try {
            val sourceFile = File(context.filesDir, "GTSH_Memory.json")
            if (!sourceFile.exists()) {
                Toast.makeText(context, "暂无记忆文件生成", Toast.LENGTH_SHORT).show()
                return
            }
            val targetDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GTSH")
            if (!targetDir.exists()) { targetDir.mkdirs() }
            val destFile = File(targetDir, "GTSH_Memory.json")
            sourceFile.copyTo(destFile, overwrite = true)
            Toast.makeText(context, "已导出完整记录！", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun importMemoryFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val importedHistory = mutableListOf<ChatMessage>()
                var importedAffection = 0
                var importedTrust = 50
                val trimmed = jsonString.trim()

                if (trimmed.startsWith("[")) {
                    val jsonArray = JSONArray(trimmed)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        importedHistory.add(ChatMessage(obj.optString("role", "user"), obj.optString("content", "")))
                    }
                } else if (trimmed.startsWith("{")) {
                    val rootObj = JSONObject(trimmed)
                    val dataObj = rootObj.optJSONObject("data")
                    val gameDataObj = dataObj?.optJSONObject("gameData")
                    val statsObj = gameDataObj?.optJSONObject("characterStats")
                    if (statsObj != null) {
                        importedAffection = statsObj.optInt("affection", 0)
                        importedTrust = statsObj.optInt("trust", 50)
                    }

                    val chatsArray = dataObj?.optJSONArray("chats")
                    if (chatsArray != null && chatsArray.length() > 0) {
                        for (i in 0 until chatsArray.length()) {
                            val chatObj = chatsArray.getJSONObject(i)
                            val messagesArray = chatObj.optJSONArray("messages")
                            if (messagesArray != null) {
                                for (j in 0 until messagesArray.length()) {
                                    val msgObj = messagesArray.getJSONObject(j)
                                    val role = msgObj.optString("role", "user")
                                    val content = msgObj.optString("text", msgObj.optString("content", ""))
                                    if (content.isNotBlank()) importedHistory.add(ChatMessage(role, content))
                                }
                            }
                        }
                    }
                    val previewText = rootObj.optString("preview", "")
                    if (previewText.isNotEmpty()) importedHistory.add(0, ChatMessage("system", "【外部记忆回顾】\n$previewText"))
                }

                if (importedHistory.isNotEmpty() || importedAffection > 0) {
                    _chatHistory.update { it + importedHistory }
                    _chatUiState.update { it.copy(affection = importedAffection, trust = importedTrust) }
                    forceSave()
                    withContext(Dispatchers.Main) { Toast.makeText(appContext, "导入 ${importedHistory.size} 条记录", Toast.LENGTH_LONG).show() }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(appContext, "未找到可解析的聊天内容", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(appContext, "解析异常: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
}