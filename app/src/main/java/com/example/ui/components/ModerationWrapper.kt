package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.example.data.UserRepository

@Composable
fun ModerationWrapper(
    onForceProfileUpdate: () -> Unit,
    content: @Composable () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val userRepository = remember { UserRepository() }
    var isSuspended by remember { mutableStateOf(false) }
    var suspensionReason by remember { mutableStateOf("") }
    var hasNameViolation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        if (user != null) {
            val result = userRepository.getProfile(user.uid)
            result.onSuccess { profile ->
                isSuspended = profile.suspended
                suspensionReason = profile.suspensionReason ?: ""
                
                if (profile.moderationStatus == "nameViolation") {
                    hasNameViolation = true
                    onForceProfileUpdate()
                } else {
                    hasNameViolation = false
                }
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (isSuspended) {
        // Block the app entirely for suspended users
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Cuenta Suspendida",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tu cuenta ha sido suspendida permanentemente por violar las normas de seguridad de la plataforma.",
                color = Color.LightGray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            if (suspensionReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Motivo: $suspensionReason",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { auth.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Cerrar sesión", color = Color.White)
            }
        }
    } else if (hasNameViolation) {
        // Show block screen that forces them to fix name, but also calls `onForceProfileUpdate` to auto-navigate
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFF59E0B), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Acción Requerida",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tu nombre de usuario no cumple nuestras normas de seguridad. Debes cambiarlo para continuar utilizando la plataforma.",
                color = Color.LightGray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { onForceProfileUpdate() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)) // Blue
            ) {
                Text("Actualizar Perfil", color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { auth.signOut() }) {
                Text("Cerrar sesión", color = Color.Gray)
            }
        }
    } else {
        // Normal app flow
        content()
    }
}
