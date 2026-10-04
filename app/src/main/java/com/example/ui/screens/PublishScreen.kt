package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.data.VideoModel
import com.example.data.PostRepository
import com.example.data.UserRepository
import com.example.data.ProfileResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val postRepository = remember { PostRepository() }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("video") }
    var isStorySelected by remember { mutableStateOf(false) }
    
    var description by remember { mutableStateOf("") }
    var hashtagsText by remember { mutableStateOf("") }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf(0f) }
    
    var currentUserProfile by remember { mutableStateOf<ProfileResponse?>(null) }
    
    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        if (user != null) {
            val result = userRepository.getProfile(user.uid)
            result.onSuccess { currentUserProfile = it }
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Not all pickers support persistable permissions, handled by local copy
            }
            val type = context.contentResolver.getType(uri)
            mediaType = if (type?.startsWith("image") == true) "image" else "video"
            
            if (mediaType == "image") {
                isStorySelected = true
            }
        }
    }

    if (isStorySelected && selectedMediaUri != null) {
        StoryEditorScreen(navController, selectedMediaUri!!, mediaType)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (selectedMediaUri == null) {
            // Select Video State
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Crear contenido",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                Button(
                    onClick = {
                        mediaLauncher.launch("*/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Abrir Galería", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        try {
                            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                            mediaLauncher.launch("video/*") // Fallback if direct capture not used
                        } catch (e: Exception) {
                            Toast.makeText(context, "Usa la galería para mayor compatibilidad.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Grabar Video", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Upload Form State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    IconButton(onClick = { selectedMediaUri = null }, enabled = !isUploading) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                    }
                    Text("Nueva Publicación", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    
                    if (mediaType == "video" && !isUploading) {
                        TextButton(onClick = { isStorySelected = true }) {
                            Text("Hacer Historia", color = Color(0xFF3B82F6))
                        }
                    }
                }

                // Video Preview (mini ExoPlayer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                ) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            repeatMode = Player.REPEAT_MODE_ONE
                        }
                    }
                    
                    LaunchedEffect(selectedMediaUri) {
                        if (selectedMediaUri != null) {
                            val mediaItem = MediaItem.fromUri(selectedMediaUri!!)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    }
                    
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
                            else if (event == Lifecycle.Event.ON_RESUME) exoPlayer.play()
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            exoPlayer.release()
                        }
                    }
                    
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { it.player = exoPlayer }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hashtagsText,
                    onValueChange = { hashtagsText = it },
                    label = { Text("Hashtags (separados por espacio)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uploadError.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(uploadError, color = Color(0xFFEF4444), modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { uploadError = "" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Reintentar", color = Color.White) }
                    }
                } else if (isUploading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subiendo... ${(uploadProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFEF4444),
                            trackColor = Color.DarkGray
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val user = auth.currentUser
                            
                            isUploading = true
                            uploadProgress = 0f
                            
                            val videoId = UUID.randomUUID().toString()
                            val localVideoPath = LocalVideoManager.copyVideoToLocal(context, selectedMediaUri!!)
                            
                            if (localVideoPath != null) {
                                
                                val hashtagsList = hashtagsText.split(" ")
                                    .filter { it.isNotBlank() }
                                    .map { if (it.startsWith("#")) it else "#$it" }
                                
                                val localFileUriString = Uri.fromFile(java.io.File(localVideoPath)).toString()

                                scope.launch {
                                    try {
                                        val result = postRepository.publishVideo(
                                            videoId = videoId,
                                            localUriString = localFileUriString,
                                            description = description,
                                            hashtags = hashtagsList,
                                            username = currentUserProfile?.username ?: user?.displayName?.lowercase()?.replace(" " , "") ?: "invitado",
                                            displayName = currentUserProfile?.displayName ?: user?.displayName ?: "Invitado",
                                            visibility = "PUBLIC" // TODO: Add visibility state
                                        )
                                        
                                        // Show private security review animation
                                        navController.navigate("security_review")

                                        result.onSuccess { publishedVideo ->
                                            isUploading = false
                                            Toast.makeText(context, "¡Video publicado con éxito!", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        }.onFailure {
                                            isUploading = false
                                            uploadError = "Error al publicar: ${it.message}"
                                        }
                                    } catch (e: Exception) {
                                        isUploading = false
                                        uploadError = "Error al publicar: ${e.message}"
                                    }
                                }
                            } else {
                                isUploading = false
                                uploadError = "Error al guardar el video localmente."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Publicar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
