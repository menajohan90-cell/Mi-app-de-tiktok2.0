package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.data.ProfileResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBadgePanelScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    
    val currentUser = auth.currentUser
    val email = currentUser?.email?.lowercase() ?: ""
    val isAdmin = email == "menajohan90@gmail.com" || email == "joyerialucia93@gmail.com" || email.contains("admin")

    var users by remember { mutableStateOf<List<ProfileResponse>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadUsers() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = db.collection("users").get().await()
                users = snapshot.documents.mapNotNull { it.toObject(ProfileResponse::class.java) }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar usuarios: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isAdmin) {
            loadUsers()
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración - Insignias", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!isAdmin) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Acceso denegado. Este panel es exclusivo para Menajohan90@gmail.com", color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Button(
                        onClick = { navController.navigate("admin_update_management") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Gestionar Actualizaciones (APK / Firebase)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (email == "menajohan90@gmail.com") {
                        Button(
                            onClick = { navController.navigate("admin_verification") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Verificar cuentas (Exclusivo menajohan90@gmail.com)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar usuario por nombre o correo...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredUsers = users.filter { 
                        it.username.contains(searchQuery, true) || 
                        it.displayName.contains(searchQuery, true) || 
                        it.email.contains(searchQuery, true) 
                    }

                    val userRepository = remember { com.example.data.UserRepository() }
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredUsers, key = { it.uid }) { profile ->
                            AdminUserCard(
                                profile = profile,
                                onUpdateBadges = { isVer, isSing, isCrn ->
                                    scope.launch {
                                        val result = userRepository.setUserBadges(profile.uid, isVer, isSing, isCrn)
                                        result.onSuccess {
                                            Toast.makeText(context, "Insignias actualizadas para @${profile.username}", Toast.LENGTH_SHORT).show()
                                            loadUsers()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(profile: ProfileResponse, onUpdateBadges: (Boolean, Boolean, Boolean) -> Unit) {
    var isVerified by remember(profile.isVerified) { mutableStateOf(profile.isVerified) }
    var isSinger by remember(profile.isSinger) { mutableStateOf(profile.isSinger) }
    var isCrown by remember(profile.isCrown) { mutableStateOf(profile.isCrown) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                    Text(profile.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = profile.displayName.ifBlank { profile.username }, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "@${profile.username} • ${profile.email}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Verificado", color = Color.White, fontSize = 14.sp)
                Switch(checked = isVerified, onCheckedChange = { isVerified = it; onUpdateBadges(isVerified, isSinger, isCrown) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cantante", color = Color.White, fontSize = 14.sp)
                Switch(checked = isSinger, onCheckedChange = { isSinger = it; onUpdateBadges(isVerified, isSinger, isCrown) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Corona", color = Color.White, fontSize = 14.sp)
                Switch(checked = isCrown, onCheckedChange = { isCrown = it; onUpdateBadges(isVerified, isSinger, isCrown) })
            }
        }
    }
}
