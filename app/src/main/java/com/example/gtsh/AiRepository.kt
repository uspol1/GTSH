package com.example.gtsh

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiRepository {
    fun streamChat(baseUrl: String, apiKey: String, model: String, systemPrompt: String, history: List<ChatMessage>): Flow<String> = callbackFlow {
        if (apiKey.isBlank()) {
            trySend("哥哥，你还没在设置里填 API Key 哦。<好感变化:+0>")
            close()
            return@callbackFlow
        }

        val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build()
        val jsonBody = JSONObject().apply {
            put("model", if (model.isBlank()) "deepseek-chat" else model)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                history.forEach { msg -> put(JSONObject().apply { put("role", msg.role); put("content", msg.content) }) }
            })
        }.toString()

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val buffer = StringBuilder()
        val eventSource = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    if (buffer.isNotEmpty()) { trySend(buffer.toString()); buffer.clear() }
                    close()
                    return
                }
                try {
                    val json = JSONObject(data)
                    val content = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content") ?: ""
                    if (content.isNotEmpty() && content != "null") {
                        buffer.append(content)
                        val textToSend = buffer.toString()
                        if (!textToSend.contains("<") || textToSend.contains(">")) {
                            trySend(textToSend)
                            buffer.clear()
                        }
                    }
                } catch (e: Exception) {}
            }
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) { close(t) }
            override fun onClosed(eventSource: EventSource) { close() }
        })
        awaitClose { eventSource.cancel() }
    }

    suspend fun getOneShotMessage(baseUrl: String, apiKey: String, model: String, systemPrompt: String, prompt: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "API Key 未配置"
        val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
        val jsonBody = JSONObject().apply {
            put("model", if (model.isBlank()) "deepseek-chat" else model)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", prompt) })
            })
        }.toString()
        val request = Request.Builder().url("${baseUrl.trimEnd('/')}/chat/completions").header("Authorization", "Bearer $apiKey").header("Content-Type", "application/json").post(jsonBody.toRequestBody("application/json".toMediaType())).build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                JSONObject(body).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: "呜..."
            } else "请求失败: ${response.code}"
        } catch (e: Exception) { "网络异常: ${e.message}" }
    }
}