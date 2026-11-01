package com.example.gtsh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(viewModel: GtshViewModel) {

    val chatSettings by viewModel.chatSettings.collectAsState()
    val currentLang = chatSettings.appLanguage
    val uiState by viewModel.chatUiState.collectAsState()
    val history by viewModel.chatHistory.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    // 自动滚动到底部
    LaunchedEffect(uiState.currentReply, history.size) {
        val total = history.size + if (uiState.currentReply.isNotEmpty() || uiState.isTyping) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {

        // 顶部数值面板 (你可以把这部分换成你想要的样式)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("❤️ 好感: ${uiState.affection}", color = Color(0xFFFF7597))
            Text("🛡️ 信任: ${uiState.trust}", color = Color(0xFF75B8FF))
        }

        // 聊天列表
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { msg ->
                ChatBubble(text = msg.content, isUser = msg.role == "user", currentLang = currentLang)
            }
            if (uiState.currentReply.isNotEmpty() || uiState.isTyping) {
                item {
                    ChatBubble(
                        text = uiState.currentReply.ifEmpty { if (currentLang == "EN") "Thinking..." else "正在思考..." },
                        isUser = false,
                        currentLang = currentLang
                    )
                }
            }
        }

        // 底部输入框
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 90.dp, start = 16.dp, end = 16.dp), // 底部留白防止被胶囊挡
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("对 Yuki 说点什么...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF7597), unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.sendMessageToAi(input)
                        input = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7597)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("发送", color = Color.White)
            }
        }
    }
}

