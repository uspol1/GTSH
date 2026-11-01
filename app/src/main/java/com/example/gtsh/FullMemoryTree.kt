package com.example.gtsh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

data class TreeNode(
    val id: String,
    val x: Float,
    val y: Float,
    val content: String,
    val level: Int // 0=原话, 1=日总结, 2=周总结, 3=终极日记
)

@Composable
fun FullMemoryTreeScreen(viewModel: GtshViewModel, onDismiss: () -> Unit) {
    val history by viewModel.chatHistory.collectAsState()

    // 初始缩放调小，方便看全貌
    var scale by remember { mutableFloatStateOf(0.15f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var nodes by remember { mutableStateOf<List<TreeNode>>(emptyList()) }

    LaunchedEffect(history.size, viewModel.memorySummary) {
        val tempNodes = mutableListOf<TreeNode>()

        // --- 核心间距参数 ---
        val rawSpacingY = 450f // 原话之间的纵向间距
        val dailySpacingY = 2500f // 日总结之间的纵向间距
        val weeklySpacingY = 8000f // 周总结之间的纵向间距

        val xRaw = 0f
        val xDaily = 1500f
        val xWeekly = 3500f
        val xRoot = 6000f

        // 1. 生成日总结 (每 4 条原话算一天)
        val rawChunks = history.chunked(4)
        val dailyChunks = rawChunks.chunked(3) // 每 3 天算一周

        // 先定死日总结的 Y 坐标
        val dailyNodesData = mutableListOf<TreeNode>()
        rawChunks.forEachIndexed { index, _ ->
            val y = index * dailySpacingY + 500f // 加上初始偏移
            dailyNodesData.add(TreeNode("daily_$index", xDaily, y, "", 1))
        }

        // 先定死周总结的 Y 坐标
        val weeklyNodesData = mutableListOf<TreeNode>()
        dailyChunks.forEachIndexed { index, _ ->
            val y = index * weeklySpacingY + (weeklySpacingY / 2f) + 500f
            weeklyNodesData.add(TreeNode("weekly_$index", xWeekly, y, "", 2))
        }

        // 2. 填充原话内容，并根据日总结的 Y 坐标均匀分布
        rawChunks.forEachIndexed { index, chunk ->
            // 找到这个原话组对应的日总结
            val parentDailyY = dailyNodesData[index].y
            val count = chunk.size

            chunk.forEachIndexed { i, msg ->
                // 👈 魔法算法：把这几句话均匀挂在日总结的上下方
                // 比如 4 句话，offsetY 就是 -1.5, -0.5, 0.5, 1.5
                val offsetY = (i - (count - 1) / 2f) * rawSpacingY
                val finalY = parentDailyY + offsetY

                val text = "${if (msg.role == "user") "我" else "她"}: ${msg.content}"
                tempNodes.add(TreeNode("raw_${index}_$i", xRaw, finalY, text, 0))
            }
        }

        // 3. 填充日总结内容
        dailyNodesData.forEachIndexed { index, dailyNode ->
            val chunk = rawChunks[index]
            val text = chunk.joinToString("\n") { it.content }.take(120)
            tempNodes.add(dailyNode.copy(content = "【日总结】\n$text..."))
        }

        // 4. 填充周总结内容
        weeklyNodesData.forEachIndexed { index, weeklyNode ->
            val chunk = dailyChunks[index]
            val text = chunk.flatten().joinToString("\n") { it.content }.take(200)
            tempNodes.add(weeklyNode.copy(content = "【周总结】\n$text..."))
        }

        // 5. 最终宏观日记
        if (viewModel.memorySummary.isNotEmpty()) {
            val finalY = (rawChunks.size * dailySpacingY) / 2f
            tempNodes.add(TreeNode("root", xRoot, finalY, "【终极宏观日记】\n${viewModel.memorySummary}", 3))
        }

        nodes = tempNodes
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030303))
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.02f, 2f)
                        val zoomChange = newScale / oldScale
                        offset = (offset + pan) * zoomChange + (centroid * (1 - zoomChange))
                        scale = newScale
                    }
                }
        ) {
            // 顶部 UI
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding().zIndex(100f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("全息自顶向下记忆树", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("等距分列对齐 | 绝不重叠", color = Color(0xFF00FFFF), fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFF222222), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                val startX = 300f
                val startY = 300f
                val rawWidthPx = 600f // 原话框宽度
                val dailyWidthPx = 800f // 日总结宽度

                // 👈 连线层
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val level0 = nodes.filter { it.level == 0 }
                    val level1 = nodes.filter { it.level == 1 }
                    val level2 = nodes.filter { it.level == 2 }
                    val root = nodes.find { it.level == 3 }

                    // 画线函数
                    fun drawLineToParent(parent: TreeNode, childList: List<TreeNode>, childWidth: Float, color: Color) {
                        val startXPos = startX + parent.x
                        val startYPos = startY + parent.y + 50f // 父节点左侧中点

                        childList.forEach { child ->
                            val endXPos = startX + child.x + childWidth
                            val endYPos = startY + child.y + 30f // 子节点右侧中点

                            // 贝塞尔曲线
                            val path = Path().apply {
                                moveTo(startXPos, startYPos)
                                cubicTo(startXPos - 200f, startYPos, endXPos + 200f, endYPos, endXPos, endYPos)
                            }
                            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                        }
                    }

                    // 日总结 -> 下辖原话
                    level1.forEachIndexed { index, dailyNode ->
                        val children = level0.filter { it.id.startsWith("raw_${index}_") }
                        drawLineToParent(dailyNode, children, rawWidthPx, Color(0xFF00FFFF).copy(alpha = 0.3f))
                    }

                    // 周总结 -> 下辖日总结
                    level2.forEachIndexed { index, weeklyNode ->
                        val children = level1.drop(index * 3).take(3)
                        drawLineToParent(weeklyNode, children, dailyWidthPx, Color(0xFF9370DB).copy(alpha = 0.5f))
                    }

                    // 宏观日记 -> 周总结
                    if (root != null) {
                        drawLineToParent(root, level2, dailyWidthPx, Color(0xFFFF1493).copy(alpha = 0.7f))
                    }
                }

                // UI 节点层
                nodes.forEach { node ->
                    val width = when (node.level) { 0 -> 600.dp; 1 -> 800.dp; 2 -> 1000.dp; else -> 1400.dp }
                    val borderColor = when (node.level) {
                        0 -> Color(0xFF00FFFF)
                        1 -> Color(0xFF9370DB)
                        2 -> Color(0xFFFF69B4)
                        else -> Color(0xFFFF1493)
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((startX + node.x).roundToInt(), (startY + node.y).roundToInt()) }
                            .width(width)
                            // 高度完全由内容撑开，不再剪短！
                            .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = node.content,
                            color = borderColor,
                            fontSize = 18.sp, // 字体大一点，因为间距拉开了
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}