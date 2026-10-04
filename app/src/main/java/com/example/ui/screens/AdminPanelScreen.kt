package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.UserBrief
import com.example.data.UserRepository
import com.example.data.ProfileResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    
    var searchQuery by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserBrief>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    var selectedUser by remember { mutableStateOf<ProfileResponse?>(null) }
    var showUserOptions by remember { mutableStateOf(false) }

    fun search(q: String) {
        if (q.length < 3) return
        isSearching = true
        scope.launch {
            userRepository.searchUsers(q).onSuccess {
                userResults = it
                isSearching = false
            }.onFailure {
                isSearching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    search(it)
                },
                placeholder = { Text("Buscar usuario (@username)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.White)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(userResults) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    userRepository.getProfile(user.uid).onSuccess {
                                        selectedUser = it
                                        showUserOptions = true
                                    }
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                com.example.ui.components.UserAvatar(photoUrl = user.photoUrl, size = 48.dp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("@${user.username}", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserOptions && selectedUser != null) {
        AdminUserOptionsDialog(
            user = selectedUser!!,
            userRepository = userRepository,
            onDismiss = { showUserOptions = false },
            onUpdate = {
                showUserOptions = false
                search(searchQuery) // Refresh
            }
        )
    }
}

@Composable
fun AdminUserOptionsDialog(
    user: ProfileResponse,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var isVerified by remember { mutableStateOf(user.isVerified) }
    var isSinger by remember { mutableStateOf(user.isSinger) }
    var isCrown by remember { mutableStateOf(user.isCrown) }
    
    var showSuspendOptions by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Gestionar @${user.username}", color = Color.White) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isVerified, onCheckedChange = { isVerified = it })
                    Text("Verificado (Badge Azul)", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSinger, onCheckedChange = { isSinger = it })
                    Text("Cantante (Badge Rojo)", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCrown, onCheckedChange = { isCrown = it })
                    Text("Corona (Badge Dorado)", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { showSuspendOptions = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suspender Cuenta")
                }
                
                if (user.suspended) {
                    Text("Actualmente suspendido", color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    userRepository.setUserBadges(user.uid, isVerified, isSinger, isCrown)
                    Toast.makeText(context, "Insignias actualizadas", Toast.LENGTH_SHORT).show()
                    onUpdate()
                }
            }) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Color.Gray) }
        }
    )

    if (showSuspendOptions) {
        SuspendOptionsDialog(
            onDismiss = { showSuspendOptions = false },
            onConfirm = { durationMs, reason ->
                scope.launch {
                    userRepository.suspendUser(user.uid, durationMs, reason)
                    Toast.makeText(context, "Usuario suspendido", Toast.LENGTH_SHORT).show()
                    showSuspendOptions = false
                    onUpdate()
                }
            }
        )
    }
}

@Composable
fun SuspendOptionsDialog(onDismiss: () -> Unit, onConfirm: (Long, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    val options = listOf(
        "1 Día" to 24L * 60 * 60 * 1000,
        "1 Semana" to 7L * 24 * 60 * 60 * 1000,
        "1 Mes" to 30L * 24 * 60 * 60 * 1000,
        "Permanente" to 100L * 365 * 24 * 60 * 60 * 1000,
        "Levantar Suspensión" to 0L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Elegir Duración", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo de la suspensión", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { (label, duration) ->
                    Button(
                        onClick = { onConfirm(duration, reason) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (duration == 0L) Color(0xFF22C55E) else Color(0xFF3F3F46)
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }
    )
}
