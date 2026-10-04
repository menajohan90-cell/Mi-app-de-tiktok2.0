package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReportModel
import com.example.utils.SvgFlag
import com.example.utils.SvgScanning
import com.example.utils.SvgSuccess
import com.example.utils.SvgInfo
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(navController: androidx.navigation.NavController, reportId: String) {
    val db = FirebaseFirestore.getInstance()
    var report by remember { mutableStateOf<ReportModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(reportId) {
        val listener = db.collection("reports").document(reportId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    report = snapshot.toObject(ReportModel::class.java)
                }
                isLoading = false
            }
        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Denuncia", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        SvgFlag(tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else if (report == null) {
                Text("No se encontró la denuncia.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                val currentStatus = report!!.status
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SvgFlag(tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Denuncia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Motivo: ${report!!.reason}", color = Color.White, fontSize = 15.sp)
                            if (!report!!.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Descripción: ${report!!.description}", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }

                    Text("Progreso del Sistema", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            
                            StepRow(
                                title = "Revisando denuncia",
                                isActive = currentStatus == "reviewing_report",
                                isCompleted = currentStatus == "reviewing_publication" || currentStatus == "resolved"
                            )

                            StepRow(
                                title = "Revisando publicación",
                                isActive = currentStatus == "reviewing_publication",
                                isCompleted = currentStatus == "resolved"
                            )

                            val isResolved = currentStatus == "resolved"
                            val resultTitle = if (isResolved) {
                                if (report!!.systemResult == "accepted") "Denuncia aceptada" else "Denuncia rechazada"
                            } else {
                                "Resultado"
                            }

                            StepRow(
                                title = resultTitle,
                                isActive = isResolved,
                                isCompleted = isResolved
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Análisis del Sistema", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report!!.systemAnalysis,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepRow(title: String, isActive: Boolean, isCompleted: Boolean) {
    val activeColor = Color(0xFFEF4444)
    val inactiveColor = Color.Gray

    val tint = when {
        isActive -> activeColor
        isCompleted -> Color(0xFF10B981)
        else -> inactiveColor
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                SvgScanning(tint = tint)
            } else if (isCompleted) {
                SvgSuccess(tint = tint)
            } else {
                SvgInfo(tint = tint)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = if (isActive) activeColor else if (isCompleted) Color.White else Color.Gray,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}
