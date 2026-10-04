package com.example.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SvgFlag(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.1f)
            lineTo(size.width * 0.85f, size.height * 0.4f)
            lineTo(size.width * 0.2f, size.height * 0.7f)
            close()
        }
        drawPath(path, color = tint)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.2f, size.height * 0.1f),
            end = Offset(size.width * 0.2f, size.height * 0.95f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun SvgScanning(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeW = 2.dp.toPx()
        // Outer box corners or circle
        drawCircle(color = tint, radius = size.width * 0.35f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = strokeW))
        drawCircle(color = tint, radius = size.width * 0.15f, center = Offset(size.width * 0.5f, size.height * 0.5f))
    }
}

@Composable
fun SvgSuccess(modifier: Modifier = Modifier, tint: Color = Color(0xFF10B981)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.55f)
            lineTo(size.width * 0.45f, size.height * 0.75f)
            lineTo(size.width * 0.8f, size.height * 0.3f)
        }
        drawPath(path, color = tint, style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
fun SvgError(modifier: Modifier = Modifier, tint: Color = Color(0xFFEF4444)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeW = 2.5.dp.toPx()
        drawLine(color = tint, start = Offset(size.width * 0.25f, size.height * 0.25f), end = Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = strokeW)
        drawLine(color = tint, start = Offset(size.width * 0.75f, size.height * 0.25f), end = Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = strokeW)
    }
}

@Composable
fun SvgInfo(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        drawCircle(color = tint, radius = size.width * 0.08f, center = Offset(size.width * 0.5f, size.height * 0.28f))
        drawRect(color = tint, topLeft = Offset(size.width * 0.42f, size.height * 0.42f), size = androidx.compose.ui.geometry.Size(size.width * 0.16f, size.height * 0.45f))
    }
}

@Composable
fun SvgShield(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.1f)
            lineTo(size.width * 0.85f, size.height * 0.25f)
            lineTo(size.width * 0.85f, size.height * 0.55f)
            quadraticBezierTo(size.width * 0.85f, size.height * 0.85f, size.width * 0.5f, size.height * 0.95f)
            quadraticBezierTo(size.width * 0.15f, size.height * 0.85f, size.width * 0.15f, size.height * 0.55f)
            lineTo(size.width * 0.15f, size.height * 0.25f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx()))
    }
}
