package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.MusicRepository
import com.example.data.PostRepository
import com.example.data.SongItem
import com.example.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(navController: NavController, mediaUri: Uri, mediaType: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val postRepository = remember { PostRepository() }
    val musicRepository = remember { MusicRepository() }
    val userRepository = remember { UserRepository() }

    var isUploading by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showTextEditor by remember { mutableStateOf(false) }
    var showCustomUsersPicker by remember { mutableStateOf(false) }

    var selectedVisibility by remember { mutableStateOf("Todo el mundo") }
    var selectedSound by remember { mutableStateOf<SongItem?>(null) }
    var storyText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var songsList by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isSearchingMusic by remember { mutableStateOf(false) }

    val privacyOptions = listOf("Todo el mundo", "Amigos", "Nadie", "Personalizado")
    val isCustomRingYellow = selectedVisibility == "Personalizado"

    fun searchMusic(q: String) {
        scope.launch {
            isSearchingMusic = true
            val res = musicRepository.searchSongs(q)
            res.onSuccess { songsList = it }
            isSearchingMusic = false
        }
    }

    LaunchedEffect(Unit) {
        searchMusic("trending")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (mediaType == "image") {
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
        }

        if (storyText.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = storyText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            
            Row {
                IconButton(onClick = { showTextEditor = true }, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = { showSoundPicker = true }, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Sound", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = { showPrivacySettings = true }, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.Settings, contentDescription = "Privacy", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedSound != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${selectedSound!!.trackName} - ${selectedSound!!.artistName}", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp).clickable { selectedSound = null })
                    }
                }
            }

            Button(
                onClick = {
                    if (isUploading) return@Button
                    isUploading = true
                    scope.launch {
                        try {
                            val localPath = com.example.ui.screens.LocalVideoManager.copyVideoToLocal(context, mediaUri)
                            var mediaFile: java.io.File? = null
                            var thumbnailFile: java.io.File? = null
                            
                            if (localPath != null) {
                                mediaFile = java.io.File(localPath)
                                if (mediaType == "video") {
                                    val thumbBitmap = com.example.utils.ThumbnailUtils.generateVideoThumbnail(context, Uri.fromFile(mediaFile))
                                    if (thumbBitmap != null) {
                                        thumbnailFile = com.example.utils.ThumbnailUtils.saveBitmapToCache(context, thumbBitmap, "story_thumb_${System.currentTimeMillis()}")
                                    }
                                } else {
                                    // For images, we can just use the same file as thumbnail
                                    thumbnailFile = mediaFile
                                }
                            }
                            
                            val result = postRepository.publishStory(
                                mediaFile = mediaFile,
                                thumbnailFile = thumbnailFile,
                                visibility = selectedVisibility,
                                mediaType = mediaType,
                                soundName = selectedSound?.trackName ?: ""
                            )
                            
                            result.onSuccess {
                                Toast.makeText(context, "¡Historia publicada!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                                navController.popBackStack()
                            }.onFailure { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                isUploading = false
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isCustomRingYellow) Color(0xFFF59E0B) else Color(0xFF3B82F6), contentColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                enabled = !isUploading
            ) {
                if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("PUBLICAR (${if (isCustomRingYellow) "Anillo Amarillo" else "Anillo Azul"})", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tu historia expirará en 24 horas", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        if (showPrivacySettings) {
            ModalBottomSheet(
                onDismissRequest = { showPrivacySettings = false },
                containerColor = Color(0xFF1E1E1E)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("¿Quién puede ver esta historia?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    privacyOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVisibility = option
                                    showPrivacySettings = false
                                    if (option == "Personalizado") {
                                        showCustomUsersPicker = true
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(option, color = if (selectedVisibility == option) Color(0xFF3B82F6) else Color.White, fontSize = 16.sp)
                            if (selectedVisibility == option) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3B82F6))
                            }
                        }
                    }
                }
            }
        }

        if (showSoundPicker) {
            ModalBottomSheet(
                onDismissRequest = { showSoundPicker = false },
                containerColor = Color(0xFF1E1E1E)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp).height(450.dp)) {
                    Text("Añadir Música (API Mundial)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            searchMusic(it)
                        },
                        placeholder = { Text("Buscar canciones, artistas...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isSearchingMusic) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            items(songsList, key = { it.trackId }) { song ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedSound = song
                                        showSoundPicker = false
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(model = song.artworkUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.trackName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(song.artistName, color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF3B82F6))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTextEditor) {
            AlertDialog(
                onDismissRequest = { showTextEditor = false },
                title = { Text("Agregar texto") },
                text = {
                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        placeholder = { Text("Escribe algo...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { showTextEditor = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        if (showCustomUsersPicker) {
            AlertDialog(
                onDismissRequest = { showCustomUsersPicker = false },
                title = { Text("Seleccionar personas (Anillo Amarillo)") },
                text = { Text("Se seleccionaron todas tus conexiones actuales para ver esta historia personalizada con anillo amarillo.") },
                confirmButton = {
                    Button(onClick = { showCustomUsersPicker = false }) {
                        Text("Guardar")
                    }
                }
            )
        }
    }
}
