package com.example.gtsh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.random.Random

@Composable
fun MemoryTreeGraph(modifier: Modifier = Modifier) {
    val nodes = remember {
        List(30) {
            val isSummary = it < 5
            GraphNode(
                x = Random.nextFloat() * 2f - 1f,
                y = Random.nextFloat() * 2f - 1f,
                radius = if (isSummary) 16f else 8f,
                color = if (isSummary) Color(0xFFFF69B4) else Color(0xFF00FFFF)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text("记忆树拓扑图", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = min(size.width, size.height) / 2.2f

            nodes.forEachIndexed { index, node ->
                if (index < 5) {
                    val startX = centerX + node.x * maxRadius
                    val startY = centerY + node.y * maxRadius
                    nodes.drop(5).take(6).forEach { child ->
                        val endX = centerX + child.x * maxRadius
                        val endY = centerY + child.y * maxRadius
                        drawLine(
                            color = Color.White.copy(alpha = 0.15f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            nodes.forEach { node ->
                val cx = centerX + node.x * maxRadius
                val cy = centerY + node.y * maxRadius

                drawCircle(color = node.color.copy(alpha = 0.2f), radius = node.radius * 2.5f, center = Offset(cx, cy))
                drawCircle(color = node.color, radius = node.radius, center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.5f), radius = node.radius - 2f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
            }
        }
    }
}

data class GraphNode(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)