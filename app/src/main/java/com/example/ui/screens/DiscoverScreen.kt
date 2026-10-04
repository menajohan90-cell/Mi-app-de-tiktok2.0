package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.components.UserAvatar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid

    var searchQuery by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val postRepository = remember { PostRepository() }
    val userRepository = remember { UserRepository() }
    
    var recommendedVideos by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var searchVideoResults by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var searchUserResults by remember { mutableStateOf<List<UserBrief>>(emptyList()) }
    var userConnections by remember { mutableStateOf<List<ConnectionResponse>>(emptyList()) }
    var connectedUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Load initial feed & connections
    fun loadInitialData() {
        scope.launch {
            try {
                // 1. Load active connections
                if (currentUid != null) {
                    val connResult = userRepository.getConnections()
                    connResult.onSuccess { conns ->
                        userConnections = conns
                        connectedUids = conns.mapNotNull { conn ->
                            if (conn.requesterUid == currentUid) conn.targetUid else conn.requesterUid
                        }.toSet()
                    }
                }

                // 2. Load recommended videos
                val result = postRepository.getVideos("RECOMMENDED")
                if (result.isSuccess) {
                    recommendedVideos = result.getOrNull() ?: emptyList()
                } else {
                    isError = true
                    val ex = result.exceptionOrNull()
                    errorMessage = ex?.localizedMessage ?: ex?.message ?: "Error al cargar videos recomendados"
                }
            } catch (e: Exception) {
                isError = true
                errorMessage = e.localizedMessage ?: e.message ?: "Error inesperado"
            }
        }
    }

    LaunchedEffect(currentUid) {
        loadInitialData()
    }

    // Debounced search for real users & videos
    LaunchedEffect(searchQuery) {
        delay(350)
        activeSearch = searchQuery.trim()
        if (activeSearch.isNotEmpty()) {
            isSearching = true
            try {
                // Real search in Firestore
                val userResult = userRepository.searchUsers(activeSearch)
                userResult.onSuccess { searchUserResults = it }

                // Search videos
                val videoResult = postRepository.getVideos("SEARCH_$activeSearch")
                if (videoResult.isSuccess) {
                    searchVideoResults = videoResult.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.w("DiscoverScreen", "Search error: ${e.message}")
            } finally {
                isSearching = false
            }
        } else {
            searchUserResults = emptyList()
            searchVideoResults = emptyList()
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar usuarios o videos...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF333333),
                unfocusedBorderColor = Color(0xFF222222),
                focusedContainerColor = Color(0xFF141414),
                unfocusedContainerColor = Color(0xFF141414)
            ),
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true
        )

        if (activeSearch.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Sección: Conexiones activas
                if (userConnections.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tus conexiones",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(userConnections) { conn ->
                                val otherUser = if (conn.requesterUid == currentUid) conn.target else conn.requester
                                if (otherUser != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable {
                                                navController.navigate("profile/${otherUser.uid}")
                                            }
                                    ) {
                                        UserAvatar(
                                            photoUrl = otherUser.photoUrl,
                                            size = 56.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = otherUser.username,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Sección: Tendencias de videos
                item {
                    Text(
                        text = "Tendencias",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (isError) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(errorMessage, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { loadInitialData() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                } else {
                    val chunked = recommendedVideos.chunked(3)
                    items(chunked) { rowVideos ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowVideos.forEach { video ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(9f / 16f)
                                        .padding(1.dp)
                                        .background(Color.DarkGray)
                                        .clickable { navController.navigate("inicio") }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(video.videoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Miniatura",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${video.views}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowVideos.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // Active Search Results
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                } else {
                    if (searchUserResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Personas",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        items(searchUserResults, key = { it.uid }) { user ->
                            val isConnected = connectedUids.contains(user.uid)

                            UserSearchItem(
                                user = user,
                                isConnected = isConnected,
                                onUserClick = {
                                    if (auth.currentUser == null) {
                                        Toast.makeText(context, "Inicia sesión para ver perfiles", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login")
                                    } else {
                                        navController.navigate("profile/${user.uid}")
                                    }
                                },
                                onConnectToggle = {
                                    if (auth.currentUser == null) {
                                        Toast.makeText(context, "Inicia sesión para conectar", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login")
                                        return@UserSearchItem
                                    }

                                    scope.launch {
                                        if (isConnected) {
                                            // Disconnect
                                            val res = userRepository.disconnectUser(user.uid)
                                            res.onSuccess {
                                                connectedUids = connectedUids - user.uid
                                                userConnections = userConnections.filterNot { 
                                                    (it.requesterUid == user.uid || it.targetUid == user.uid)
                                                }
                                                Toast.makeText(context, "Desconectado de @${user.username}", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                Toast.makeText(context, "Error al desconectar", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            // Connect
                                            val res = userRepository.connectUser(user.uid)
                                            res.onSuccess {
                                                connectedUids = connectedUids + user.uid
                                                Toast.makeText(context, "¡Conectado con @${user.username}!", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else if (searchVideoResults.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No se encontraron resultados para '$activeSearch'", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }

                    if (searchVideoResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Videos",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        val chunked = searchVideoResults.chunked(3)
                        items(chunked) { rowVideos ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowVideos.forEach { video ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(9f / 16f)
                                            .padding(1.dp)
                                            .background(Color.DarkGray)
                                            .clickable { navController.navigate("inicio") }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(video.videoUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Miniatura",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                repeat(3 - rowVideos.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimalist user item compliant with requirement:
 * [Foto de perfil] [Nombre de usuario] [Conectar / Conectado]
 */
@Composable
fun UserSearchItem(
    user: UserBrief,
    isConnected: Boolean,
    onUserClick: () -> Unit,
    onConnectToggle: () -> Unit
) {
    var showDisconnectMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [Foto]
        UserAvatar(
            photoUrl = user.photoUrl,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // [Nombre de usuario]
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${user.username}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.displayName.isNotBlank() && !user.displayName.equals(user.username, ignoreCase = true)) {
                Text(
                    text = user.displayName,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // [Botón Conectar / Conectado / Desconectar]
        if (isConnected) {
            Box {
                OutlinedButton(
                    onClick = { showDisconnectMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Conectado", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                DropdownMenu(
                    expanded = showDisconnectMenu,
                    onDismissRequest = { showDisconnectMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Desconectar", color = Color(0xFFEF4444), fontSize = 13.sp) },
                        onClick = {
                            showDisconnectMenu = false
                            onConnectToggle()
                        }
                    )
                }
            }
        } else {
            Button(
                onClick = onConnectToggle,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
