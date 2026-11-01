package com.example.gtsh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val NeonCyan = Color(0xFF00FFFF)
val NeonPink = Color(0xFFFF69B4)

fun tr(zh: String, lang: String): String {
    if (lang == "ZH_CN") return zh
    val dict = if (lang == "EN") {
        mapOf(
            "主页" to "Home", "设置" to "Settings",
            "云端大模型设置" to "Cloud LLM Settings", "语言设置 (Language)" to "Language",
            "聊天高级设置" to "Advanced Settings", "AI 名字" to "AI Name",
            "自定义人物提示词 (人设)" to "Custom Persona Prompt",
            "📊 图像化记忆树" to "📊 Visual Memory Tree", "详细数据 >" to "Details >",
            "强制解析历史数据（生成日记）" to "Force Parse History",
            "应用设置" to "Apply Settings", "确认" to "Confirm", "云端大模型接入" to "Cloud LLM Access",
            "接口地址 (Base URL)" to "Base URL", "验证 Key 并获取模型列表" to "Verify Key",
            "选择模型" to "Select Model", "进入聊天" to "Enter Chat"
        )
    } else {
        mapOf(
            "主页" to "主頁", "设置" to "設置", "云端大模型设置" to "雲端大模型設置",
            "语言设置 (Language)" to "語言設置 (Language)", "聊天高级设置" to "聊天高級設置",
            "AI 名字" to "AI 名字", "自定义人物提示词 (人设)" to "自定義人物提示詞 (人設)",
            "📊 图像化记忆树" to "📊 圖像化記憶樹",
            "详细数据 >" to "詳細數據 >", "强制解析历史数据（生成日记）" to "強制解析歷史數據（生成日記）",
            "应用设置" to "應用設置", "确认" to "確認"
        )
    }
    return dict[zh] ?: zh
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(gtshViewModel: GtshViewModel = viewModel()) {
    var showChatScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("主页", "设置")
    val currentLang = gtshViewModel.chatSettings.collectAsState().value.appLanguage

    LaunchedEffect(Unit) {
        gtshViewModel.appContext = context
        gtshViewModel.loadStateOnStart()
    }

    if (showChatScreen) {
        ChatScreen(viewModel = gtshViewModel, onBack = { showChatScreen = false })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFF8F9FA)) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(if (index == 0) Icons.Default.Home else Icons.Default.Settings, contentDescription = null) },
                            label = { Text(tr(title, currentLang)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color.Black,
                                indicatorColor = NeonCyan
                            )
                        )
                    }
                }
            },
            containerColor = Color(0xFFF8F9FA)
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                // 顶部标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("GTSH", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E1E1E))
                }
                Spacer(modifier = Modifier.height(40.dp))

                if (selectedTab == 0) {
                    Button(
                        onClick = { showChatScreen = true },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(tr("进入聊天", currentLang), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    DrawerApiSettings(gtshViewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                    BaseDrawer {
                        Text("🌐 " + tr("语言设置 (Language)", currentLang), color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LanguageSelector(currentLang = currentLang, onLanguageSelected = { gtshViewModel.updateAppLanguage(it) })
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(currentLang: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("ZH_CN" to "🇨🇳 简体中文", "ZH_TW" to "🇭🇰 繁體中文", "EN" to "🇺🇸 English")
    val currentLabel = languages.find { it.first == currentLang }?.second ?: "🇨🇳 简体中文"

    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan), shape = RoundedCornerShape(12.dp)) {
            Text(currentLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E1E1E))) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(text = { Text(label, color = if (code == currentLang) NeonCyan else Color.White) }, onClick = { onLanguageSelected(code); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: GtshViewModel, onBack: () -> Unit) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatState by viewModel.chatUiState.collectAsState()
    val chatSettings by viewModel.chatSettings.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    val currentLang = chatSettings.appLanguage

    LaunchedEffect(chatState.currentReply, chatHistory.size) {
        val total = chatHistory.size + if (chatState.currentReply.isNotEmpty() || chatState.isTyping) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(chatSettings.aiName, fontWeight = FontWeight.Bold)
                            Text("好感度: ${chatState.affection} | 信任: ${chatState.trust}", fontSize = 12.sp, color = Color(0xFFFF7597))
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                    actions = { IconButton(onClick = { showSettingsDrawer = true }) { Icon(Icons.Default.Settings, contentDescription = "设置") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(chatHistory) { msg -> ChatBubble(msg.content, msg.role == "user") }
                    if (chatState.currentReply.isNotEmpty() || chatState.isTyping) {
                        item { ChatBubble(chatState.currentReply.ifEmpty { "Thinking..." }, false) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Say something...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.LightGray),
                        shape = RoundedCornerShape(24.dp), maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (inputText.isNotBlank()) { viewModel.sendMessageToAi(inputText); inputText = "" } }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), shape = RoundedCornerShape(24.dp)) {
                        Text("发送", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (showSettingsDrawer) {
            ModalBottomSheet(onDismissRequest = { showSettingsDrawer = false }, containerColor = Color(0xFF1E1E1E)) { ChatSettingsDrawer(viewModel) }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    val regex = Regex("<好感变化:([+-]?\\d+)>")
    val match = regex.find(text)
    val cleanText = text.replace(regex, "").trim()
    val displayText = if (match != null) {
        val delta = match.groupValues[1]
        "$cleanText (好感: $delta)"
    } else { cleanText }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(modifier = Modifier.widthIn(max = 280.dp).background(color = if (isUser) Color(0xFF2C2C2C) else Color(0xFF3E1F29), shape = RoundedCornerShape(16.dp)).padding(12.dp)) {
            Text(text = displayText, color = Color.White)
        }
    }
}

@Composable
fun BaseDrawer(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun DrawerApiSettings(gtshViewModel: GtshViewModel) {
    val apiConfig by gtshViewModel.apiConfig.collectAsState()
    val availableModels by gtshViewModel.availableModels.collectAsState()
    val verifyStatus by gtshViewModel.verifyStatus.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    BaseDrawer {
        Text("云端大模型接入", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Text("接口地址 (Base URL)", color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(value = apiConfig.baseUrl, onValueChange = { gtshViewModel.setBaseUrl(it) }, modifier = Modifier.fillMaxWidth().height(48.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), singleLine = true)
        Spacer(modifier = Modifier.height(4.dp))

        Text("API Key", color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(value = apiConfig.apiKey, onValueChange = { gtshViewModel.setApiKey(it) }, modifier = Modifier.fillMaxWidth().height(48.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { gtshViewModel.verifyApiKey() }, modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
            Text("验证 Key 并获取模型列表", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (verifyStatus != null) {
            Text(text = verifyStatus!!, color = if (verifyStatus!!.contains("成功")) Color.Green else Color.Yellow, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("选择模型", color = Color.Gray, fontSize = 12.sp)

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2A2A))) {
            Row(modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = !dropdownExpanded }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(apiConfig.selectedModel, fontSize = 14.sp, color = NeonCyan)
                Text(text = "▼", fontSize = 10.sp, color = NeonCyan, modifier = Modifier.rotate(if (dropdownExpanded) 180f else 0f))
            }
            AnimatedVisibility(visible = dropdownExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                    availableModels.forEach { model ->
                        Box(modifier = Modifier.fillMaxWidth().clickable { gtshViewModel.setModel(model); dropdownExpanded = false }.background(if (apiConfig.selectedModel == model) Color(0xFF333333) else Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(text = model, color = if (apiConfig.selectedModel == model) NeonCyan else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsDrawer(viewModel: GtshViewModel) {
    val settings by viewModel.chatSettings.collectAsState()
    val currentLang = settings.appLanguage
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    var showDetailTree by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().imePadding()) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(16.dp).padding(bottom = 32.dp)) {
            Text(tr("聊天高级设置", currentLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(tr("AI 名字", currentLang), color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = settings.aiName, onValueChange = { viewModel.updateAiName(it) },
                modifier = Modifier.fillMaxWidth().height(48.dp), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(tr("自定义人物提示词 (人设)", currentLang), color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = settings.systemPrompt, onValueChange = { viewModel.updateSystemPrompt(it) },
                modifier = Modifier.fillMaxWidth().height(100.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("📊 图像化记忆树", currentLang), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showDetailTree = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(tr("详细数据 >", currentLang), color = NeonCyan, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MemoryTreeGraph()

            if (showDetailTree) {
                FullMemoryTreeScreen(viewModel = viewModel, onDismiss = { showDetailTree = false })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.forceCompressImportedMemory() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1F29))) {
                Text(tr("强制解析历史数据（生成日记）", currentLang), color = Color(0xFFFF69B4), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { keyboardController?.hide() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)) {
                Text(tr("应用设置", currentLang), fontWeight = FontWeight.Bold)
            }
        }
    }
}