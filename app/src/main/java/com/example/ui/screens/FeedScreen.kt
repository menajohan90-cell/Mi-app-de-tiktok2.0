package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.UserAvatar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.PostRepository
import com.example.data.UserRepository
import com.example.data.VideoModel
import com.example.data.CommentResponse
import com.example.utils.AppIcons
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun FeedScreen(navController: androidx.navigation.NavController, feedType: String = "FYP") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isAuthRestored by remember { mutableStateOf(false) }

    var currentErrorMessage by remember { mutableStateOf("") }
    val postRepository = remember { PostRepository() }

    fun loadVideos() {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            isLoading = false
            videos = emptyList()
            isError = false
            return
        }

        scope.launch {
            isLoading = true
            isError = false
            currentErrorMessage = ""
            android.util.Log.d("FeedScreen", "Iniciando carga de feed: $feedType para usuario: ${user.uid}")
            try {
                val result = postRepository.getVideos(feedType)
                if (result.isSuccess) {
                    videos = result.getOrNull() ?: emptyList()
                    isError = false
                } else {
                    val exception = result.exceptionOrNull()
                    val rawMsg = exception?.localizedMessage ?: exception?.message ?: ""
                    if (rawMsg.startsWith("AUTH_REQUIRED")) {
                        isError = false
                        videos = emptyList()
                    } else {
                        currentErrorMessage = when (exception) {
                            is com.google.firebase.FirebaseNetworkException ->
                                "Error de red. Verifica tu conexión a internet o datos móviles."
                            is com.google.firebase.firestore.FirebaseFirestoreException -> {
                                when (exception.code) {
                                    com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                        "Firebase rechazó la lectura por permisos. Verifica que tu cuenta tenga acceso o que las reglas de seguridad de Firestore permitan leer videos públicos a usuarios autenticados."
                                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                                        "Servicio de base de datos no disponible temporalmente (UNAVAILABLE)."
                                    com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                                        "Falta índice compuesto en Firebase Firestore (FAILED_PRECONDITION)."
                                    else -> "Error de base de datos: ${exception.code} ($rawMsg)"
                                }
                            }
                            is java.net.UnknownHostException -> "No se pudo resolver el host del servidor (DNS)."
                            is java.net.SocketTimeoutException -> "Tiempo de espera agotado al conectar con el servidor."
                            is java.net.ConnectException -> "No se pudo conectar al servidor."
                            else -> if (rawMsg.isNotEmpty()) rawMsg else "Error al cargar publicaciones."
                        }
                        isError = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("FeedScreen", "Excepción al cargar videos: ${e.message}")
                isError = true
                currentErrorMessage = "Error inesperado: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            isAuthRestored = true
            if (currentUser != null && !currentUser!!.isAnonymous) {
                loadVideos()
            } else {
                isLoading = false
                videos = emptyList()
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(feedType) {
        if (isAuthRestored && currentUser != null && !currentUser!!.isAnonymous) {
            loadVideos()
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (isError) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isLoggedOut = currentUser == null || currentUser.isAnonymous
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp), 
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (currentErrorMessage.isNotBlank()) currentErrorMessage else "No se pudo conectar al servidor",
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (isLoggedOut && currentErrorMessage.contains("iniciar sesión", ignoreCase = true)) {
                    Button(
                        onClick = { navController.navigate("login") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Iniciar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Crear Cuenta", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { loadVideos() }) {
                        Text("Reintentar", color = Color.Gray)
                    }
                } else {
                    Button(
                        onClick = { loadVideos() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Reintentar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (videos.isEmpty()) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isLoggedOut = currentUser == null || currentUser.isAnonymous

        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp), 
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoggedOut) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "¡Bienvenido a Momentos!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inicia sesión o crea una cuenta para ver las publicaciones en Para Ti y conectar con amigos.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { navController.navigate("login") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Iniciar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Crear Cuenta", color = Color.White)
                    }
                } else {
                    Icon(
                        imageVector = if (feedType == "FYP") Icons.Default.PlayArrow else Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (feedType == "FYP") 
                            "Publicar un video ahora puede hacerte viral porque no hay videos y en momentos solo dura 24 horas, ¡recuérdalo!"
                            else "Dile a tus amigos que compartan un video, pueden hacerte viral o ¡tú mismo publica!",
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("publish") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text(if (feedType == "FYP") "Publicar un video ahora" else "Publicar un video", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { videos.size })

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
                .background(Color.Black)
        ) { page ->
            val video = videos[page]
            val isPlaying = pagerState.currentPage == page
            VideoPlayerItem(videoModel = video, isPlaying = isPlaying, navController = navController, onVideoUpdated = { updatedVideo -> videos = videos.map { if (it.videoId == updatedVideo.videoId) updatedVideo else it } })
        }
    }
}


fun formatCount(count: Int): String {
    if (count <= 0) return "0"
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> {
            val kValue = count / 1000f
            if (kValue >= 10) {
                "${kValue.toInt()}K"
            } else {
                String.format("%.1fK", kValue).replace(",", ".").replace(".0", "")
            }
        }
        else -> {
            val mValue = count / 1000000f
            if (mValue >= 10) {
                "${mValue.toInt()}M"
            } else {
                String.format("%.1fM", mValue).replace(",", ".").replace(".0", "")
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerItem(videoModel: VideoModel, isPlaying: Boolean, navController: androidx.navigation.NavController, onVideoUpdated: (VideoModel) -> Unit = {}) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid ?: ""

    val safeVideoId = remember(videoModel.videoId) {
        if (videoModel.videoId.contains("/")) "local_" + videoModel.videoId.hashCode()
        else videoModel.videoId
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    
    val postRepository = remember { PostRepository() }
    
    var isLiked by remember(safeVideoId) { mutableStateOf(videoModel.isLiked) }
    var likesCount by remember(safeVideoId) { mutableStateOf(videoModel.likesCount) }
    
    var showComments by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    // Like Animation
    val scaleA = remember { Animatable(1f) }

    LaunchedEffect(safeVideoId) {
        if (currentUid.isNotEmpty() && safeVideoId.isNotEmpty()) {
            // Check like state via API if needed, for now we assume false until first toggle
            // or we could fetch video details including user-specific like state.
        }
    }

    var isMediaAccessible by remember(videoModel.videoUrl) { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.w("VideoPlayerItem", "Error de reproducción ExoPlayer: ${error.message}")
                isMediaAccessible = false
            }
        }
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
        }
    }

    LaunchedEffect(videoModel.videoUrl) {
        if (videoModel.videoUrl.isNotEmpty()) {
            var canOpen = true
            if (videoModel.videoUrl.startsWith("content://")) {
                try {
                    val parsed = Uri.parse(videoModel.videoUrl)
                    val pfd = context.contentResolver.openAssetFileDescriptor(parsed, "r")
                    pfd?.close()
                } catch (e: SecurityException) {
                    android.util.Log.w("VideoPlayerItem", "Sin permiso para URI de contenido: ${videoModel.videoUrl}")
                    canOpen = false
                    isMediaAccessible = false
                } catch (e: Exception) {
                    android.util.Log.w("VideoPlayerItem", "No se puede abrir URI: ${videoModel.videoUrl}")
                    canOpen = false
                    isMediaAccessible = false
                }
            } else if (videoModel.videoUrl.startsWith("file://") || videoModel.videoUrl.startsWith("/")) {
                val filePath = if (videoModel.videoUrl.startsWith("file://")) Uri.parse(videoModel.videoUrl).path ?: "" else videoModel.videoUrl
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    android.util.Log.w("VideoPlayerItem", "Archivo local no encontrado: $filePath")
                    canOpen = false
                    isMediaAccessible = false
                }
            }
            if (canOpen) {
                try {
                    val mediaItem = MediaItem.fromUri(videoModel.videoUrl)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                } catch (e: Exception) {
                    android.util.Log.w("VideoPlayerItem", "Error al inicializar medio: ${e.message}")
                    isMediaAccessible = false
                }
            }
        } else {
            isMediaAccessible = false
        }
    }

    LaunchedEffect(isPlaying, isMediaAccessible) {
        if (isPlaying && isMediaAccessible) {
            exoPlayer.play()
            // Record view after 2 seconds of playback
            scope.launch {
                delay(2000)
                if (exoPlayer.isPlaying) {
                    try {
                        val result = postRepository.recordVideoView(safeVideoId)
                        result.onSuccess { newViews ->
                            onVideoUpdated(videoModel.copy(views = newViews))
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FeedScreen", "Error recording view: ${e.message}")
                    }
                }
            }
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(lifecycleOwner, isMediaAccessible) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME && isPlaying && isMediaAccessible) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // --- Secure Action Guard ---
    fun runSecureAction(action: () -> Unit) {
        if (currentUid.isEmpty()) {
            Toast.makeText(context, "Inicia sesión para interactuar", Toast.LENGTH_SHORT).show()
            navController.navigate("login")
            return
        }
        action()
    }

    var isPlayerReady by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) isPlayerReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMediaAccessible) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { it.player = exoPlayer },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    runSecureAction {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        scope.launch {
                                            try {
                                                scaleA.animateTo(1.2f, tween(100))
                                                scaleA.animateTo(1f, tween(100))
                                                
                                                val result = postRepository.toggleLikeVideo(safeVideoId, isLiked)
                                                result.onSuccess { 
                                                    isLiked = it.isLiked
                                                    likesCount = it.likesCount
                                                    onVideoUpdated(videoModel.copy(isLiked = it.isLiked, likesCount = it.likesCount))
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("FeedScreen", "Error toggling like", e)
                                            }
                                        }
                                    }
                                },
                                onTap = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            )
                        }
                )

                // Thumbnail Overlay (Source of Truth)
                if (!isPlayerReady || !exoPlayer.isPlaying) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(if (videoModel.thumbnailURL.isNotBlank()) videoModel.thumbnailURL else videoModel.videoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Video en procesamiento o no disponible en este dispositivo.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // UI Overlay: Right buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clickable { navController.navigate("profile/${videoModel.uid}") },
                contentAlignment = Alignment.TopCenter
            ) {
                UserAvatar(
                    photoUrl = null,
                    size = 48.dp,
                    borderWidth = 1.5.dp,
                    borderColor = Color.White
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(20.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(2.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Seguir", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            
            // Like Button
            InteractionButton(
                icon = AppIcons.Heart,
                text = formatCount(likesCount),
                tint = if (isLiked) Color.Red else Color.White,
                onClick = {
                    runSecureAction {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        scope.launch {
                            try {
                                scaleA.animateTo(1.2f, tween(100))
                                scaleA.animateTo(1f, tween(100))
                                val result = postRepository.toggleLikeVideo(safeVideoId, isLiked)
                                result.onSuccess { 
                                    isLiked = it.isLiked
                                    likesCount = it.likesCount
                                                    onVideoUpdated(videoModel.copy(isLiked = it.isLiked, likesCount = it.likesCount))
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FeedScreen", "Error toggling like", e)
                            }
                        }
                    }
                }
            )
            
            InteractionButton(icon = AppIcons.Comment, text = formatCount(videoModel.commentsCount)) {
                runSecureAction { showComments = true }
            }
            InteractionButton(icon = AppIcons.Share, text = "Compartir") {
                runSecureAction { showShare = true }
            }
            InteractionButton(icon = AppIcons.Report, text = "Reportar") {
                runSecureAction { showReport = true }
            }
    }

    // UI Overlay: Left info
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 96.dp, start = 16.dp)
            .fillMaxWidth(0.75f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "@${videoModel.username.ifEmpty { "usuario" }}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF60A5FA), // blue-400
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = videoModel.description.ifEmpty { "Video de Momentos #momentos" },
            color = Color(0xFFE5E5E5), // neutral-200
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Audio Pill
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Audio",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Audio original",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }

    if (showComments) {
        ModalBottomSheet(
            onDismissRequest = { showComments = false },
            containerColor = Color(0xFF121212),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            CommentsSection(videoModel, currentUid, postRepository)
        }
    }

    if (showShare) {
        ModalBottomSheet(
            onDismissRequest = { showShare = false },
            containerColor = Color(0xFF121212),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            ShareSection(videoModel, currentUid, postRepository, onDismiss = { showShare = false })
        }
    }

    if (showReport) {
        ModalBottomSheet(
            onDismissRequest = { showReport = false },
            containerColor = Color(0xFF121212),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            ReportSection(videoModel, currentUid, postRepository, onDismiss = { showReport = false })
        }
    }
}
}

@Composable
fun CommentsSection(videoModel: VideoModel, currentUid: String, repository: PostRepository) {
    val safeVideoId = videoModel.videoId
    val videoOwnerId = videoModel.uid
    var comments by remember { mutableStateOf<List<CommentResponse>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentResponse?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(safeVideoId) {
        try {
            val result = repository.getComments(safeVideoId)
            result.onSuccess { 
                comments = it
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedScreen", "Error fetching comments", e)
        }
    }

    val submitComment = {
        if (inputText.isNotBlank() && currentUid.isNotEmpty()) {
            val currentText = inputText
            val parentId = replyingTo?.commentId
            inputText = ""
            replyingTo = null
            scope.launch {
                try {
                    val result = repository.addComment(safeVideoId, currentText, parentId)
                    result.onSuccess { newComment ->
                        // Refresh comments or append locally
                        scope.launch {
                            try {
                                val fetchResult = repository.getComments(safeVideoId)
                                fetchResult.onSuccess { comments = it }
                            } catch (e: Exception) {
                                android.util.Log.e("FeedScreen", "Error refreshing comments", e)
                            }
                        }
                    }.onFailure {
                        Toast.makeText(context, "Error al comentar", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FeedScreen", "Exception adding comment", e)
                    Toast.makeText(context, "Error de red al comentar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(Color(0xFF161616))) {
        Text(
            "Comentarios", 
            color = Color.White, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(16.dp),
            fontSize = 18.sp
        )
        
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(comments) { comment ->
                CommentItem(comment, videoOwnerId, repository, onReply = { replyingTo = it })
            }
        }

        AnimatedVisibility(visible = replyingTo != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF262626)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Respondiendo a @${replyingTo?.username}", color = Color.Gray, fontSize = 12.sp)
                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, tint = Color.Gray, contentDescription = "Cancelar respuesta")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Escribe un comentario...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = { submitComment() }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { submitComment() }) {
                Icon(androidx.compose.material.icons.Icons.Default.Send, tint = Color(0xFF10B981), contentDescription = "Enviar")
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentResponse, 
    videoOwnerId: String, 
    repository: PostRepository,
    onReply: (CommentResponse) -> Unit
) {
    var isLiked by remember(comment.commentId) { mutableStateOf(comment.isLiked) }
    var likesCount by remember(comment.commentId) { mutableStateOf(comment.likesCount) }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = if (comment.userProfilePic.isNotEmpty()) comment.userProfilePic else "https://ui-avatars.com/api/?name=${comment.username}",
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("@${comment.username}")
                        if (comment.uid == videoOwnerId) {
                            withStyle(SpanStyle(color = Color(0xFF10B981), fontWeight = FontWeight.Bold)) {
                                append(" · CREADOR")
                            }
                        }
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(comment.text, color = Color.White, fontSize = 14.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "Reciente",
                        color = Color.DarkGray, 
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Responder", 
                        color = Color.Gray, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onReply(comment) }
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                scope.launch {
                    val result = if (isLiked) repository.unlikeComment(comment.commentId) else repository.likeComment(comment.commentId)
                    if (result.isSuccess) {
                        isLiked = result.getOrNull()!!.isLiked
                        likesCount = result.getOrNull()!!.likesCount
                    }
                }
            }) {
                Icon(
                    imageVector = AppIcons.Heart, 
                    contentDescription = null, 
                    tint = if (isLiked) Color.Red else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(formatCount(likesCount), color = Color.DarkGray, fontSize = 10.sp)
            }
        }
        
        // Render replies
        if (comment.replies.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 48.dp, top = 8.dp)) {
                comment.replies.forEach { reply ->
                    Row(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                            Text(reply.username.take(1).uppercase(), color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("@${reply.username}", color = Color.Gray, fontSize = 11.sp)
                            Text(reply.text, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShareSection(videoModel: VideoModel, currentUid: String, repository: PostRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var friends by remember { mutableStateOf<List<com.example.data.UserBrief>>(emptyList()) }
    var selectedFriends by remember { mutableStateOf<Set<String>>(emptySet()) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isSharing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            val result = userRepository.searchUsers(searchQuery)
            result.onSuccess { friends = it }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(Color(0xFF161616))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Compartir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = onDismiss) {
                Icon(androidx.compose.material.icons.Icons.Default.Close, tint = Color.White, contentDescription = "Close")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar amigos...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981)
            ),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShareOption(androidx.compose.material.icons.Icons.Default.Add, "Historia", Color(0xFF3B82F6)) {
                scope.launch {
                    repository.sharePost(videoModel.videoId, "STORY")
                    Toast.makeText(context, "Compartido en historia", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }
            ShareOption(androidx.compose.material.icons.Icons.Default.Person, "Perfil", Color(0xFF10B981)) {
                scope.launch {
                    repository.sharePost(videoModel.videoId, "PROFILE")
                    Toast.makeText(context, "Compartido en perfil", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }
            ShareOption(androidx.compose.material.icons.Icons.Default.Share, "Enlace", Color.DarkGray) {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://momentos.app/v/${videoModel.videoId}"))
                Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(friends) { friend ->
                val isSelected = selectedFriends.contains(friend.uid)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedFriends = if (isSelected) selectedFriends - friend.uid else selectedFriends + friend.uid
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                        Text(friend.username.take(1).uppercase(), color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(friend.displayName, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("@${friend.username}", color = Color.Gray, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Icon(androidx.compose.material.icons.Icons.Default.CheckCircle, tint = Color(0xFF10B981), contentDescription = null)
                    }
                }
            }
        }

        if (selectedFriends.isNotEmpty()) {
            Button(
                onClick = {
                    scope.launch {
                        isSharing = true
                        repository.sharePost(videoModel.videoId, "FRIENDS", selectedFriends.toList())
                        Toast.makeText(context, "Video compartido", Toast.LENGTH_SHORT).show()
                        isSharing = false
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Compartir con ${selectedFriends.size} amigos")
            }
        }
    }
}

@Composable
fun ShareOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun ReportSection(videoModel: VideoModel, currentUid: String, repository: PostRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isReporting by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val reportOptions = listOf(
        "Spam", "Contenido ofensivo", "Acoso", "Suplantación", "Contenido sexual",
        "Violencia", "Discurso de odio", "Contenido peligroso", "Desinformación",
        "Cuenta falsa", "Fraude", "Copyright", "Contenido inapropiado", "Otro"
    )

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(Color(0xFF161616))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reportar Contenido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = onDismiss) {
                Icon(androidx.compose.material.icons.Icons.Default.Close, tint = Color.White, contentDescription = "Close")
            }
        }
        
        if (selectedOption == null) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(reportOptions) { option ->
                    Text(
                        option, 
                        color = Color.White, 
                        modifier = Modifier.fillMaxWidth().clickable { selectedOption = option }.padding(16.dp)
                    )
                    androidx.compose.material3.HorizontalDivider(color = Color.DarkGray)
                }
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Motivo: $selectedOption", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Detalles adicionales...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEF4444)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isReporting = true
                            repository.reportPost(videoModel.videoId, selectedOption!!, description)
                            Toast.makeText(context, "Denuncia enviada correctamente", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Enviar Reporte")
                }
            }
        }
    }
}

@Composable
fun InteractionButton(
    icon: ImageVector, 
    text: String, 
    tint: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(text = text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
