package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.PostRepository
import com.example.data.StoryResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewScreen(navController: NavController, storyId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRepository = remember { PostRepository() }
    
    var stories by remember { mutableStateOf<List<StoryResponse>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(storyId) {
        postRepository.getStories().onSuccess { list ->
            stories = list
            currentIndex = list.indexOfFirst { it.storyId == storyId }.coerceAtLeast(0)
            isLoading = false
        }.onFailure {
            navController.popBackStack()
        }
    }

    val currentStory = stories.getOrNull(currentIndex)

    LaunchedEffect(currentStory, isPaused) {
        if (currentStory != null && !isPaused) {
            progress.snapTo(0f)
            
            // Mark as viewed
            scope.launch {
                postRepository.markStoryAsViewed(currentStory.storyId)
            }

            if (currentStory.mediaType == "video") {
                exoPlayer.setMediaItem(MediaItem.fromUri(currentStory.mediaUrl))
                exoPlayer.prepare()
                exoPlayer.play()
                
                // Wait for video to end or progress to finish
                // For simplicity, we use a fixed time or video duration
                // We'll sync progress with time
                val duration = 15000L // Max 15s for story
                progress.animateTo(1f, tween(duration.toInt(), easing = LinearEasing))
            } else {
                progress.animateTo(1f, tween(5000, easing = LinearEasing))
            }
            
            // Auto next
            if (currentIndex < stories.size - 1) {
                currentIndex++
            } else {
                navController.popBackStack()
            }
        } else if (isPaused) {
            exoPlayer.pause()
            progress.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (currentStory == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPaused = true
                    tryAwaitRelease()
                    isPaused = false
                },
                onTap = { offset ->
                    val width = size.width
                    if (offset.x < width / 3) {
                        // Previous
                        if (currentIndex > 0) currentIndex-- else navController.popBackStack()
                    } else {
                        // Next
                        if (currentIndex < stories.size - 1) currentIndex++ else navController.popBackStack()
                    }
                }
            )
        }
    ) {
        // Media Content
        if (currentStory.mediaType == "image") {
            AsyncImage(
                model = currentStory.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Progress Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                val itemProgress = when {
                    index < currentIndex -> 1f
                    index == currentIndex -> progress.value
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(itemProgress)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }

        // Header Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.components.UserAvatar(photoUrl = null, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(currentStory.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Hace unos momentos", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
