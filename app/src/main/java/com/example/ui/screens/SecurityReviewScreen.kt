package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SecurityReviewScreen(navController: androidx.navigation.NavController) {
    var state by remember { mutableStateOf("Tu video está en revisión") }
    var description by remember { mutableStateOf("Nuestro sistema de seguridad está revisando el contenido.") }

    LaunchedEffect(Unit) {
        delay(2500)
        state = "Revisión completada"
        description = "El video es seguro y se está procesando."
        delay(1500)
        navController.navigate("main") {
            popUpTo(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = state, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, color = Color.LightGray, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
