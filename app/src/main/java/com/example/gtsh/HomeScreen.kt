package com.example.gtsh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val NeonCyan = Color(0xFF00FFFF)
val NeonPink = Color(0xFFFF69B4)

// 保留多语言字典，但精简掉不需要的
fun tr(zh: String, lang: String): String {
    if (lang == "ZH_CN") return zh
    val dict = if (lang == "EN") {
        mapOf(
            "进入聊天" to "Enter Chat", "主页" to "Home", "设置" to "Settings",
            "云端大模型设置" to "Cloud LLM Settings", "语言设置 (Language)" to "Language",
            "聊天高级设置" to "Advanced Settings", "AI 名字" to "AI Name",
            "自定义人物提示词 (人设)" to "Custom Persona Prompt", "记忆存档管理" to "Memory Archive",
            "📂 导出存档" to "📂 Export", "📥 导入记忆" to "📥 Import",
            "📊 图像化记忆树" to "📊 Visual Memory Tree", "详细数据 >" to "Details >",
            "强制解析历史数据（生成日记）" to "Force Parse History",
            "记忆压缩强度" to "Memory Compression Strength", "触发压缩的最低对话轮数" to "Min Dialogues to Compress",
            "应用设置" to "Apply Settings", "确认" to "Confirm", "云端大模型接入" to "Cloud LLM Access",
            "接口地址 (Base URL)" to "Base URL", "验证 Key 并获取模型列表" to "Verify Key",
            "选择模型" to "Select Model"
        )
    } else {
        mapOf(
            "主页" to "主頁", "设置" to "設置", "云端大模型设置" to "雲端大模型設置",
            "语言设置 (Language)" to "語言設置 (Language)", "聊天高级设置" to "聊天高級設置",
            "AI 名字" to "AI 名字", "自定义人物提示词 (人设)" to "自定義人物提示詞 (人設)",
            "记忆存档管理" to "記憶存檔管理", "📂 导出存档" to "📂 導出存檔",
            "📥 导入记忆" to "📥 導入記憶", "📊 图像化记忆树" to "📊 圖像化記憶樹",
            "详细数据 >" to "詳細數據 >", "强制解析历史数据（生成日记）" to "強制解析歷史數據（生成日記）",
            "记忆压缩强度" to "記憶壓縮強度", "触发压缩的最低对话轮数" to "觸發壓縮的最低對話輪數",
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
    val currentLang = gtshViewModel.chatSettings.collectAsState().value.appLanguage

    LaunchedEffect(Unit) {
        gtshViewModel.appContext = context
        gtshViewModel.loadStateOnStart()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        MainContent(
            gtshViewModel = gtshViewModel,
            onNavigateToChat = { showChatScreen = true }
        )

        AnimatedVisibility(visible = showChatScreen, enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            ChatScreen(viewModel = gtshViewModel, onBack = { showChatScreen = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(gtshViewModel: GtshViewModel, onNavigateToChat: () -> Unit) {
    val tabs = listOf("主页", "设置")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val backdrop = rememberLayerBackdrop()
    val currentLang = gtshViewModel.chatSettings.collectAsState().value.appLanguage

    var triggerPage0 by remember { mutableIntStateOf(0) }
    var triggerPage1 by remember { mutableIntStateOf(0) }
    var isDrawerApiOpen by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) { 0 -> triggerPage0++; 1 -> triggerPage1++ }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                when (pageIndex) {
                    0 -> {
                        val menuItems = listOf(tr("进入聊天", currentLang))
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 130.dp, bottom = 110.dp, start = 20.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(menuItems.size) { index ->
                                val title = menuItems[index]
                                val alphaAnim = remember(triggerPage0) { Animatable(0f) }
                                val offsetAnim = remember(triggerPage0) { Animatable(150f) }
                                LaunchedEffect(triggerPage0) { delay(index * 30L); launch { alphaAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }; launch { offsetAnim.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) } }
                                Column(modifier = Modifier.fillMaxWidth().offset(y = offsetAnim.value.dp).alpha(alphaAnim.value)) {
                                    CyberClickBox(
                                        modifier = Modifier.fillMaxWidth().height(115.dp),
                                        bgBrush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B2B2))),
                                        borderColor = Color.White.copy(alpha = 0.8f),
                                        onClick = { onNavigateToChat() }
                                    ) { Text(title, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }

                    1 -> {
                        val menuItems = listOf(tr("云端大模型设置", currentLang), tr("语言设置 (Language)", currentLang))
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 130.dp, bottom = 110.dp, start = 20.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(menuItems.size) { index ->
                                val title = menuItems[index]
                                val alphaAnim = remember(triggerPage1) { Animatable(0f) }
                                val offsetAnim = remember(triggerPage1) { Animatable(150f) }
                                LaunchedEffect(triggerPage1) { delay(index * 40L); launch { alphaAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }; launch { offsetAnim.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) } }
                                Column(modifier = Modifier.fillMaxWidth().offset(y = offsetAnim.value.dp).alpha(alphaAnim.value)) {
                                    if (index == 1) {
                                        BaseDrawer {
                                            Text("🌐 " + tr("语言设置 (Language)", currentLang), color = Color.White, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LanguageSelector(currentLang = currentLang, onLanguageSelected = { gtshViewModel.updateAppLanguage(it) })
                                        }
                                    } else {
                                        CyberClickBox(
                                            modifier = Modifier.fillMaxWidth().height(115.dp),
                                            bgBrush = Brush.verticalGradient(listOf(Color(0xFFFF3B30), Color(0xFFC0392B))),
                                            borderColor = Color.White.copy(alpha = 0.8f),
                                            onClick = { isDrawerApiOpen = !isDrawerApiOpen }
                                        ) { Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                        AnimatedVisibility(visible = isDrawerApiOpen, enter = expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)), exit = shrinkVertically(animationSpec = tween(400, easing = FastOutSlowInEasing))) {
                                            DrawerApiSettings(gtshViewModel = gtshViewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp).zIndex(100f).height(80.dp).clip(RoundedCornerShape(24.dp)).drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(24.dp) }, effects = { vibrancy(); blur(16.dp.toPx()); lens(refractionHeight = 16.dp.toPx(), refractionAmount = 32.dp.toPx()) }).background(Color.White.copy(alpha = 0.35f)).border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.1f))), RoundedCornerShape(24.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 注意：如果你删了 logo 图，这里会报错。你可以随便找个系统图标替换，或者把 Image 删掉只留文字。
                Icon(
                    imageVector = Icons.Default.Settings, // 或者换成 Icons.Default.Home, Icons.Default.Build 都可以
                    contentDescription = "Logo",
                    tint = NeonCyan, // 用你定义好的赛博青色
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "GTSH", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = Color(0xFF1E1E1E))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 24.dp).zIndex(100f), contentAlignment = Alignment.Center) {
            GlassCapsuleBar(pagerState, tabs, coroutineScope, backdrop, currentLang)
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
                            Text(if(currentLang == "EN") "Affection: ${chatState.affection} | Trust: ${chatState.trust}" else "好感度: ${chatState.affection} | 信任: ${chatState.trust}", fontSize = 12.sp, color = Color(0xFFFF7597))
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
                    items(chatHistory) { msg -> ChatBubble(msg.content, msg.role == "user", currentLang) }
                    if (chatState.currentReply.isNotEmpty() || chatState.isTyping) {
                        item { ChatBubble(chatState.currentReply.ifEmpty { "Thinking..." }, false, currentLang) }
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
fun ChatBubble(text: String, isUser: Boolean, currentLang: String) {
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
    val currentLang = gtshViewModel.chatSettings.collectAsState().value.appLanguage
    val apiConfig by gtshViewModel.apiConfig.collectAsState()
    val availableModels by gtshViewModel.availableModels.collectAsState()
    val verifyStatus by gtshViewModel.verifyStatus.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    BaseDrawer {
        Text(tr("云端大模型接入", currentLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Text(tr("接口地址 (Base URL)", currentLang), color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(value = apiConfig.baseUrl, onValueChange = { gtshViewModel.setBaseUrl(it) }, modifier = Modifier.fillMaxWidth().height(48.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), placeholder = { Text("https://api.deepseek.com", fontSize = 13.sp, color = Color.DarkGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), singleLine = true)
        Spacer(modifier = Modifier.height(4.dp))

        Text("API Key", color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(value = apiConfig.apiKey, onValueChange = { gtshViewModel.setApiKey(it) }, modifier = Modifier.fillMaxWidth().height(48.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp), placeholder = { Text("sk-...", fontSize = 13.sp, color = Color.DarkGray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { gtshViewModel.verifyApiKey() }, modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
            Text(tr("验证 Key 并获取模型列表", currentLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (verifyStatus != null) {
            Text(text = verifyStatus!!, color = if (verifyStatus!!.contains("成功")) Color.Green else Color.Yellow, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(tr("选择模型", currentLang), color = Color.Gray, fontSize = 12.sp)

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.Gray, RoundedCornerShape(12.dp)).background(Color(0xFF2A2A2A))) {
            Row(modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = !dropdownExpanded }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(apiConfig.selectedModel, fontSize = 14.sp, color = NeonCyan)
                Text(text = "▼", fontSize = 10.sp, color = NeonCyan, modifier = Modifier.rotate(if (dropdownExpanded) 180f else 0f))
            }
            AnimatedVisibility(visible = dropdownExpanded, enter = expandVertically(tween(300)) + fadeIn(), exit = shrinkVertically(tween(300)) + fadeOut()) {
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
    val context = LocalContext.current
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

            // 记忆树模块
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("📊 图像化记忆树", currentLang), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showDetailTree = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(tr("详细数据 >", currentLang), color = NeonCyan, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MemoryTreeGraph() // 占位图

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

@Composable
fun CyberClickBox(modifier: Modifier = Modifier, bgBrush: Brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F5F5))), borderColor: Color = Color.LightGray.copy(alpha = 0.3f), onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    var clickOffset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    val lineProgress = remember { Animatable(0f) }
    val lineAlpha = remember { Animatable(0f) }
    Box(modifier = modifier.onGloballyPositioned { boxSize = it.size }.clip(RoundedCornerShape(20.dp)).background(bgBrush).border(1.5.dp, borderColor, RoundedCornerShape(20.dp)).pointerInput(Unit) { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(); if (event.type == PointerEventType.Release) { val change = event.changes.firstOrNull(); if (change != null && !change.isConsumed) { clickOffset = change.position; onClick(); scope.launch { rippleRadius.snapTo(0f); rippleAlpha.snapTo(0.8f); lineProgress.snapTo(0f); lineAlpha.snapTo(0.8f); launch { rippleRadius.animateTo(boxSize.width.toFloat() * 1.8f, tween(1200, easing = LinearOutSlowInEasing)) }; launch { rippleAlpha.animateTo(0f, tween(1200, easing = LinearOutSlowInEasing)) }; launch { lineProgress.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing)) }; launch { lineAlpha.animateTo(0f, tween(150, delayMillis = 1350)) } } } } } } }.drawBehind { if (rippleAlpha.value > 0f) { drawCircle(color = NeonCyan.copy(alpha = rippleAlpha.value), radius = rippleRadius.value, center = clickOffset, style = Stroke(width = 5f)) }; if (lineAlpha.value > 0f) { val startX = -100f; val endX = size.width + 100f; val currentX = startX + (endX - startX) * lineProgress.value; val angleRad = Math.toRadians(30.0); val length = size.height * 2.0f; val deltaX = (length * cos(angleRad)).toFloat(); val deltaY = (length * sin(angleRad)).toFloat(); val startY = (size.height - deltaY) / 2f; val path = Path().apply { moveTo(currentX, startY); lineTo(currentX + deltaX, startY + deltaY) }; drawPath(path = path, color = NeonCyan.copy(alpha = lineAlpha.value), style = Stroke(width = 6f)) } }, contentAlignment = Alignment.CenterStart) { Box(modifier = Modifier.padding(horizontal = 24.dp)) { content() } }
}

@Composable
fun GlassCapsuleBar(pagerState: PagerState, tabs: List<String>, coroutineScope: CoroutineScope, backdrop: Backdrop, currentLang: String) {
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    val tabWidthDp = with(density) { (barWidthPx / tabs.size).toDp() }
    val tabWidthPx = with(density) { tabWidthDp.toPx() }
    val indicatorWidthDp = tabWidthDp + 12.dp
    val indicatorHeightDp = 52.dp
    val pagerBasePx = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * tabWidthPx
    val gridCenterX = pagerBasePx + (tabWidthPx / 2)
    val targetBaseOffset = (gridCenterX - (indicatorWidthDp.value * density.density / 2)).dp
    val containerPaddingPx = (containerWidthPx - barWidthPx) / 2f
    val absoluteTargetOffset = with(density) { (containerPaddingPx + targetBaseOffset.value).toDp() }
    val animatedOffset by animateDpAsState(targetValue = if (barWidthPx > 0f && containerWidthPx > 0f) absoluteTargetOffset else 0.dp, animationSpec = tween(durationMillis = if (isDragging || pagerState.isScrollInProgress) 0 else 300, easing = FastOutSlowInEasing), label = "indicator_offset")
    val maskOffsetPx = with(density) { (tabWidthDp * pagerState.currentPage).toPx() }
    val maskWidthPx = tabWidthPx

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(64.dp).onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).height(40.dp).onGloballyPositioned { barWidthPx = it.size.width.toFloat() }.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.2f)).border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).zIndex(0f))

        Row(modifier = Modifier.fillMaxWidth(0.85f).height(40.dp).zIndex(1f), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { coroutineScope.launch { pagerState.animateScrollToPage(index) } }, contentAlignment = Alignment.Center) {
                    Text(tr(title, currentLang), fontSize = 13.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium)
                }
            }
        }

        if (barWidthPx > 0f && containerWidthPx > 0f) {
            Box(modifier = Modifier.width(indicatorWidthDp).height(indicatorHeightDp).offset(x = animatedOffset).align(Alignment.CenterStart).clip(RoundedCornerShape(26.dp)).drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(26.dp) }, effects = { vibrancy(); blur(12.dp.toPx()); lens(refractionHeight = 12.dp.toPx(), refractionAmount = 24.dp.toPx()) }).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.1f))), RoundedCornerShape(26.dp)).zIndex(2f).pointerInput(Unit) { detectDragGesturesAfterLongPress(onDragStart = { isDragging = true }, onDragEnd = { isDragging = false; val targetPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt()).coerceIn(0, tabs.size - 1); coroutineScope.launch { pagerState.animateScrollToPage(targetPage) } }, onDragCancel = { isDragging = false }, onDrag = { change, dragAmount -> change.consume(); val amplifiedDrag = dragAmount.x * 3.5f; pagerState.dispatchRawDelta(amplifiedDrag) }) })
        }

        Row(modifier = Modifier.fillMaxWidth(0.85f).height(40.dp).zIndex(3f).drawWithContent { clipRect(left = maskOffsetPx + (pagerState.currentPageOffsetFraction * maskWidthPx), right = maskOffsetPx + maskWidthPx + (pagerState.currentPageOffsetFraction * maskWidthPx), top = 0f, bottom = size.height) { this@drawWithContent.drawContent() } }, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { title ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(tr(title, currentLang), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}