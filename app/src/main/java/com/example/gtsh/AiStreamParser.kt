package com.example.gtsh

object AiStreamParser {
    data class ParsedResult(val text: String, val affectionDelta: Int, val trustDelta: Int)

    fun parse(chunk: String): ParsedResult {
        var cleanText = chunk
        var affection = 0
        var trust = 0
        Regex("<好感变化:([+-]?\\d+)>").find(chunk)?.let { affection = it.groupValues[1].toIntOrNull() ?: 0; cleanText = cleanText.replace(it.value, "") }
        Regex("<信任变化:([+-]?\\d+)>").find(chunk)?.let { trust = it.groupValues[1].toIntOrNull() ?: 0; cleanText = cleanText.replace(it.value, "") }
        return ParsedResult(cleanText, affection, trust)
    }
}