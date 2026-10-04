package com.example.ui.screens

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.border as composeBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.animation.core.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.Share
import com.example.data.VideoModel
import coil.compose.AsyncImage
import com.example.auth.AuthResult
import com.example.auth.GoogleAuthService
import com.example.ui.components.UserAvatar
import com.example.utils.UsernameValidator
import com.example.data.PostRepository
import com.example.data.UserRepository
import com.example.data.ProfileResponse
import com.example.data.ConnectionResponse
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.utils.AppIcons
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class ProfileState {
    data class Loading(val message: String = "Cargando...") : ProfileState()
    object LoggedOut : ProfileState()
    object Setup : ProfileState()
    object LoggedIn : ProfileState()
    data class Error(val message: String) : ProfileState()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileScreen(navController: androidx.navigation.NavController, targetUid: String? = null) {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val authService = remember { GoogleAuthService(context) }
    val scope = rememberCoroutineScope()
    
    

    val userRepository = remember { UserRepository() }
    var profileData by remember { mutableStateOf<ProfileResponse?>(null) }
    var currentProfileState by remember { mutableStateOf<ProfileState>(ProfileState.Loading("Cargando...")) }

    fun verifyProfile() {
        scope.launch {
            currentProfileState = ProfileState.Loading("Verificando perfil...")
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                currentProfileState = ProfileState.LoggedOut
                return@launch
            }
            
            val result = userRepository.getProfile(user.uid)
            result.onSuccess { profile ->
                profileData = profile
                if (!profile.username.isNotEmpty()) {
                    currentProfileState = ProfileState.Setup
                } else {
                    currentProfileState = ProfileState.LoggedIn
                }
            }.onFailure {
                // If 404, we might need to create it, but usually the backend should handle creation on first get if it doesn't exist
                // For now, let's assume it exists if they are authenticated or they need setup.
                currentProfileState = ProfileState.Error("No se pudo obtener el perfil")
            }
        }
    }

    // Listen to Firebase Auth state changes
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                currentProfileState = ProfileState.LoggedOut
            } else {
                verifyProfile()
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentProfileState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(400)
                )).togetherWith(
                    fadeOut(animationSpec = tween(400)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(400)
                    )
                )
            },
            label = "ProfileStateTransition"
        ) { targetState ->
            when (targetState) {
                is ProfileState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(targetState.message, color = Color.LightGray, fontSize = 14.sp)
                    }
                }
                is ProfileState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Hubo un problema", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            targetState.message,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { verifyProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                ProfileState.LoggedOut -> {
                    LaunchedEffect(Unit) {
                        navController.navigate("login")
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                ProfileState.Setup -> {
                    ProfileSetupView(
                        user = FirebaseAuth.getInstance().currentUser,
                        userRepository = userRepository,
                        onComplete = {
                            currentProfileState = ProfileState.LoggedIn
                            verifyProfile()
                        }
                    )
                }
                ProfileState.LoggedIn -> {
                    LoggedInView(
                        user = FirebaseAuth.getInstance().currentUser,
                        navController = navController,
                        targetUid = targetUid,
                        userRepository = userRepository,
                        onLogout = {
                            scope.launch {
                                currentProfileState = ProfileState.Loading("Cerrando sesión...")
                                authService.signOut()
                                currentProfileState = ProfileState.LoggedOut
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoggedOutView(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Icono de perfil grande
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Así se verá tu perfil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Inicia sesión para crear tu perfil, conectar con amigos, ver tus chats y disfrutar de todas las funciones.",
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tarjeta de perfil de ejemplo (Preview)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("@usuario_ejemplo", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Text("0 seguidos", color = Color.Gray, fontSize = 12.sp)
                        Text("0 conexiones", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón Google Elegante
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                // Usando un ícono temporal ya que no tengo acceso a archivos SVG externos
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle, // Placeholder para icono Google
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continuar con Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileSetupView(user: com.google.firebase.auth.FirebaseUser?, userRepository: UserRepository, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Completa tu perfil", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        
        UserAvatar(
            photoUrl = user?.photoUrl?.toString(),
            size = 100.dp,
            borderWidth = 1.5.dp,
            borderColor = Color.White.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Foto desde Google", color = Color.Gray, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color(0xFFEF4444), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de usuario (único)", color = Color.Gray) },
            leadingIcon = { Text("@", color = Color.Gray, modifier = Modifier.padding(start = 16.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nombre", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                if (username.isNotBlank() && !isSaving) {
                    isSaving = true
                    errorMessage = ""
                    
                    scope.launch {
                        val result = userRepository.updateProfile(username, displayName)
                        result.onSuccess { 
                            onComplete() 
                        }.onFailure { 
                            errorMessage = it.message ?: "Error al actualizar"
                            isSaving = false
                        }
                    }
                }
            },
            enabled = username.isNotBlank() && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Continuar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}



@Composable
fun LoggedInView(
    user: com.google.firebase.auth.FirebaseUser?, 
    navController: androidx.navigation.NavController, 
    targetUid: String?, 
    userRepository: UserRepository,
    onLogout: () -> Unit
) {
    var profileData by remember { mutableStateOf<ProfileResponse?>(null) }
    var userVideos by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val postRepository = remember { PostRepository() }
    val context = LocalContext.current
    
    LaunchedEffect(user, targetUid) {
        val activeUid = targetUid ?: user?.uid
        if (activeUid != null) {
            val profileResult = userRepository.getProfile(activeUid)
            profileResult.onSuccess { profileData = it }
            
            val videoResult = try {
                postRepository.getVideos("USER_$activeUid")
            } catch(e: Exception) {
                null
            }
            if (videoResult != null && videoResult.isSuccess) {
                userVideos = videoResult.getOrNull() ?: emptyList()
            }
        }
    }
        
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        val isSpecial = (profileData?.username?.lowercase() == "menajohan90")

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            if (isSpecial) {
                GlowingWingsBackground(modifier = Modifier.fillMaxSize())
            }
            
            Box(
                modifier = Modifier.size(100.dp)
            ) {
                UserAvatar(
                    photoUrl = profileData?.photoUrl ?: if (targetUid == null || targetUid == user?.uid) user?.photoUrl?.toString() else null,
                    size = 100.dp,
                    borderWidth = if (isSpecial) 2.5.dp else 1.5.dp,
                    borderColor = if (isSpecial) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                    contentDescription = "Foto de perfil"
                )
                
                if (isSpecial) {
                    CrownIcon(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-12).dp)
                    )
                }
            }
        }
                
        Spacer(modifier = Modifier.height(16.dp))
                
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "@${profileData?.username ?: "usuario"}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (profileData?.isCreator == true) {
                Spacer(modifier = Modifier.width(6.dp))
                CreatorBadge()
            }
        }
                
        Spacer(modifier = Modifier.height(8.dp))
                
        Text(
            text = profileData?.displayName ?: if (targetUid == null || targetUid == user?.uid) user?.displayName ?: "Usuario" else "Usuario",
            fontSize = 16.sp,
            color = Color.LightGray
        )
        
        if (isSpecial) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .composeBorder(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verificado",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Developer / Desarrollador",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val isVerified = profileData?.isVerified == true
        val isSinger = profileData?.isSinger == true
        val isCrown = profileData?.isCrown == true

        if (isVerified || isSinger || isCrown) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isVerified) {
                    Box(modifier = Modifier.background(Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = "Verificado", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (isSinger) {
                    Box(modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = "Cantante", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (isCrown) {
                    Box(modifier = Modifier.background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = "Corona", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
                
        Spacer(modifier = Modifier.height(24.dp))
                

        // Row de acciones para el perfil
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (targetUid != null && targetUid != FirebaseAuth.getInstance().currentUser?.uid) {
                // Connection Logic
                var connStatus by remember { mutableStateOf<String>("none") }
                var isConnecting by remember { mutableStateOf(false) }
                val currentUser = FirebaseAuth.getInstance().currentUser

                LaunchedEffect(targetUid, currentUser?.uid) {
                    if (currentUser != null && targetUid != null) {
                        val result = userRepository.getConnections()
                        result.onSuccess { conns ->
                            val conn = conns.find { it.requesterUid == targetUid || it.targetUid == targetUid }
                            connStatus = when {
                                conn == null -> "none"
                                conn.status.equals("CONNECTED", true) || conn.status.equals("connected", true) || conn.status.equals("accepted", true) -> "connected"
                                conn.requesterUid == currentUser.uid -> "pending_sent"
                                else -> "pending_received"
                            }
                        }
                    }
                }

                when (connStatus) {
                    "connected" -> {
                        Button(
                            onClick = { navController.navigate("chat/${targetUid}") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("✓ Conectados")
                        }
                    }
                    "pending_sent" -> {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Pendiente")
                        }
                    }
                    "pending_received" -> {
                        Button(
                            onClick = {
                                if (currentUser != null && !isConnecting) {
                                    isConnecting = true
                                    scope.launch {
                                        userRepository.connectUser(targetUid)
                                        isConnecting = false
                                        // Refresh status
                                        val result = userRepository.getConnections()
                                        result.onSuccess { conns ->
                                            val conn = conns.find { it.requesterUid == targetUid || it.targetUid == targetUid }
                                            if (conn != null) connStatus = if (conn.status == "CONNECTED") "connected" else "pending_sent"
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.height(40.dp),
                            enabled = !isConnecting
                        ) {
                            if (isConnecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Aceptar Conexión")
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                if (currentUser != null && !isConnecting) {
                                    isConnecting = true
                                    scope.launch {
                                        userRepository.connectUser(targetUid)
                                        isConnecting = false
                                        connStatus = "pending_sent"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.height(40.dp),
                            enabled = !isConnecting
                        ) {
                            if (isConnecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Conectar")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Editar Perfil", color = Color.White)
                }

                OutlinedButton(
                    onClick = { navController.navigate("reports") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Denuncias")
                }

                OutlinedButton(
                    onClick = { navController.navigate("update_screen") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Actualizaciones")
                }

                val adminEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase() ?: ""
                if (adminEmail == "menajohan90@gmail.com" || adminEmail == "joyerialucia93@gmail.com" || adminEmail.contains("admin")) {
                    Button(
                        onClick = { navController.navigate("admin_badges") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Insignias (Admin)", color = Color.White)
                    }
                }
                
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cerrar Sesión")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(thickness = 1.dp, color = Color.DarkGray)
        
        var selectedTabIndex by remember { mutableStateOf(0) }
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color.White
                )
            }
        ) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                Text("Mis Videos", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
            }
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                Text("Compartidos", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal)
            }
        }
        
        var sharedVideos by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
        LaunchedEffect(selectedTabIndex, user?.uid) {
            if (selectedTabIndex == 1 && user != null) {
                try {
                    val result = postRepository.getVideos("SHARED")
                    if (result.isSuccess) {
                        sharedVideos = result.getOrNull() ?: emptyList()
                    }
                } catch(e: Exception) {
                    // Ignore silently
                }
            }
        }

        // Video grid
        val displayVideos = if (selectedTabIndex == 0) userVideos else sharedVideos
        
        if (displayVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (selectedTabIndex == 0) "No tienes videos." else "No has compartido videos.", color = Color.Gray)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp)
        ) {
            items(displayVideos) { video ->
                Box(
                    modifier = Modifier
                        .aspectRatio(9f / 16f)
                        .padding(1.dp)
                        .background(Color.DarkGray)
                        .clickable { /* Play video */ }
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(video.videoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (selectedTabIndex == 1) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Share, contentDescription = "Shared", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
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
        }
    }
    
    }

    if (showEditDialog && profileData != null && user != null) {
        EditProfileDialog(
            user = user,
            currentUsername = profileData!!.username,
            currentDisplayName = profileData!!.displayName,
            userRepository = userRepository,
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun EditProfileDialog(
    user: com.google.firebase.auth.FirebaseUser, 
    currentUsername: String, 
    currentDisplayName: String, 
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }
    var newDisplayName by remember { mutableStateOf(currentDisplayName) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Editar Perfil", color = Color.White) },
        text = {
            Column {
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color(0xFFEF4444), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Nombre de usuario", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White, unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = { Text("Nombre", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White, unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isBlank() || newDisplayName.isBlank()) {
                        errorMessage = "Los campos no pueden estar vacíos"
                        return@Button
                    }
                    isSaving = true
                    errorMessage = ""
                    
                    scope.launch {
                        val result = userRepository.updateProfile(newUsername, newDisplayName)
                        result.onSuccess {
                            onDismiss()
                        }.onFailure {
                            errorMessage = it.message ?: "Error al guardar"
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CrownIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(24.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.85f)
            lineTo(size.width * 0.9f, size.height * 0.85f)
            lineTo(size.width * 0.85f, size.height * 0.45f)
            lineTo(size.width * 0.65f, size.height * 0.65f)
            lineTo(size.width * 0.5f, size.height * 0.25f)
            lineTo(size.width * 0.35f, size.height * 0.65f)
            lineTo(size.width * 0.15f, size.height * 0.45f)
            close()
        }
        drawPath(path, color = Color(0xFFFFD700)) // Gold Crown
        
        // Draw little ruby / sapphire jewels on top of the peaks
        drawCircle(color = Color(0xFFEF4444), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f))
        drawCircle(color = Color(0xFF3B82F6), radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.45f))
        drawCircle(color = Color(0xFF3B82F6), radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.45f))
    }
}

@Composable
fun GlowingWingsBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wingsTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    
    androidx.compose.foundation.Canvas(modifier = modifier.size(160.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        val leftWingPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 10.dp.toPx(), centerY)
            cubicTo(
                centerX - 40.dp.toPx(), centerY - 60.dp.toPx(),
                centerX - 80.dp.toPx(), centerY - 40.dp.toPx(),
                centerX - 95.dp.toPx() * glowScale, centerY + 10.dp.toPx()
            )
            cubicTo(
                centerX - 70.dp.toPx(), centerY + 30.dp.toPx(),
                centerX - 40.dp.toPx(), centerY + 20.dp.toPx(),
                centerX - 10.dp.toPx(), centerY + 5.dp.toPx()
            )
            close()
        }
        
        val rightWingPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX + 10.dp.toPx(), centerY)
            cubicTo(
                centerX + 40.dp.toPx(), centerY - 60.dp.toPx(),
                centerX + 80.dp.toPx(), centerY - 40.dp.toPx(),
                centerX + 95.dp.toPx() * glowScale, centerY + 10.dp.toPx()
            )
            cubicTo(
                centerX + 70.dp.toPx(), centerY + 30.dp.toPx(),
                centerX + 40.dp.toPx(), centerY + 20.dp.toPx(),
                centerX + 10.dp.toPx(), centerY + 5.dp.toPx()
            )
            close()
        }
        
        val brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(Color(0xFFFF3D00).copy(alpha = 0.4f), Color(0xFFFFD700).copy(alpha = 0.05f)),
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            radius = 120.dp.toPx() * glowScale
        )
        
        drawPath(leftWingPath, brush)
        drawPath(rightWingPath, brush)
        
        drawPath(leftWingPath, color = Color(0xFFFFD700).copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
        drawPath(rightWingPath, color = Color(0xFFFFD700).copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun CreatorBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Creador Verificado",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

