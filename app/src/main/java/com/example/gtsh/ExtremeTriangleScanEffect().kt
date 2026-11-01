package com.example.gtsh

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import androidx.compose.ui.graphics.drawscope.Fill

@Composable
fun ExtremeTriangleScanEffect() {
    // 1. 利用无限循环，让两条扫描线交叉扫射
    val infiniteTransition = rememberInfiniteTransition(label = "extreme_scan")

    // 扫描线 1（左上到右下）
    val progress1 by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "scan1"
    )

    // 扫描线 2（反向，制造更复杂的视差和光效叠加）
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 1.5f, targetValue = -0.5f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "scan2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val triangleSize = 60f // 三角形稍微放大一点，更有压迫感
        val cols = (size.width / triangleSize).toInt() + 1
        val rows = (size.height / triangleSize).toInt() + 1

        // 两条扫描线的当前坐标
        val scan1X = size.width * progress1
        val scan2X = size.width * progress2

        // 👈 SFL 状态标记：同一帧内防重算
        val isVisited = Array(rows) { BooleanArray(cols) { false } }

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                if (isVisited[row][col]) continue

                val triCenterX = col * triangleSize + triangleSize / 2
                val triCenterY = row * triangleSize + triangleSize / 2

                // 计算到两条扫描线的距离，取最小值（哪条线近算哪条）
                val distToScan1 = abs(triCenterX - scan1X)
                val distToScan2 = abs(triCenterX - scan2X)
                val minDistance = minOf(distToScan1, distToScan2)

                var brightness = 0.05f // 背景暗度（极暗）

                // 光晕扩散范围拉大（覆盖 4 个三角形）
                if (minDistance < triangleSize * 4) {
                    // 核心算法：90% 亮度起步，距离越远衰减越快
                    val decaySteps = minDistance / triangleSize
                    brightness = (0.9f - (decaySteps * 0.2f)).coerceAtLeast(0.05f)

                    isVisited[row][col] = true
                }

                // 👈 性能溢出专属：颜色叠加发光（Neon Pink 和 Neon Cyan 混合）
                val baseColor = Color(0xFF00FFFF) // 赛博青
                val highlightColor = Color(0xFFFF69B4) // 赛博粉（高光）

                // 如果亮度极高，混入粉色高光，形成光斑效果
                val drawColor = if (brightness > 0.7f) {
                    lerp(baseColor, highlightColor, (brightness - 0.7f) * 3f)
                } else {
                    lerp(Color(0xFF0A0A0A), baseColor, brightness)
                }

                // 绘制全等三角形
                val path = Path().apply {
                    moveTo(col * triangleSize, row * triangleSize)
                    lineTo((col + 1) * triangleSize, row * triangleSize)
                    lineTo(col * triangleSize, (row + 1) * triangleSize)
                    close()
                }

                // 高光时刻，直接上 Fill 填充，其他时候用 Stroke 描边
                drawPath(
                    path = path,
                    color = drawColor.copy(alpha = if (brightness > 0.4f) brightness else 0.3f),
                    style = if (brightness > 0.5f) Fill else Stroke(width = 1.5f)
                )
            }
        }
    }
}