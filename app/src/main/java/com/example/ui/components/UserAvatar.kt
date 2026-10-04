package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun UserAvatar(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    isOnline: Boolean? = null,
    contentDescription: String? = "Perfil de usuario"
) {
    var isImageError by remember(photoUrl) { mutableStateOf(false) }
    val validUrl = remember(photoUrl) {
        val trimmed = photoUrl?.trim()
        if (trimmed.isNullOrBlank() || trimmed == "null") null else trimmed
    }

    val baseModifier = if (borderWidth > 0.dp) {
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
    } else {
        Modifier
            .size(size)
            .clip(CircleShape)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = baseModifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3A3A3C), Color(0xFF242426))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (validUrl != null && !isImageError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(validUrl)
                        .crossfade(true)
                        .listener(
                            onError = { _, _ -> isImageError = true },
                            onSuccess = { _, _ -> isImageError = false }
                        )
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isImageError = true }
                )
            } else {
                // Icono humano/persona inequívoco
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription,
                    tint = Color(0xFFD1D1D6),
                    modifier = Modifier.size(size * 0.58f)
                )
            }
        }

        // Indicador de presencia en tiempo real
        if (isOnline != null) {
            val dotSize = (size * 0.28f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF10B981) else Color.White)
                    .border(1.5.dp, Color.Black, CircleShape)
            )
        }
    }
}
