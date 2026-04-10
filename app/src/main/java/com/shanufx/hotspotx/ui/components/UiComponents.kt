package com.shanufx.hotspotx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shanufx.hotspotx.ui.theme.*

// ─────────────────────────────────────────────────────────────
// Glassmorphism card
// ─────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ─────────────────────────────────────────────────────────────
// Animated hotspot toggle
// ─────────────────────────────────────────────────────────────
@Composable
fun HotspotToggle(
    isActive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue  = if (isActive) 0.70f else 0.25f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(120.dp * pulseScale)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            if (isActive) drawCircle(
                color  = CyanPrimary.copy(alpha = glowAlpha),
                radius = size.minDimension / 2f,
                style  = Stroke(width = 6.dp.toPx())
            )
        }
        Button(
            onClick      = onClick,
            enabled      = !isLoading,
            shape        = RoundedCornerShape(50),
            colors       = ButtonDefaults.buttonColors(
                containerColor = if (isActive) CyanPrimary else SurfaceVariant
            ),
            modifier     = Modifier.size(96.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isLoading) CircularProgressIndicator(
                modifier    = Modifier.size(28.dp),
                color       = Color.White,
                strokeWidth = 2.dp
            ) else Text(
                text  = if (isActive) "ON" else "OFF",
                style = MaterialTheme.typography.titleLarge,
                color = if (isActive) Color.Black else TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Spark-line chart (Canvas, last N points)
// ─────────────────────────────────────────────────────────────
@Composable
fun SparkLineChart(
    uploadPoints: List<Float>,
    downloadPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (uploadPoints.size >= 2)   drawSparkSeries(uploadPoints,   ChartUpload,   size)
        if (downloadPoints.size >= 2) drawSparkSeries(downloadPoints, ChartDownload, size)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkSeries(
    points: List<Float>,
    color: Color,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val maxVal = points.max().coerceAtLeast(1f)
    val w = canvasSize.width
    val h = canvasSize.height
    val stepX = w / (points.size - 1).coerceAtLeast(1)

    val linePath = Path().apply {
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal) * h * 0.9f
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(fillPath, brush = Brush.verticalGradient(
        listOf(color.copy(alpha = 0.35f), Color.Transparent)
    ))
    drawPath(linePath, color = color, style = Stroke(width = 2.dp.toPx()))
}

// ─────────────────────────────────────────────────────────────
// Status badge (dot + label)
// ─────────────────────────────────────────────────────────────
@Composable
fun StatusBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color  = color.copy(alpha = 0.15f),
        shape  = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Canvas(Modifier.size(6.dp)) { drawCircle(color = color) }
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Stat row (label + value)
// ─────────────────────────────────────────────────────────────
@Composable
fun StatRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

// ─────────────────────────────────────────────────────────────
// Password strength meter
// ─────────────────────────────────────────────────────────────
@Composable
fun PasswordStrengthMeter(strength: Float, label: String) {
    val color = when {
        strength < 0.3f -> StatusBlocked
        strength < 0.6f -> StatusIdle
        else            -> StatusActive
    }
    Column {
        LinearProgressIndicator(
            progress    = { strength },
            modifier    = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color       = color,
            trackColor  = SurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

