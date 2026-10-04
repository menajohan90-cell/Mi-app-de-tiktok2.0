package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ChatBrief
import com.example.data.ChatRepository
import com.example.data.PostRepository
import com.example.data.StoryResponse
import com.example.ui.components.UserAvatar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatsScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val chatRepository = remember { ChatRepository() }
    val postRepository = remember { PostRepository() }
    val scope = rememberCoroutineScope()
    
    var chats by remember { mutableStateOf<List<ChatBrief>>(emptyList()) }
    var userOnlineMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    
    var showLinkDialog by remember { mutableStateOf(false) }
    var pastedLink by remember { mutableStateOf("") }
    
    var showStoryDialog by remember { mutableStateOf(false) }
    var storyPrivacy by remember { mutableStateOf("Todo el mundo") }
    var storyMediaUri by remember { mutableStateOf<Uri?>(null) }
    var stories by remember { mutableStateOf<List<StoryResponse>>(emptyList()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            storyMediaUri = uri
            showStoryDialog = true
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val storyResult = postRepository.getStories()
                if (storyResult.isSuccess) {
                    stories = storyResult.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }
    
    LaunchedEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        
        while (true) {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val chatResult = chatRepository.getChats()
                    chatResult.onSuccess { list ->
                        chats = list
                        isLoading = false
                        isError = false
                        
                        // Consultar presencia real de los contactos
                        val oMap = mutableMapOf<String, Boolean>()
                        for (c in list) {
                            val targetId = c.otherUser?.uid ?: ""
                            if (targetId.isNotBlank()) {
                                try {
                                    val doc = FirebaseFirestore.getInstance().collection("users").document(targetId).get().await()
                                    if (doc.exists()) {
                                        val online = doc.getBoolean("isOnline") ?: false
                                        val active = doc.getLong("lastActive") ?: 0L
                                        oMap[targetId] = online && (System.currentTimeMillis() - active < 90000)
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                        userOnlineMap = oMap
                    }.onFailure {
                        if (chats.isEmpty()) {
                            isLoading = false
                            isError = true
                        }
                    }
                } catch (e: Exception) {
                    if (chats.isEmpty()) {
                        isLoading = false
                        isError = true
                    }
                }
            } else {
                isLoading = false
            }
            delay(3500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading && chats.isEmpty()) {
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (isError && chats.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Error de conexión", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        isLoading = true
                        isError = false
                        scope.launch {
                            chatRepository.getChats().onSuccess {
                                chats = it
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Reintentar")
                }
            }
        } else if (currentUser == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(photoUrl = null, size = 64.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Bandeja de entrada", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Inicia sesión para ver tus chats y mensajes", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {

                // --- 3 Tarjetas Fijas Modernizadas ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("notifications/system") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Sistema", tint = Color(0xFF3B82F6), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Sistema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Denuncias y notificaciones oficiales", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("notifications/connections") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = "Conexiones", tint = Color(0xFF10B981), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Nuevas conexiones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Solicitudes y personas que conectaron contigo", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("notifications/novedades") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Novedades", tint = Color(0xFFEF4444), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Novedades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Me gusta, comentarios e interacciones", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLinkDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Link", tint = Color(0xFF8B5CF6), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Abrir chat compartido", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Leer conversación respaldada mediante enlace", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF1E1E1E))
                }

                // Fila de Historias
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Historias",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clickable { galleryLauncher.launch("*/*") },
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        UserAvatar(
                                            photoUrl = currentUser?.photoUrl?.toString(),
                                            size = 58.dp,
                                            borderWidth = 2.dp,
                                            borderColor = Color(0xFF3B82F6)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3B82F6))
                                                .border(2.dp, Color.Black, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tu historia", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            
                            items(stories) { story ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .border(2.5.dp, Color(0xFF3B82F6), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = story.mediaUrl,
                                            contentDescription = "Historia de ${story.username}",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(story.username.ifBlank { "Historia" }, color = Color(0xFFA1A1AA), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Mensajes",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                val sortedChats = chats.sortedByDescending { it.lastMessageAt }
                items(sortedChats, key = { it.chatId }) { chat ->
                    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val otherUid = chat.otherUser?.uid ?: ""
                    val targetChatId = if (chat.chatId.contains("_")) {
                        chat.chatId
                    } else if (myUid.isNotBlank() && otherUid.isNotBlank()) {
                        if (myUid < otherUid) "${myUid}_$otherUid" else "${otherUid}_$myUid"
                    } else {
                        otherUid
                    }
                    val isPartnerOnline = userOnlineMap[otherUid] ?: false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController?.navigate("chat_detail/$targetChatId")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar con icono de usuario y punto de presencia
                        UserAvatar(
                            photoUrl = chat.otherUser?.photoUrl,
                            size = 52.dp,
                            isOnline = isPartnerOnline
                        )
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chat.otherUser?.displayName?.ifBlank { null } ?: chat.otherUser?.username ?: "Usuario", 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color.White
                                )
                                if (chat.lastMessageAt > 0) {
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastMessageAt))
                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = chat.lastMessage, 
                                fontSize = 13.sp, 
                                color = Color(0xFFA1A1AA),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                if (chats.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(42.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Aún no tienes conversaciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Explora perfiles y conecta con amigos para chatear en tiempo real", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        
        if (showLinkDialog) {
            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = { Text("Abrir Enlace Compartido") },
                text = {
                    Column {
                        Text("Escribe o pega el link para ver el chat respaldado en modo lectura.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pastedLink,
                            onValueChange = { pastedLink = it },
                            label = { Text("URL del enlace") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            var payload = pastedLink.substringAfterLast("/")
                            if (payload.contains("#")) {
                                payload = payload.substringAfterLast("/")
                            }
                            if (payload.isNotEmpty()) {
                                navController?.navigate("shared_chat_offline/$payload")
                            }
                            showLinkDialog = false
                            pastedLink = ""
                        }
                    ) {
                        Text("Abrir", color = Color(0xFF10B981))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1F2937),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        if (showStoryDialog) {
            AlertDialog(
                onDismissRequest = { showStoryDialog = false },
                title = { Text("Publicar Historia (24h)", color = Color.White) },
                text = {
                    Column {
                        Text("Tu historia desaparecerá automáticamente después de 24 horas.", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Privacidad", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Todo el mundo", "Amigos", "Solo yo", "Personalizado").forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { storyPrivacy = option }.padding(vertical = 4.dp)) {
                                RadioButton(
                                    selected = storyPrivacy == option,
                                    onClick = { storyPrivacy = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981), unselectedColor = Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (storyMediaUri == null) {
                                Toast.makeText(context, "Debes seleccionar un archivo.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val s = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                            s.launch {
                                try {
                                    val localPath = com.example.ui.screens.LocalVideoManager.copyVideoToLocal(context, storyMediaUri!!)
                                    var mediaFile: java.io.File? = null
                                    if (localPath != null) {
                                        mediaFile = java.io.File(localPath)
                                    }
                                    
                                    postRepository.publishStory(
                                        mediaFile = mediaFile,
                                        thumbnailFile = mediaFile,
                                        visibility = storyPrivacy,
                                        mediaType = "image",
                                        soundName = ""
                                    )
                                    
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "Historia publicada correctamente.", Toast.LENGTH_SHORT).show()
                                        showStoryDialog = false
                                        storyMediaUri = null
                                        scope.launch {
                                            try {
                                                val storyResult = postRepository.getStories()
                                                if (storyResult.isSuccess) {
                                                    stories = storyResult.getOrNull() ?: emptyList()
                                                }
                                            } catch (e: Exception) {}
                                        }
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Publicar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStoryDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1F2937),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}
