package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReportModel
import com.example.data.ConnectionResponse
import com.example.utils.SvgFlag
import com.example.utils.SvgSuccess
import com.example.utils.SvgScanning
import com.example.utils.SvgInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NotificationModel(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val reportId: String? = null,
    val senderUsername: String? = null,
    val senderDisplayName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: androidx.navigation.NavController, filterType: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var reports by remember { mutableStateOf<List<ReportModel>>(emptyList()) }
    var connections by remember { mutableStateOf<List<ConnectionResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        val user = auth.currentUser
        if (user != null) {
            scope.launch {
                isLoading = true
                try {
                    if (filterType == "system") {
                        val snapshot = db.collection("system_notifications")
                            .whereEqualTo("uid", user.uid)
                            .get().await()
                        notifications = snapshot.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
                            .filter { it.type != "connection" }
                            .sortedByDescending { it.createdAt }

                        val reportSnapshot = db.collection("reports")
                            .whereEqualTo("reporterUid", user.uid)
                            .get().await()
                        reports = reportSnapshot.documents.mapNotNull { it.toObject(ReportModel::class.java) }
                            .sortedByDescending { it.createdAt }
                    } else if (filterType == "connections") {
                        val snapshot = db.collection("connections")
                            .whereEqualTo("targetUid", user.uid)
                            .get().await()
                        connections = snapshot.documents.mapNotNull { it.toObject(ConnectionResponse::class.java) }
                            .filter { it.status == "pending" || it.status == "accepted" }
                            .sortedByDescending { it.createdAt }
                    } else {
                        // Novedades
                        val snapshot = db.collection("system_notifications")
                            .whereEqualTo("uid", user.uid)
                            .get().await()
                        notifications = snapshot.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
                            .filter { it.type != "connection" }
                            .sortedByDescending { it.createdAt }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationsScreen", "Error loading", e)
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(filterType) {
        loadData()
    }

    val titleText = when (filterType) {
        "system" -> "Sistema y Denuncias"
        "connections" -> "Nuevas Conexiones"
        else -> "Novedades"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White) },
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
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else if (filterType == "connections" && connections.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SvgInfo(tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay solicitudes de conexión pendientes.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (filterType == "system" && notifications.isEmpty() && reports.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SvgFlag(tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay denuncias ni notificaciones en el sistema.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filterType == "connections") {
                        items(connections, key = { it.connectionId }) { conn ->
                            ConnectionRequestCard(connection = conn, onAccept = {
                                scope.launch {
                                    db.collection("connections").document(conn.connectionId)
                                        .update("status", "accepted").await()
                                    loadData()
                                    navController.navigate("chat_detail/${conn.requesterUid}")
                                }
                            })
                        }
                    } else if (filterType == "system") {
                        if (reports.isNotEmpty()) {
                            item {
                                Text("Tus Denuncias", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            items(reports, key = { it.reportId }) { report ->
                                SystemReportCard(report = report, onClick = {
                                    navController.navigate("report_detail/${report.reportId}")
                                })
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Avisos del Sistema", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            items(notifications, key = { it.id }) { notif ->
                                SystemNotificationCard(notification = notif, onClick = {
                                    if (!notif.reportId.isNullOrBlank()) {
                                        navController.navigate("report_detail/${notif.reportId}")
                                    }
                                })
                            }
                        }
                    } else {
                        items(notifications, key = { it.id }) { notif ->
                            SystemNotificationCard(notification = notif, onClick = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestCard(connection: ConnectionResponse, onAccept: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                SvgSuccess(tint = Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¿Deseas conectar?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Un usuario quiere conectar contigo para iniciar mensajes.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Aceptar y Chatear", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SystemReportCard(report: ReportModel, onClick: () -> Unit) {
    val statusText = when (report.status) {
        "reviewing_report" -> "El sistema está revisando tu denuncia. Espere por favor."
        "reviewing_publication" -> "El sistema está revisando la publicación."
        "resolved" -> if (report.systemResult == "accepted") "Denuncia aprobada (Video eliminado)" else "Denuncia rechazada"
        else -> report.systemAnalysis
    }

    val statusColor = when (report.status) {
        "resolved" -> if (report.systemResult == "accepted") Color(0xFF10B981) else Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (report.status == "resolved") SvgSuccess(tint = statusColor)
                else SvgScanning(tint = statusColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Motivo: ${report.reason}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = android.text.format.DateUtils.getRelativeTimeSpanString(report.createdAt).toString(),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SystemNotificationCard(notification: NotificationModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                SvgInfo(tint = Color(0xFF3B82F6))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = android.text.format.DateUtils.getRelativeTimeSpanString(notification.createdAt).toString(),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
